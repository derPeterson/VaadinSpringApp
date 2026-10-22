package de.derpeterson.app.security;

/** Nur grobe Hinweise aus dem User-Agent, niemals ein eindeutiger Gerätenachweis. */
public record DeviceDescription(String system, String browser, boolean mobile)
    implements java.io.Serializable {
  private static final String ANDROID = "Android";

  public static DeviceDescription from(String agent) {
    String value = agent == null ? "" : agent.substring(0, Math.min(agent.length(), 512));
    String system = detectSystem(value);
    String browser = detectBrowser(value);
    return new DeviceDescription(
        system,
        browser,
        value.contains("Mobile") || value.contains(ANDROID) || value.contains("iPad"));
  }

  private static String detectSystem(String value) {
    if (value.contains(ANDROID)) {
      return ANDROID;
    }
    if (value.contains("iPhone") || value.contains("iPad")) {
      return "iOS";
    }
    if (value.contains("Windows")) {
      return "Windows";
    }
    if (value.contains("Macintosh")) {
      return "macOS";
    }
    if (value.contains("Linux")) {
      return "Linux";
    }
    return "";
  }

  private static String detectBrowser(String value) {
    if (value.contains("Edg/") || value.contains("EdgA/") || value.contains("EdgiOS/")) {
      return "Edge";
    }
    if (value.contains("Firefox/") || value.contains("FxiOS/")) {
      return "Firefox";
    }
    if (value.contains("Chrome/") || value.contains("CriOS/")) {
      return "Chrome";
    }
    if (value.contains("Safari/")) {
      return "Safari";
    }
    return "";
  }
}
