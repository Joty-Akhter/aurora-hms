package com.easyops.communication.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables == null ? null : variables.get(key);
            if (value == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required placeholder variable: " + key);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
