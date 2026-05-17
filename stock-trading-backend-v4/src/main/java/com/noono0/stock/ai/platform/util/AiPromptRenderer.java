package com.noono0.stock.ai.platform.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiPromptRenderer {
    private static final Pattern VAR = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    private AiPromptRenderer() {}

    public static String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        Matcher m = VAR.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String val = vars.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : val));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
