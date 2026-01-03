package io.github.kxng0109.chatwithdocs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MarkdownServiceTest {
    @InjectMocks
    private MarkdownService markdownService;

    @Test
    void stripMarkdown_shouldStripBoldAndItalics() {
        String input = "This is **important** and *very* cool.";
        String expected = "This is important and very cool.";

        assertEquals(expected, markdownService.stripMarkdown(input));
    }

    @Test
    void stripMarkdown_shouldFlattenNewlinesAndLists() {
        String input = """
                Here is a list:
                * Item 1
                * Item 2
                
                End of list.
                """;

        String expected = "Here is a list: * Item 1 * Item 2 End of list.";

        assertEquals(expected, markdownService.stripMarkdown(input));
    }

    @Test
    void stripMarkdown_shouldFlattenLinksToText() {
        String input = "Click [Here](https://google.com) for more info.";
        String expected = "Click \"Here\" (https://google.com) for more info.";

        assertEquals(expected, markdownService.stripMarkdown(input));
    }

    @Test
    void stripMarkdown_shouldHandleHeaders() {
        String input = """
                # Header 1
                ## Header 2
                Content
                """;
        String expected = "Header 1 Header 2 Content";

        assertEquals(expected, markdownService.stripMarkdown(input));
    }

    @Test
    void stripMarkdown_shouldHandleNullAndEmptyInput() {
        assertEquals("", markdownService.stripMarkdown(null));
        assertEquals("", markdownService.stripMarkdown(""));
        assertEquals("", markdownService.stripMarkdown("   "));
    }
}
