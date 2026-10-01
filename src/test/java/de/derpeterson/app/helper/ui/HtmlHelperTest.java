package de.derpeterson.app.helper.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlHelperTest {

    @ParameterizedTest
    @CsvSource({
        "'Hello', <p>Hello</p>",
        "'  Hello  ', <p>Hello</p>",
        "'<p>Hello</p>', <p>Hello</p>",
        "'<p>Hello', <p>Hello</p>",
        "'Hello</p>', <p>Hello</p>"
    })
    void ensureParagraphTagsWrapsTextAccordingToContract(String input, String expected) {
        assertEquals(expected, HtmlHelper.ensureParagraphTags(input));
    }

    @Test
    void ensureParagraphTagsReturnsEmptyParagraphForNull() {
        assertEquals("<p></p>", HtmlHelper.ensureParagraphTags(null));
    }

    @Test
    void ensureParagraphTagsReturnsEmptyParagraphForEmptyText() {
        assertEquals("<p></p>", HtmlHelper.ensureParagraphTags(""));
    }

    @Test
    void ensureParagraphTagsReturnsEmptyParagraphForWhitespaceOnly() {
        assertEquals("<p></p>", HtmlHelper.ensureParagraphTags("   "));
    }
}
