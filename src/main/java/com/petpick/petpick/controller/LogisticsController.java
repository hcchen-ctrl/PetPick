// LogisticsController.java
package com.petpick.petpick.controller;

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

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.CvsSelectRequest;
import com.petpick.petpick.mac.EcpayCheckMac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/logistics/cvs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LogisticsController {

    private final EcpayProperties prop;

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 產生超商選店頁用的表單（HTML），前端解析後 submit。
     */
    @PostMapping(path = "/map", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> openMap(@RequestBody CvsSelectRequest req) {

        // 1) 基本檢查
        if (req == null || req.getOrderId() == null) {
            return ResponseEntity.badRequest().body("orderId is required");
        }

        // 使用者要求的品牌/是否代收（前端可傳），先帶上預設
        String subType = blank(req.getSubType()) ? "FAMIC2C" : req.getSubType(); // 7-11/FAMI/HILIFE/OK
        String isCollection = blank(req.getIsCollection()) ? "N" : req.getIsCollection();

        // ☆ 測試環境：固定走全家 + 不代收，才會出現可選門市
        if (prop.isStage()) {
            subType = "FAMIC2C";
            isCollection = "N";
        }

        // 2) 端點（測試/正式）
        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";

        // 3) 金鑰組（物流用）
        var logi = prop.getLogistics();
        String mid = nz(logi == null ? null : logi.getMerchantId());
        String key = nz(logi == null ? null : logi.getHashKey());
        String iv  = nz(logi == null ? null : logi.getHashIv());
        String serverReplyUrl = nz(logi == null ? null : logi.getCvsMapReturnUrl());

        if (blank(mid) || blank(key) || blank(iv) || blank(serverReplyUrl)) {
            log.error("[Logi-ERROR] Missing logistics config. MID={}, Key(4)={}, IV(4)={}, ServerReplyURL={}",
                    mid, safe4(key), safe4(iv), serverReplyUrl);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Logistics config missing: please check MerchantID/HashKey/HashIV/ServerReplyURL");
        }

        // 4) 商家交易編號（<=20 碼、英數）
        String merchantTradeNo = "L" + LocalDateTime.now().format(NO);

        // 5) 組參數（原值；EcpayCheckMac 會自動忽略空字串）
        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", subType);
        p.put("IsCollection", isCollection);
        p.put("ServerReplyURL", serverReplyUrl);
        p.put("Device", "0"); // 0=PC, 1=Mobile；測試多用 0 比較穩定
        // 讓回傳能知道是哪筆訂單
        p.put("ExtraData", String.valueOf(req.getOrderId()));

        // 6) 產簽（物流金鑰/向量）
        String mac = EcpayCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        // 7) Debug
        log.warn("[Logi-DEBUG] stage={}, action={}, MID={}, Key(4)={}, IV(4)={}, subType={}, isCollection={}, serverReply={}",
                prop.isStage(), action, mid, safe4(key), safe4(iv), subType, isCollection, serverReplyUrl);
        p.forEach((k, v) -> log.warn("[Logi-DEBUG] {}={}", k, v));

        // 8) 回傳 HTML form（無 inline script）
        String html = buildFormHtml(action, p);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /**
     * 選店完成回拋（瀏覽器 POST）— 顯示選店結果或導回結帳頁。
     * ECPay 這個回拋不是 server-to-server，因此不需要回 "1|OK"。
     */
    @PostMapping(path = "/store-return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> storeReturn(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> m = toFlatMap(form);

        // 讀物流金鑰
        var logi = prop.getLogistics();
        String key = nz(logi == null ? null : logi.getHashKey());
        String iv  = nz(logi == null ? null : logi.getHashIv());

        // 驗簽（若有帶 CheckMacValue）
        boolean macOk = true;
        if (m.containsKey("CheckMacValue")) {
            try {
                macOk = EcpayCheckMac.verify(m, key, iv);
            } catch (Exception e) {
                macOk = false;
            }
        }

        // Log 回來的欄位
        log.warn("[Logi-RETURN] macOk={}, data={}", macOk, m);

        // 取常見欄位
        String orderId = nz(m.get("ExtraData"));
        String brand = nz(m.get("LogisticsSubType"));
        String storeId = nz(m.get("CVSStoreID"));
        String storeName = nz(m.get("CVSStoreName"));
        String storeAddr = nz(m.get("CVSAddress"));

        // 回一個簡單頁面（你可改成 redirect 到 /checkout.html 並帶 querystring）
        String page = """
        <!doctype html>
        <html lang="zh-Hant">
        <head><meta charset="utf-8"><title>門市已選擇</title></head>
        <body style="font-family:sans-serif">
          <h3>門市已選擇</h3>
          <ul>
            <li>訂單編號：%s</li>
            <li>物流通路：%s</li>
            <li>門市代碼：%s</li>
            <li>門市名稱：%s</li>
            <li>門市地址：%s</li>
            <li>簽章驗證：%s</li>
          </ul>
          <p><a href="/checkout.html">返回結帳頁</a></p>
        </body>
        </html>
        """.formatted(escapeHtml(orderId),
                      escapeHtml(brand),
                      escapeHtml(storeId),
                      escapeHtml(storeName),
                      escapeHtml(storeAddr),
                      macOk ? "通過" : "未通過");

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
    }

    // ===== Helpers =====

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
    private static String nz(String s) {
        return s == null ? "" : s;
    }
    private static String safe4(String s) {
        if (blank(s)) return "null";
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
          <p>請稍後，正在開啟超商選店頁…</p>
          <noscript><button type="submit" form="cvsForm">若未自動跳轉請按此</button></noscript>
        </body>
        </html>
        """.formatted(escapeHtml(action), inputs.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#x27;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
