package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Eigener kurzlebiger OIDC-Dienst pro Worker, mit echten signierten Tokens und PKCE-Prüfung. */
final class OidcTestProvider implements AutoCloseable {
  private record Code(
      String provider, Map<String, String> authorization, String email, String mode) {}

  private final ObjectMapper json = new ObjectMapper();
  private final HttpServer server;
  private final KeyPair key;
  private final Map<String, Code> codes = new ConcurrentHashMap<>();
  private final String appUrl;
  private static final String CLIENT = "own-browser-client";
  private static final String TENANT = "9188040d-6c67-4c5b-b112-36a304b66dad";

  OidcTestProvider(String appUrl) throws Exception {
    this.appUrl = appUrl;
    key = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  void configure(Map<String, String> environment) {
    int port = server.getAddress().getPort();
    for (String provider : List.of("GOOGLE", "MICROSOFT")) {
      String prefix = "BROWSER_TEST_OIDC_" + provider + "_";
      String path = provider.toLowerCase(java.util.Locale.ROOT);
      environment.put(prefix + "CLIENT", CLIENT);
      environment.put(prefix + "ENABLED", "true");
      environment.put(prefix + "SECRET", BrowserTestAccounts.OIDC_CLIENT_SECRET);
      environment.put(prefix + "AUTH", "http://127.0.0.1:" + port + "/" + path + "/authorize");
      environment.put(
          prefix + "TOKEN", "http://host.docker.internal:" + port + "/" + path + "/token");
      environment.put(prefix + "KEYS", "http://host.docker.internal:" + port + "/keys");
    }
  }

  private void handle(HttpExchange exchange) throws IOException {
    try {
      String path = exchange.getRequestURI().getPath();
      if (path.equals("/keys")) {
        RSAPublicKey rsa = (RSAPublicKey) key.getPublic();
        send(
            exchange,
            200,
            "application/json",
            json.writeValueAsString(
                Map.of(
                    "keys",
                    List.of(
                        Map.of(
                            "kty",
                            "RSA",
                            "kid",
                            "own-browser-key",
                            "use",
                            "sig",
                            "issuer",
                            "https://login.microsoftonline.com/{tenantid}/v2.0",
                            "alg",
                            "RS256",
                            "n",
                            unsigned(rsa.getModulus()),
                            "e",
                            unsigned(rsa.getPublicExponent()))))));
      } else if (path.endsWith("/authorize")) {
        authorize(exchange, path.split("/")[1]);
      } else if (path.endsWith("/token")) {
        token(exchange, path.split("/")[1]);
      } else {
        send(exchange, 404, "text/plain", "Own fixture endpoint not found");
      }
    } catch (Exception _) {
      send(exchange, 400, "application/json", "{\"error\":\"invalid_request\"}");
    }
  }

  private void authorize(HttpExchange exchange, String provider) throws IOException {
    Map<String, String> authorization = form(exchange.getRequestURI().getRawQuery());
    String callback = appUrl + "/login/oauth2/code/" + provider;
    if (!CLIENT.equals(authorization.get("client_id"))
        || !callback.equals(authorization.get("redirect_uri"))
        || !"S256".equals(authorization.get("code_challenge_method"))
        || authorization.get("nonce") == null) {
      throw new IllegalArgumentException("Invalid own authorization request");
    }
    if (exchange.getRequestMethod().equals("GET")) {
      String parameters =
          "<div id=fixture-authorize-parameters data-prompt=\""
              + htmlAttribute(authorization.getOrDefault("prompt", ""))
              + "\" data-max-age=\""
              + htmlAttribute(authorization.getOrDefault("max_age", ""))
              + "\" data-claims=\""
              + htmlAttribute(authorization.getOrDefault("claims", ""))
              + "\"></div>";
      send(
          exchange,
          200,
          "text/html; charset=utf-8",
          "<!doctype html><html><body><h1>Own OIDC test provider</h1>"
              + parameters
              + "<form method=post><label>Test email<input name=email id=fixture-email required type=email></label><label>Test response<select name=mode id=fixture-mode><option value=valid>Valid</option><option value=nonce>Wrong nonce</option><option value=audience>Wrong audience</option><option value=stale>Old authentication</option><option value=missing-auth-time>Missing authentication time</option><option value=cancel>Cancel</option></select></label><button id=fixture-signin>Continue with own test identity</button></form></body></html>");
      return;
    }
    Map<String, String> input =
        form(new String(exchange.getRequestBody().readNBytes(8192), StandardCharsets.UTF_8));
    String target;
    if ("cancel".equals(input.get("mode"))) {
      target = callback + "?error=access_denied&state=" + encode(authorization.get("state"));
    } else {
      String code = UUID.randomUUID().toString();
      codes.put(
          code,
          new Code(
              provider, authorization, input.get("email"), input.getOrDefault("mode", "valid")));
      target = callback + "?code=" + code + "&state=" + encode(authorization.get("state"));
    }
    exchange.getResponseHeaders().set("Location", target);
    exchange.sendResponseHeaders(302, -1);
    exchange.close();
  }

  private void token(HttpExchange exchange, String provider) throws Exception {
    Map<String, String> input =
        form(new String(exchange.getRequestBody().readNBytes(8192), StandardCharsets.UTF_8));
    Code code = codes.remove(input.get("code"));
    if (code == null
        || !provider.equals(code.provider())
        || !CLIENT.equals(input.get("client_id"))
        || !BrowserTestAccounts.OIDC_CLIENT_SECRET.equals(input.get("client_secret"))
        || !code.authorization().get("redirect_uri").equals(input.get("redirect_uri"))
        || !code.authorization()
            .get("code_challenge")
            .equals(
                base64(
                    MessageDigest.getInstance("SHA-256")
                        .digest(
                            input
                                .getOrDefault("code_verifier", "")
                                .getBytes(StandardCharsets.US_ASCII))))) {
      throw new IllegalArgumentException("Invalid own token exchange");
    }
    long now = Instant.now().getEpochSecond();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put(
        "iss",
        provider.equals("google")
            ? "https://accounts.google.com"
            : "https://login.microsoftonline.com/" + TENANT + "/v2.0");
    claims.put("sub", "own-" + code.email());
    claims.put("aud", code.mode().equals("audience") ? "wrong-client" : CLIENT);
    claims.put("iat", now);
    claims.put("exp", now + 300);
    if (!code.mode().equals("missing-auth-time")) {
      claims.put("auth_time", code.mode().equals("stale") ? now - 3600 : now);
    }
    claims.put(
        "nonce", code.mode().equals("nonce") ? "wrong-nonce" : code.authorization().get("nonce"));
    claims.put("email", code.email());
    claims.put("email_verified", true);
    claims.put("given_name", "External");
    claims.put("family_name", "Browser");
    claims.put("tid", TENANT);
    String content =
        base64(json.writeValueAsBytes(Map.of("alg", "RS256", "kid", "own-browser-key")))
            + "."
            + base64(json.writeValueAsBytes(claims));
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(key.getPrivate());
    signature.update(content.getBytes(StandardCharsets.US_ASCII));
    String jwt = content + "." + base64(signature.sign());
    send(
        exchange,
        200,
        "application/json",
        json.writeValueAsString(
            Map.of(
                "access_token",
                "own-discarded-access-token",
                "token_type",
                "Bearer",
                "expires_in",
                300,
                "scope",
                "openid profile email",
                "id_token",
                jwt)));
  }

  private String htmlAttribute(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private Map<String, String> form(String data) {
    Map<String, String> result = new HashMap<>();
    if (data != null) {
      for (String pair : data.split("&")) {
        String[] parts = pair.split("=", 2);
        result.put(
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
            parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
      }
    }
    return result;
  }

  private String unsigned(BigInteger value) {
    byte[] bytes = value.toByteArray();
    return base64(bytes[0] == 0 ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes);
  }

  private String base64(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private void send(HttpExchange exchange, int status, String type, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  @Override
  public void close() {
    server.stop(0);
    codes.clear();
  }
}
