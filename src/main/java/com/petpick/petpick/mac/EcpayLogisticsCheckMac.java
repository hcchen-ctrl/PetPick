package com.petpick.petpick.mac;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class EcpayLogisticsCheckMac {

    public static boolean verify(Map<String, String> params, String hashKey, String hashIv) {
        String recv = nvl(params.get("CheckMacValue"));
        return recv.equalsIgnoreCase(generate(params, hashKey, hashIv));
    }

    /** 物流用：UrlEncode → toLower → 7 置換 → MD5 → upper */
    public static String generate(Map<String, String> params, String hashKey, String hashIv) {
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.forEach((k, v) -> {
            if (!"CheckMacValue".equalsIgnoreCase(k) && v != null && !v.isBlank()) map.put(k, v);
        });

        StringBuilder sb = new StringBuilder("HashKey=").append(hashKey);
        for (var e : map.entrySet()) sb.append("&").append(e.getKey()).append("=").append(e.getValue());
        sb.append("&HashIV=").append(hashIv);

        String encodedLower = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT)
                .replace("%2d", "-").replace("%5f", "_").replace("%2e", ".")
                .replace("%21", "!").replace("%2a", "*").replace("%28", "(").replace("%29", ")");

        return md5(encodedLower).toUpperCase(Locale.ROOT);
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(out.length * 2);
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("CheckMac MD5 error", e);
        }
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}