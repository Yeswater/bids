package com.yeswater.bids.exec.application;

import org.springframework.stereotype.Service;

import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SqlDisplayFormatter {
    private static final Pattern NAMED_PARAMETER = Pattern.compile("(?<!:):([A-Za-z][A-Za-z0-9_]*)");

    public String toDisplaySql(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        Matcher matcher = NAMED_PARAMETER.matcher(sql);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!parameters.containsKey(name)) {
                continue;
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(toLiteral(parameters.get(name))));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String toLiteral(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof TemporalAccessor) {
            return "'" + value + "'";
        }
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }
}
