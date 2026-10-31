package de.derpeterson.app.security;

import com.nimbusds.jose.util.JSONObjectUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Erhält Microsofts zusätzliche Issuerbindung der Schlüssel vor der Nimbus-JWK-Auswertung. */
final class MicrosoftSigningKeys {
  private MicrosoftSigningKeys() {}

  static RestTemplate client(String issuer) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    factory.setReadTimeout(Duration.ofSeconds(10));
    RestTemplate client = new RestTemplate(factory);
    client
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              ClientHttpResponse original = execution.execute(request, body);
              try {
                byte[] raw = original.getBody().readNBytes(1024 * 1024 + 1);
                if (raw.length > 1024 * 1024) {
                  throw new IOException("Signing key response too large");
                }
                byte[] filtered =
                    filter(new String(raw, StandardCharsets.UTF_8), issuer)
                        .getBytes(StandardCharsets.UTF_8);
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(original.getHeaders());
                headers.setContentLength(filtered.length);
                return new ClientHttpResponse() {
                  @Override
                  public HttpStatusCode getStatusCode() throws IOException {
                    return original.getStatusCode();
                  }

                  @Override
                  public String getStatusText() throws IOException {
                    return original.getStatusText();
                  }

                  @Override
                  public HttpHeaders getHeaders() {
                    return headers;
                  }

                  @Override
                  public InputStream getBody() {
                    return new ByteArrayInputStream(filtered);
                  }

                  @Override
                  public void close() {
                    original.close();
                  }
                };
              } catch (IOException | RuntimeException failure) {
                original.close();
                throw failure;
              }
            });
    return client;
  }

  static String filter(String json, String issuer) throws IOException {
    try {
      Map<String, Object> document = JSONObjectUtils.parse(json);
      List<Object> keys = JSONObjectUtils.getJSONArray(document, "keys");
      List<Object> allowed =
          keys.stream()
              .filter(
                  value -> {
                    if (!(value instanceof Map<?, ?> key)
                        || !(key.get("issuer") instanceof String keyIssuer)) {
                      return false;
                    }
                    return keyIssuer.equals(issuer)
                        || keyIssuer.equals("https://login.microsoftonline.com/{tenantid}/v2.0");
                  })
              .toList();
      return JSONObjectUtils.toJSONString(Map.of("keys", allowed));
    } catch (ParseException failure) {
      throw new IOException("Invalid signing key response", failure);
    }
  }
}
