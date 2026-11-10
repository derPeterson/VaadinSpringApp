package de.derpeterson.app.security.session;

import de.derpeterson.app.security.SessionAdmissionService.Active;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Feste JSON-Typen statt Java-Deserialisierung oder polymorphem DefaultTyping. */
public final class SessionAttributeCodec {
  private static final int MAX_BYTES = 65536;
  private final JsonMapper sessionAttributeJsonMapper = JsonMapper.builder().build();

  private record Value(int version, String kind, JsonNode value) {}

  private record Csrf(String header, String parameter, String token) {}

  public byte[] encode(Object sessionAttribute) {
    String attributeKind;
    Object attributePayload = sessionAttribute;
    if (sessionAttribute instanceof DurableAuthentication) {
      attributeKind = "authentication";
    } else if (sessionAttribute instanceof Active) {
      attributeKind = "admission";
    } else if (sessionAttribute instanceof CsrfToken csrfToken) {
      attributeKind = "csrf";
      attributePayload =
          new Csrf(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    } else if (sessionAttribute instanceof String) {
      attributeKind = "text";
    } else if (sessionAttribute instanceof Boolean) {
      attributeKind = "flag";
    } else {
      throw new IllegalArgumentException("Unsupported durable session attribute");
    }

    byte[] encodedAttributeBytes =
        sessionAttributeJsonMapper.writeValueAsBytes(
            new Value(1, attributeKind, sessionAttributeJsonMapper.valueToTree(attributePayload)));

    if (encodedAttributeBytes.length > MAX_BYTES) {
      throw new IllegalArgumentException("Durable session attribute too large");
    }

    return encodedAttributeBytes;
  }

  public Object decode(byte[] encodedAttributeBytes) {
    if (encodedAttributeBytes.length > MAX_BYTES) {
      throw new IllegalArgumentException("Durable session attribute too large");
    }

    Value decodedAttribute =
        sessionAttributeJsonMapper.readValue(encodedAttributeBytes, Value.class);
    if (decodedAttribute == null
        || decodedAttribute.version() != 1
        || decodedAttribute.kind() == null
        || decodedAttribute.value() == null
        || decodedAttribute.value().isNull()) {
      throw new IllegalArgumentException("Unsupported durable session version");
    }

    return switch (decodedAttribute.kind()) {
      case "authentication" ->
          sessionAttributeJsonMapper.treeToValue(
              decodedAttribute.value(), DurableAuthentication.class);
      case "admission" ->
          sessionAttributeJsonMapper.treeToValue(decodedAttribute.value(), Active.class);
      case "csrf" -> {
        Csrf decodedCsrfToken =
            sessionAttributeJsonMapper.treeToValue(decodedAttribute.value(), Csrf.class);
        yield new DefaultCsrfToken(
            decodedCsrfToken.header(), decodedCsrfToken.parameter(), decodedCsrfToken.token());
      }
      case "text" -> sessionAttributeJsonMapper.treeToValue(decodedAttribute.value(), String.class);
      case "flag" ->
          sessionAttributeJsonMapper.treeToValue(decodedAttribute.value(), Boolean.class);
      default -> throw new IllegalArgumentException("Unknown durable session attribute type");
    };
  }
}
