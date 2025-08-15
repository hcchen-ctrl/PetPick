package com.petpick.petpick.mac;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class EcpayCheckMac {
    public static String generate(Map<String, String> params, String hashKey, String hashIv) {
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : params.entrySet()) {
            String k = e.getKey(), v = e.getValue();
            if (k.equalsIgnoreCase("CheckMacValue") || v == null || v.isBlank()) continue;
            map.put(k, v);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("HashKey=").append(hashKey);
        for (var e : map.entrySet()) {
            sb.append("&").append(e.getKey()).append("=").append(e.getValue());
        }
        sb.append("&HashIV=").append(hashIv);

        String encoded = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).toLowerCase();
        // .NET urlencode 修正（官方附錄要求）
        encoded = encoded
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(encoded.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02X", b));
            return hex.toString(); // 大寫
        } catch (Exception e) {
            throw new RuntimeException("CheckMacValue error", e);
        }
    }

    public static boolean verify(Map<String, String> params, String hashKey, String hashIv) {
        String recv = params.getOrDefault("CheckMacValue", "");
        return recv.equalsIgnoreCase(generate(params, hashKey, hashIv));
    }
}