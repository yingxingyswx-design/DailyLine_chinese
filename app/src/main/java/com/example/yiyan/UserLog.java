package com.example.yiyan;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 用户一言的「日志」：把 {@code custom_quotes} 里的条目按添加时间倒序整理出来。
 *
 * <p>添加时间写在 {@link Quote#createdAt} 里，随 {@link Quote#toLine()} 一起存储；
 * 旧版本留下的条目没有时间戳（{@code createdAt == 0}），日志里显示「添加时间未记录」。
 *
 * <p>打开 {@link #KEY_MODE} 后，桌面小组件只从这份日志里取句子。
 */
final class UserLog {

    /** 日志模式：小组件只显示用户自己添加的一言。 */
    static final String KEY_MODE = "logMode";

    /** 用户添加的一言（{@link Quote#toLine()} 行的集合）。 */
    static final String KEY_CUSTOM_QUOTES = "custom_quotes";

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String UNKNOWN_TIME = "添加时间未记录";

    private UserLog() {
    }

    static boolean enabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_MODE, false);
    }

    static Set<String> lines(SharedPreferences prefs) {
        return prefs.getStringSet(KEY_CUSTOM_QUOTES, new HashSet<String>());
    }

    /**
     * 全部用户添加的一言。按「同一句 + 同一作者 + 同一出处」去重，时间新的排在前面；
     * 没有时间戳的旧条目排在最后。只读，不会改动 prefs。
     */
    static List<Quote> quotes(SharedPreferences prefs) {
        Map<String, Quote> unique = new LinkedHashMap<>();
        for (String line : lines(prefs)) {
            Quote quote = Quote.parse(line, "用户添加", "用户");
            if (quote == null || quote.text.trim().isEmpty()) continue;
            String key = quote.text + "\u0001" + quote.author + "\u0001" + quote.work;
            Quote old = unique.get(key);
            // 同一句有多份时保留时间最新的一份，旧版本无时间的记录会被新记录替换
            if (old == null || quote.createdAt > old.createdAt) {
                unique.put(key, quote);
            }
        }

        List<Quote> list = new ArrayList<>(unique.values());
        list.sort((left, right) -> {
            if (left.createdAt == right.createdAt) return 0;
            if (left.createdAt <= 0) return 1;
            if (right.createdAt <= 0) return -1;
            return Long.compare(right.createdAt, left.createdAt);
        });
        return list;
    }

    static String dateTime(long createdAt) {
        if (createdAt <= 0) return "";
        return new SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(new Date(createdAt));
    }

    /** 日志列表用：「添加于 2026-11-05 22:10:24」；旧条目返回「添加时间未记录」。 */
    static String timeLabel(long createdAt) {
        String text = dateTime(createdAt);
        return text.isEmpty() ? UNKNOWN_TIME : "添加于 " + text;
    }

    /** 小组件元信息用：「2026-11-05 22:10:24 添加」；旧条目只写「时间未记录」。 */
    static String widgetTimeLabel(long createdAt) {
        String text = dateTime(createdAt);
        return text.isEmpty() ? "添加时间未记录" : text + " 添加";
    }
}
