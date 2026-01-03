package io.github.kxng0109.chatwithdocs.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {
    private final Parser parser;
    private final TextContentRenderer renderer;

    public MarkdownService() {
        this.parser = Parser.builder().build();
        this.renderer = TextContentRenderer.builder().build();
    }

    public String stripMarkdown(String markdown) {
        if (markdown == null) return "";
        Node document = parser.parse(markdown);
        String cleanText = renderer.render(document);
        return cleanText.replaceAll("\\s+", " ").trim();
    }
}
