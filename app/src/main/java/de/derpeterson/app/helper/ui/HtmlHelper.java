package de.derpeterson.app.helper.ui;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlHelper {

  public static String ensureParagraphTags(String htmlInputText) {
    if (StringUtils.isBlank(htmlInputText)) {
      return "<p></p>";
    }

    htmlInputText = StringUtils.trim(htmlInputText);

    if (!Strings.CS.startsWith(htmlInputText, "<p>")) {
      htmlInputText = "<p>" + htmlInputText;
    }
    if (!Strings.CS.endsWith(htmlInputText, "</p>")) {
      htmlInputText = htmlInputText + "</p>";
    }
    return htmlInputText;
  }
}
