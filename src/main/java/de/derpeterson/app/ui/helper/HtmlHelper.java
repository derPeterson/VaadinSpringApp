package de.derpeterson.app.ui.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlHelper {
    
    public static String ensureParagraphTags(String input) {
        if (StringUtils.isBlank(input)) {
            return "<p></p>";
        }

        input = StringUtils.trim(input);

        if (!StringUtils.startsWith(input, "<p>")) {
            input = "<p>" + input;
        }
        if (!StringUtils.endsWith(input, "</p>")) {
            input = input + "</p>";
        }
        return input;
    }
}
