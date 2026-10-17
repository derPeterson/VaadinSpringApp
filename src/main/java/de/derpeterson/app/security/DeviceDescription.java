package de.derpeterson.app.security;

/** Nur grobe Hinweise aus dem User-Agent, niemals ein eindeutiger Gerätenachweis. */
public record DeviceDescription(String system, String browser, boolean mobile)
    implements java.io.Serializable {
  public static DeviceDescription from(String agent) {
    String value = agent == null ? "" : agent.substring(0, Math.min(agent.length(), 512));
    String system =
        value.contains("Android")
            ? "Android"
            : value.contains("iPhone") || value.contains("iPad")
                ? "iOS"
                : value.contains("Windows")
                    ? "Windows"
                    : value.contains("Macintosh")
                        ? "macOS"
                        : value.contains("Linux") ? "Linux" : "";
    String browser =
        value.contains("Edg/") || value.contains("EdgA/") || value.contains("EdgiOS/")
            ? "Edge"
            : value.contains("Firefox/") || value.contains("FxiOS/")
                ? "Firefox"
                : value.contains("Chrome/") || value.contains("CriOS/")
                    ? "Chrome"
                    : value.contains("Safari/") ? "Safari" : "";
    return new DeviceDescription(
        system,
        browser,
        value.contains("Mobile") || value.contains("Android") || value.contains("iPad"));
  }
}
