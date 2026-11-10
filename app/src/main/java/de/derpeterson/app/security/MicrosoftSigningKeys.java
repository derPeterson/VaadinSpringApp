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

  static RestTemplate client(String issuerUrl) {
    JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    RestTemplate restTemplate = new RestTemplate(requestFactory);
    restTemplate
        .getInterceptors()
        .add((clientRequest, requestBodyBytes, requestExecution) -> {
          ClientHttpResponse originalResponse =
              requestExecution.execute(clientRequest, requestBodyBytes);

          try {
            byte[] originalResponseBytes = originalResponse.getBody().readNBytes(1024 * 1024 + 1);
            if (originalResponseBytes.length > 1024 * 1024) {
              throw new IOException("Signing key response too large");
            }

            byte[] filteredResponseBytes =
                filter(new String(originalResponseBytes, StandardCharsets.UTF_8), issuerUrl)
                    .getBytes(StandardCharsets.UTF_8);

            HttpHeaders filteredResponseHeaders = new HttpHeaders();
            filteredResponseHeaders.putAll(originalResponse.getHeaders());
            filteredResponseHeaders.setContentLength(filteredResponseBytes.length);

            return new ClientHttpResponse() {
              @Override
              public HttpStatusCode getStatusCode() throws IOException {
                return originalResponse.getStatusCode();
              }

              @Override
              public String getStatusText() throws IOException {
                return originalResponse.getStatusText();
              }

              @Override
              public HttpHeaders getHeaders() {
                return filteredResponseHeaders;
              }

              @Override
              public InputStream getBody() {
                return new ByteArrayInputStream(filteredResponseBytes);
              }

              @Override
              public void close() {
                originalResponse.close();
              }
            };
          } catch (IOException | RuntimeException failure) {
            originalResponse.close();
            throw failure;
          }
        });
    return restTemplate;
  }

  static String filter(String responseJson, String issuerUrl) throws IOException {
    try {
      Map<String, Object> responseDocument = JSONObjectUtils.parse(responseJson);
      List<Object> signingKeyEntries = JSONObjectUtils.getJSONArray(responseDocument, "keys");
      List<Object> allowedSigningKeys =
          signingKeyEntries.stream()
              .filter(signingKeyEntry -> {
                if (!(signingKeyEntry instanceof Map<?, ?> signingKeyMap)
                    || !(signingKeyMap.get("issuer") instanceof String signingKeyIssuerUrl)) {
                  return false;
                }

                return signingKeyIssuerUrl.equals(issuerUrl)
                    || signingKeyIssuerUrl.equals(
                        "https://login.microsoftonline.com/{tenantid}/v2.0");
              })
              .toList();

      return JSONObjectUtils.toJSONString(Map.of("keys", allowedSigningKeys));
    } catch (ParseException failure) {
      throw new IOException("Invalid signing key response", failure);
    }
  }
}
