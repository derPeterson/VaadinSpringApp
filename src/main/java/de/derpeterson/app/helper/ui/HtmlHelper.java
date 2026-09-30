package de.derpeterson.app.helper.ui;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlHelper {

    public static String ensureParagraphTags(String input) {
        if (StringUtils.isBlank(input)) {
            return "<p></p>";
        }

        input = StringUtils.trim(input);

        if (!Strings.CS.startsWith(input, "<p>")) {
            input = "<p>" + input;
        }
        if (!Strings.CS.endsWith(input, "</p>")) {
            input = input + "</p>";
        }
        return input;
    }
}
