package com.myshareapp.config;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("fmt")
public class TemplateFormatters {

    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    public String money(Object val) {
        if (val == null) return "0.00";
        double v;
        if (val instanceof Number n) v = n.doubleValue();
        else {
            try { v = Double.parseDouble(String.valueOf(val)); } catch (Exception e) { v = 0.0; }
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    public String shortDate(Object val) {
        if (val == null) return "-";
        if (val instanceof LocalDateTime dt) return dt.format(SHORT_DATE);
        if (val instanceof LocalDate d) return d.format(SHORT_DATE);
        return String.valueOf(val);
    }

    public String dateTime(Object val) {
        if (val == null) return "-";
        if (val instanceof LocalDateTime dt) return dt.format(DATE_TIME);
        if (val instanceof LocalDate d) return d.format(SHORT_DATE);
        String s = String.valueOf(val).trim();
        if (s.isEmpty()) return "-";
        try {
            Instant inst = Instant.parse(s);
            return inst.atZone(NPT).format(DATE_TIME);
        } catch (Exception ignored) {
            return s;
        }
    }

    public String initial(Object text) {
        if (text == null || String.valueOf(text).trim().isEmpty()) return "U";
        return String.valueOf(text).trim().substring(0, 1).toUpperCase();
    }

    public String truncate(Object text, int max) {
        if (text == null) return "";
        String s = String.valueOf(text);
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    public String urlEncode(Object text) {
        return URLEncoder.encode(text == null ? "" : String.valueOf(text), StandardCharsets.UTF_8);
    }
}
