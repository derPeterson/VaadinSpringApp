package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class MicrosoftSigningKeysTest {
  @Test
  void signingKeyIssuerMustMatchConcreteTenantOrTheMicrosoftTemplate() throws Exception {
    String issuer = "https://login.microsoftonline.com/12345678-1234-1234-1234-123456789abc/v2.0";
    String document =
        "{\"keys\":[{\"kid\":\"a\",\"issuer\":\""
            + issuer
            + "\"},"
            + "{\"kid\":\"b\",\"issuer\":\"https://login.microsoftonline.com/{tenantid}/v2.0\"},"
            + "{\"kid\":\"c\",\"issuer\":\"https://other.example.test\"},{\"kid\":\"d\"}]}";
    java.util.Map<String, Object> filtered =
        com.nimbusds.jose.util.JSONObjectUtils.parse(MicrosoftSigningKeys.filter(document, issuer));

    assertEquals(2, com.nimbusds.jose.util.JSONObjectUtils.getJSONArray(filtered, "keys").size());
    assertThrows(IOException.class, () -> MicrosoftSigningKeys.filter("invalid", issuer));
  }
}
