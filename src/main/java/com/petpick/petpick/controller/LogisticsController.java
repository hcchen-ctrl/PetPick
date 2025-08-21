package com.petpick.petpick.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.CvsSelectRequest;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/logistics/cvs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LogisticsController {

    private final EcpayProperties prop;
    private final OrderService orderService; // ★ 新增：回拋當下寫回 DB

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 產生超商選店頁用的表單（HTML），前端解析後 submit。
     */
    @PostMapping(path = "/map", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> openMap(@RequestBody CvsSelectRequest req) {

        if (req == null || req.getOrderId() == null) {
            return ResponseEntity.badRequest().body("orderId is required");
        }

        // 預設與測試環境處理
        String subType = blank(req.getSubType()) ? "FAMIC2C" : req.getSubType();
        String isCollection = blank(req.getIsCollection()) ? "N" : req.getIsCollection();
        if (prop.isStage()) {
            subType = "FAMIC2C";
            isCollection = "N";
        }

        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";

        var logi = prop.getLogistics();
        String mid = nz(logi == null ? null : logi.getMerchantId());
        String key = nz(logi == null ? null : logi.getHashKey());
        String iv = nz(logi == null ? null : logi.getHashIv());
        String serverReplyUrl = nz(logi == null ? null : logi.getCvsMapReturnUrl());

        if (blank(mid) || blank(key) || blank(iv) || blank(serverReplyUrl)) {
            log.error("[Logi-ERROR] Missing logistics config. MID={}, Key(4)={}, IV(4)={}, ServerReplyURL={}",
                    mid, safe4(key), safe4(iv), serverReplyUrl);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Logistics config missing: please check MerchantID/HashKey/HashIV/ServerReplyURL");
        }

        // ≤ 20 碼英數
        String merchantTradeNo = "L" + LocalDateTime.now().format(NO);

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", subType);
        p.put("IsCollection", isCollection);
        p.put("ServerReplyURL", serverReplyUrl);
        p.put("Device", "0"); // 0=PC
        // 讓回拋知道是哪張訂單
        p.put("ExtraData", String.valueOf(req.getOrderId()));

        String mac = EcpayCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        log.info("[Logi-MAP] stage={}, action={}, MID={}, Key(4)={}, IV(4)={}, subType={}, isCollection={}, reply={}",
                prop.isStage(), action, mid, safe4(key), safe4(iv), subType, isCollection, serverReplyUrl);

        String html = buildFormHtml(action, p);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /**
     * 選店完成回拋（瀏覽器 POST）。做法 B：302 導回你自己的頁面，並在此處即時寫回 DB。 此回拋為
     * browser-to-server，不需回 "1|OK"。
     */
@PostMapping(path = "/store-return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> storeReturn(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> m = new LinkedHashMap<>();
        form.forEach((k, v) -> m.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));

        String orderIdStr = nz(m.get("ExtraData"));
        String brandCode  = nz(m.getOrDefault("LogisticsSubType",
                                m.getOrDefault("SubType", m.getOrDefault("CVSSubType", ""))));
        String storeId    = nz(m.getOrDefault("CVSStoreID", m.getOrDefault("StoreID", "")));
        String storeNm    = nz(m.getOrDefault("CVSStoreName", m.getOrDefault("StoreName", "")));
        String addr       = nz(m.getOrDefault("CVSAddress", m.getOrDefault("StoreAddress", "")));

        // ★ 在這裡就寫回 DB（會同時把 shipping_type 設成 cvs_cod 並做品牌正規化）
        try {
            Integer oid = Integer.valueOf(orderIdStr);
            if (!storeId.isBlank()) {
                orderService.setStoreInfo(oid, brandCode, storeId, storeNm, addr);
            }
        } catch (Exception e) {
            // log 但不擋使用者導回
            // log.warn("store-return save failed", e);
        }

        // 導去你的結果頁（或結帳頁）
        String redirect = UriComponentsBuilder.fromPath("/cvs-selected.html")
                .queryParam("orderId", orderIdStr)
                .queryParam("brand", brandCode)
                .queryParam("id", storeId)
                .queryParam("name", storeNm)
                .queryParam("addr", addr)
                .build()
                .toUriString();
        return ResponseEntity.status(302).location(URI.create(redirect)).build();
    }
    // ===== Helpers =====
    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String safe4(String s) {
        if (blank(s)) {
            return "null";
        }
        return s.length() < 4 ? "**" : s.substring(0, 2) + "**" + s.substring(s.length() - 2);
    }

    private static Map<String, String> toFlatMap(MultiValueMap<String, String> form) {
        Map<String, String> out = new LinkedHashMap<>();
        form.forEach((k, v) -> out.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));
        return out;
    }

    private static String buildFormHtml(String action, Map<String, String> params) {
        StringBuilder inputs = new StringBuilder();
        params.forEach((k, v) -> inputs.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(k)).append("\" value=\"")
                .append(escapeHtml(v)).append("\"/>\n"));

        return """
        <!doctype html>
        <html lang="zh-Hant">
        <head><meta charset="utf-8"><title>CVS Map</title></head>
        <body style="font-family:sans-serif">
          <form id="cvsForm" method="post" action="%s">
            %s
          </form>
          <script>document.getElementById('cvsForm').submit();</script>
          <noscript><button type="submit" form="cvsForm">若未自動跳轉請按此</button></noscript>
        </body>
        </html>
        """.formatted(escapeHtml(action), inputs.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' ->
                    out.append("&amp;");
                case '<' ->
                    out.append("&lt;");
                case '>' ->
                    out.append("&gt;");
                case '"' ->
                    out.append("&quot;");
                case '\'' ->
                    out.append("&#x27;");
                default ->
                    out.append(c);
            }
        }
        return out.toString();
    }
}
