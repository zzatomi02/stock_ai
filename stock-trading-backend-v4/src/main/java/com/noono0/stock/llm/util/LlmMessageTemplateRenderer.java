package com.noono0.stock.llm.util;

import java.util.Map;

public final class LlmMessageTemplateRenderer {
    private LlmMessageTemplateRenderer() {}

    public static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String s = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            String key = e.getKey();
            if (key == null) {
                continue;
            }
            String val = e.getValue() == null ? "" : e.getValue();
            s = s.replace("{{" + key + "}}", val);
        }
        return s;
    }
}
