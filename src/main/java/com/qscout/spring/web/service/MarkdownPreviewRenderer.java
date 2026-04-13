package com.qscout.spring.web.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownPreviewRenderer {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");

    public String render(String markdown) {
        String normalized = markdown.replace("\r\n", "\n");
        StringBuilder html = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        boolean inList = false;
        boolean inCodeBlock = false;

        for (String line : normalized.split("\n", -1)) {
            if (inCodeBlock) {
                if (line.startsWith("```")) {
                    html.append("</code></pre>");
                    inCodeBlock = false;
                } else {
                    html.append(HtmlUtils.htmlEscape(line)).append("\n");
                }
                continue;
            }

            if (line.startsWith("```")) {
                flushParagraph(html, paragraph);
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                String language = line.length() > 3 ? line.substring(3).trim() : "";
                html.append("<pre><code");
                if (!language.isBlank()) {
                    html.append(" class=\"language-").append(HtmlUtils.htmlEscape(language)).append("\"");
                }
                html.append(">");
                inCodeBlock = true;
                continue;
            }

            if (line.isBlank()) {
                flushParagraph(html, paragraph);
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                continue;
            }

            if (line.startsWith("#### ") || line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")) {
                flushParagraph(html, paragraph);
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                int level = headingLevel(line);
                String text = line.substring(level + 1).trim();
                html.append("<h").append(level).append(">")
                        .append(renderInline(text))
                        .append("</h").append(level).append(">");
                continue;
            }

            if (line.startsWith("- ")) {
                flushParagraph(html, paragraph);
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(renderInline(line.substring(2).trim())).append("</li>");
                continue;
            }

            if (inList) {
                html.append("</ul>");
                inList = false;
            }
            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(line.trim());
        }

        flushParagraph(html, paragraph);
        if (inList) {
            html.append("</ul>");
        }
        if (inCodeBlock) {
            html.append("</code></pre>");
        }
        return html.toString();
    }

    private int headingLevel(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        return Math.max(1, Math.min(level, 4));
    }

    private void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        html.append("<p>").append(renderInline(paragraph.toString())).append("</p>");
        paragraph.setLength(0);
    }

    private String renderInline(String text) {
        StringBuilder html = new StringBuilder();
        Matcher matcher = LINK_PATTERN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            html.append(HtmlUtils.htmlEscape(text.substring(last, matcher.start())));
            String label = matcher.group(1);
            String url = matcher.group(2);
            if (isSafeUrl(url)) {
                html.append("<a href=\"")
                        .append(HtmlUtils.htmlEscape(url))
                        .append("\">")
                        .append(HtmlUtils.htmlEscape(label))
                        .append("</a>");
            } else {
                html.append(HtmlUtils.htmlEscape(label));
            }
            last = matcher.end();
        }
        html.append(HtmlUtils.htmlEscape(text.substring(last)));
        return html.toString();
    }

    private boolean isSafeUrl(String url) {
        return url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://");
    }
}
