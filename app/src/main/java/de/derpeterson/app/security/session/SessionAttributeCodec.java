package de.derpeterson.app.security.session;

import de.derpeterson.app.security.SessionAdmissionService.Active;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Feste JSON-Typen statt Java-Deserialisierung oder polymorphem DefaultTyping. */
public final class SessionAttributeCodec {
  private static final int MAX_BYTES = 65536;
  private final JsonMapper mapper = JsonMapper.builder().build();

  private record Value(int version, String kind, JsonNode value) {}

  private record Csrf(String header, String parameter, String token) {}

  public byte[] encode(Object value) {
    String kind;
    Object payload = value;
    if (value instanceof DurableAuthentication) {
      kind = "authentication";
    } else if (value instanceof Active) {
      kind = "admission";
    } else if (value instanceof CsrfToken token) {
      kind = "csrf";
      payload = new Csrf(token.getHeaderName(), token.getParameterName(), token.getToken());
    } else if (value instanceof String) {
      kind = "text";
    } else if (value instanceof Boolean) {
      kind = "flag";
    } else {
      throw new IllegalArgumentException("Unsupported durable session attribute");
    }
    byte[] bytes = mapper.writeValueAsBytes(new Value(1, kind, mapper.valueToTree(payload)));
    if (bytes.length > MAX_BYTES) {
      throw new IllegalArgumentException("Durable session attribute too large");
    }
    return bytes;
  }

  public Object decode(byte[] bytes) {
    if (bytes.length > MAX_BYTES) {
      throw new IllegalArgumentException("Durable session attribute too large");
    }
    Value value = mapper.readValue(bytes, Value.class);
    if (value == null
        || value.version() != 1
        || value.kind() == null
        || value.value() == null
        || value.value().isNull()) {
      throw new IllegalArgumentException("Unsupported durable session version");
    }
    return switch (value.kind()) {
      case "authentication" -> mapper.treeToValue(value.value(), DurableAuthentication.class);
      case "admission" -> mapper.treeToValue(value.value(), Active.class);
      case "csrf" -> {
        Csrf csrf = mapper.treeToValue(value.value(), Csrf.class);
        yield new DefaultCsrfToken(csrf.header(), csrf.parameter(), csrf.token());
      }
      case "text" -> mapper.treeToValue(value.value(), String.class);
      case "flag" -> mapper.treeToValue(value.value(), Boolean.class);
      default -> throw new IllegalArgumentException("Unknown durable session attribute type");
    };
  }
}
