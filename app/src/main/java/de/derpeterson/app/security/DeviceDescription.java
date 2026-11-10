package de.derpeterson.app.security;

/** Nur grobe Hinweise aus dem User-Agent, niemals ein eindeutiger Gerätenachweis. */
public record DeviceDescription(String system, String browser, boolean mobile)
    implements java.io.Serializable {
  private static final String ANDROID = "Android";

  public static DeviceDescription from(String userAgentHeader) {
    String boundedUserAgent =
        userAgentHeader == null
            ? ""
            : userAgentHeader.substring(0, Math.min(userAgentHeader.length(), 512));

    String operatingSystemName = detectSystem(boundedUserAgent);
    String browserName = detectBrowser(boundedUserAgent);

    return new DeviceDescription(
        operatingSystemName,
        browserName,
        boundedUserAgent.contains("Mobile")
            || boundedUserAgent.contains(ANDROID)
            || boundedUserAgent.contains("iPad"));
  }

  private static String detectSystem(String boundedUserAgent) {
    if (boundedUserAgent.contains(ANDROID)) {
      return ANDROID;
    }
    if (boundedUserAgent.contains("iPhone") || boundedUserAgent.contains("iPad")) {
      return "iOS";
    }
    if (boundedUserAgent.contains("Windows")) {
      return "Windows";
    }
    if (boundedUserAgent.contains("Macintosh")) {
      return "macOS";
    }
    if (boundedUserAgent.contains("Linux")) {
      return "Linux";
    }
    return "";
  }

  private static String detectBrowser(String boundedUserAgent) {
    if (boundedUserAgent.contains("Edg/")
        || boundedUserAgent.contains("EdgA/")
        || boundedUserAgent.contains("EdgiOS/")) {
      return "Edge";
    }
    if (boundedUserAgent.contains("Firefox/") || boundedUserAgent.contains("FxiOS/")) {
      return "Firefox";
    }
    if (boundedUserAgent.contains("Chrome/") || boundedUserAgent.contains("CriOS/")) {
      return "Chrome";
    }
    if (boundedUserAgent.contains("Safari/")) {
      return "Safari";
    }
    return "";
  }
}
