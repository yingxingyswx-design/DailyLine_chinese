package com.example.yiyan;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 把用户一言导出成 XML。
 *
 * <p>格式与 {@link QuoteImporter} 完全对应，导出文件可以直接用「导入一言」重新读回来：
 * <pre>{@code
 * <quotes count="2" exportedAt="2026-11-05 22:30:00">
 *   <quote text="人生如逆旅，我亦是行人。" author="苏轼" work="临江仙"
 *          category="诗词" created="1699200000000"/>
 *   <quote text="慢慢来，比较快。" category="随想"/>
 * </quotes>
 * }</pre>
 *
 * <p>{@code text} 必填，{@code author / work / category} 为空时省略，{@code created} 是添加时间
 * （epoch 毫秒，旧条目没有时间时不写）。{@code count / exportedAt} 只是给人看的，导入时会忽略。
 */
final class QuoteExporter {

    static final class Result {
        int exported;
        String error;
    }

    private static final String ROOT_TAG = "quotes";
    private static final String ITEM_TAG = "quote";

    private QuoteExporter() {
    }

    /**
     * 把 {@code quotes} 逐条写成 XML 到 {@code out}，不关闭输出流（由调用方负责）。
     * 句子为空的条目会被跳过，{@link Result#exported} 是真正写出的条数。
     */
    static Result exportXml(OutputStream out, List<Quote> quotes) {
        Result result = new Result();
        List<Quote> valid = new ArrayList<>();
        if (quotes != null) {
            for (Quote quote : quotes) {
                if (quote != null && !quote.text.trim().isEmpty()) valid.add(quote);
            }
        }

        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "utf-8");
            serializer.startDocument("utf-8", true);
            serializer.comment(" DailyLine 用户一言导出，可直接在「设置 → 导入一言（XML）」重新导入 ");
            serializer.startTag(null, ROOT_TAG);
            serializer.attribute(null, "count", String.valueOf(valid.size()));
            serializer.attribute(null, "exportedAt",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date()));
            for (Quote quote : valid) {
                serializer.startTag(null, ITEM_TAG);
                serializer.attribute(null, "text", safe(quote.text));
                if (!quote.author.trim().isEmpty()) {
                    serializer.attribute(null, "author", safe(quote.author));
                }
                if (!quote.work.trim().isEmpty()) {
                    serializer.attribute(null, "work", safe(quote.work));
                }
                if (!quote.category.trim().isEmpty()) {
                    serializer.attribute(null, "category", safe(quote.category));
                }
                if (quote.createdAt > 0) {
                    serializer.attribute(null, "created", String.valueOf(quote.createdAt));
                }
                serializer.endTag(null, ITEM_TAG);
                result.exported++;
            }
            serializer.endTag(null, ROOT_TAG);
            serializer.endDocument();
            serializer.flush();
        } catch (Exception e) {
            result.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        return result;
    }

    /**
     * 属性值清洗：XML 1.0 不允许大部分控制字符，孤立代理项也无法按 UTF-8 写出。
     * 这些字符统一换成空格，避免导出文件坏掉。
     */
    private static String safe(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0xFFFE || c == 0xFFFF) {
                out.append(' ');
            } else if (Character.isHighSurrogate(c)) {
                if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                    out.append(c).append(value.charAt(++i));
                } else {
                    out.append(' ');
                }
            } else if (Character.isLowSurrogate(c)) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
