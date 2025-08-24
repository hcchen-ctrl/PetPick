// File: src/main/java/com/petpick/petpick/service/LogisticsEcpayService.java
package com.petpick.petpick.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.HomeCreateRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsEcpayService {

    private final EcpayProperties prop;
    private final OrderRepository orderRepo;
    private final RestTemplate rest = new RestTemplate();

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ECPAY_TS = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // ====== DTOs ======
    public static class HomeCreateResult {
        public boolean ok;
        public String error;       // 失敗訊息
        public String logisticsId; // AllPayLogisticsID
        public String trackingNo;  // ShipmentNo（黑貓託運單號；可能為 null，等回拋/查詢補上）
    }

    public static class QueryResult {
        public boolean ok;
        public String error;
        public String logisticsId;
        public String trackingNo;     // ShipmentNo
        public String logisticsStatus;
        public String raw;            // for debug
    }

    // ====== 建單（Home / TCAT） ======
    /** 綠界宅配建單（TCAT） */
    public HomeCreateResult createHomeShipment(HomeCreateRequest req) {
        HomeCreateResult r = new HomeCreateResult();
        if (req == null || req.getOrderId() == null) {
            r.ok = false; r.error = "orderId is required"; return r;
        }
        Integer oid = req.getOrderId();
        log.info("[HomeCreate][svc] lookup orderId={}", oid);

        Order o = orderRepo.findById(oid).orElse(null);
        if (o == null) return fail(r, "Order not found: " + oid);

        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                : "https://logistics.ecpay.com.tw/Express/Create";

        var logi = prop.getLogistics();
        String mid   = nz(logi.getMerchantId());
        String key   = nz(logi.getHashKey());
        String iv    = nz(logi.getHashIv());
        String srvCb = nz(logi.getHomeServerReplyUrl());

        String senderName  = firstNonBlank(logi.getSenderName(),  "Petpick");
        String senderPhone = firstNonBlank(logi.getSenderPhone(), "0223456789");
        String senderZip   = firstNonBlank(logi.getSenderZip(),   "100");
        String senderAddr  = firstNonBlank(logi.getSenderAddress(),"台北市中正區中山南路1號");

        if (blank(mid) || blank(key) || blank(iv) || blank(srvCb)) {
            return fail(r, "Logistics config missing: MerchantID/HashKey/HashIV/HomeServerReplyUrl");
        }

        String recvName  = firstNonBlank(req.getReceiverName(),  o.getReceiverName());
        String recvPhone = firstNonBlank(req.getReceiverPhone(), o.getReceiverPhone());
        String recvZip   = firstNonBlank(req.getReceiverZip(),   firstNonBlank(o.getReceiverZip(), "100"));
        String recvAddr  = firstNonBlank(req.getReceiverAddr(),  o.getAddr());
        if (recvName.isBlank() || recvPhone.isBlank() || recvAddr.isBlank()) {
            return fail(r, "Receiver fields missing: name/phone/address are required");
        }

        if (!isValidReceiverName(recvName)) {
            return fail(r, "ReceiverName invalid: 中文需2~5字，英文需4~10字（不含空白與符號）");
        }
        String recvNameForEcpay = recvName.trim().replaceAll("\\s+", "")
                .replaceAll("[^A-Za-z\\u4E00-\\u9FFF]", "");

        boolean collect = Boolean.TRUE.equals(req.getIsCollection());
        int goodsAmt   = Math.max(0, n(o.getTotalPrice()));
        int collectAmt = collect ? goodsAmt : 0;

        String merchantTradeNo = buildNo("H", o.getOrderId()); // <= 20 字

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("MerchantTradeDate", LocalDateTime.now().format(ECPAY_TS));
        p.put("LogisticsType", "Home");
        p.put("LogisticsSubType", "TCAT");
        p.put("GoodsAmount", String.valueOf(goodsAmt));
        if (collect) p.put("CollectionAmount", String.valueOf(collectAmt));
        p.put("IsCollection", collect ? "Y" : "N");
        p.put("GoodsName", "Petpick商品");

        p.put("ReceiverName",      recvNameForEcpay);
        p.put("ReceiverPhone",     recvPhone);
        p.put("ReceiverCellPhone", recvPhone);
        p.put("ReceiverZipCode",   recvZip);
        p.put("ReceiverAddress",   recvAddr);

        p.put("SenderName",    senderName);
        p.put("SenderPhone",   senderPhone);
        p.put("SenderZipCode", senderZip);
        p.put("SenderAddress", senderAddr);

        p.put("Temperature",  "0001");
        p.put("Distance",     "00");
        p.put("Specification","0001");

        p.put("ServerReplyURL", srvCb);
        p.put("ExtraData", String.valueOf(o.getOrderId()));

        // ★ 物流用 MD5（非金流 SHA256）
        String mac = generateCheckMacForLogistics(p, key, iv);
        p.put("CheckMacValue", mac);

        log.info("[HomeCreate] signBase={} mac={}", signBaseForLog(p), mac);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        p.forEach(form::add);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> resp;
        try {
            resp = rest.postForEntity(action, new HttpEntity<>(form, headers), String.class);
        } catch (Exception ex) {
            log.error("[HomeCreate] HTTP call failed: {}", ex.getMessage());
            return fail(r, "HTTP call failed: " + ex.getMessage());
        }

        String rawBody = nz(resp.getBody());
        log.info("[HomeCreate] HTTP={} action={} body={}", resp.getStatusCodeValue(), action, rawBody);

        if (!resp.getStatusCode().is2xxSuccessful())
            return fail(r, "HTTP " + resp.getStatusCodeValue() + " body=" + rawBody);
        if (rawBody.isBlank())
            return fail(r, "Empty response from ECPay.");

        // 去掉前綴「1| / 0|」
        String kvPart = normalizeEcpayBody(rawBody);
        Map<String, String> kv = parseKv(kvPart);

        // 轉 Case-Insensitive Map
        Map<String, String> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        kv.forEach(m::put);
        log.info("[HomeCreate] parsed kv = {}", m);

        // 1=成功；300=訂單處理中
        String rtn = m.getOrDefault("RtnCode", "");
        String msg = m.getOrDefault("RtnMsg", "");
        if (!"1".equals(rtn) && !"300".equals(rtn)) {
            return fail(r, "ECPay Create failed: " + (msg.isBlank() ? rawBody : msg));
        }

        r.ok         = true;
        r.logisticsId= nz(m.get("AllPayLogisticsID")); // 建單回覆的物流ID
        r.trackingNo = nz(m.get("ShipmentNo"));        // 宅配黑貓託運單號（可能尚未給）

        // ===== 僅在「非空」時才更新資料庫欄位 =====
        try {
            o.setShippingType("address");

            if (notBlank(r.logisticsId)) o.setLogisticsId(r.logisticsId.trim());
            if (notBlank(r.trackingNo))  o.setTrackingNo(r.trackingNo.trim());

            o.setLogisticsStatus("CREATED"); // 等回拋/查詢補追蹤碼
            orderRepo.saveAndFlush(o);

            log.info("[HomeCreate] saved orderId={} logisticsId={} trackingNo={}",
                    o.getOrderId(), o.getLogisticsId(), o.getTrackingNo());
        } catch (Exception ex) {
            log.warn("[HomeCreate] save order failed: {}", ex.getMessage());
        }

        // 讓前端可判斷：空字串改成 null
        if (!notBlank(r.trackingNo))  r.trackingNo  = null;
        if (!notBlank(r.logisticsId)) r.logisticsId = null;

        return r;
    }

    // ====== 查詢宅配單（補追蹤碼） ======
    /**
     * 查詢宅配單資訊（以訂單的 AllPayLogisticsID 為主），若取得 ShipmentNo / LogisticsStatus 會自動寫回 DB。
     * 典型用法：建單後幾秒、或在後台點「刷新追蹤碼」時呼叫。
     */
    public QueryResult queryHomeTradeInfoByOrder(Integer orderId) {
        QueryResult out = new QueryResult();
        out.ok = false;

        Order o = orderRepo.findById(orderId).orElse(null);
        if (o == null) {
            out.error = "Order not found: " + orderId;
            return out;
        }
        String logisticsId = nz(o.getLogisticsId());
        if (logisticsId.isBlank()) {
            out.error = "LogisticsId missing on order " + orderId + " (create first)";
            return out;
        }

        var logi = prop.getLogistics();
        String mid = nz(logi.getMerchantId());
        String key = nz(logi.getHashKey());
        String iv  = nz(logi.getHashIv());
        if (blank(mid) || blank(key) || blank(iv)) {
            out.error = "Logistics config missing: MerchantID/HashKey/HashIV";
            return out;
        }

        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Helper/QueryLogisticsTradeInfo"
                : "https://logistics.ecpay.com.tw/Helper/QueryLogisticsTradeInfo";

        // 官方文件：QueryLogisticsTradeInfo 參數（常見）
        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("AllPayLogisticsID", logisticsId);
        // 很多範例會帶 TimeStamp（秒），帶了不會錯
        p.put("TimeStamp", String.valueOf(Instant.now().getEpochSecond()));

        String mac = generateCheckMacForLogistics(p, key, iv);
        p.put("CheckMacValue", mac);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        p.forEach(form::add);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> resp;
        try {
            resp = rest.postForEntity(action, new HttpEntity<>(form, headers), String.class);
        } catch (Exception ex) {
            out.error = "HTTP call failed: " + ex.getMessage();
            log.error("[QueryTradeInfo] HTTP call failed: {}", ex.getMessage());
            return out;
        }

        String rawBody = nz(resp.getBody());
        out.raw = rawBody;
        log.info("[QueryTradeInfo] HTTP={} body={}", resp.getStatusCodeValue(), rawBody);

        if (!resp.getStatusCode().is2xxSuccessful() || rawBody.isBlank()) {
            out.error = "Bad HTTP or empty body";
            return out;
        }

        String kvPart = normalizeEcpayBody(rawBody);
        Map<String, String> kv = parseKv(kvPart);
        Map<String, String> m  = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        kv.forEach(m::put);

        String rtn = m.getOrDefault("RtnCode", "");
        String msg = m.getOrDefault("RtnMsg", "");
        if (!"1".equals(rtn) && !"300".equals(rtn)) {
            out.error = "Query failed: " + (msg.isBlank() ? rawBody : msg);
            return out;
        }

        out.ok             = true;
        out.logisticsId    = m.getOrDefault("AllPayLogisticsID", logisticsId);
        out.trackingNo     = nz(m.get("ShipmentNo"));       // 重點
        out.logisticsStatus= nz(m.get("LogisticsStatus"));  // 若有就回

        // 有新追蹤碼或狀態才寫 DB（避免把 null/空覆蓋）
        boolean changed = false;
        if (notBlank(out.trackingNo) && !out.trackingNo.equals(nz(o.getTrackingNo()))) {
            o.setTrackingNo(out.trackingNo);
            changed = true;
        }
        if (notBlank(out.logisticsStatus) && !out.logisticsStatus.equals(nz(o.getLogisticsStatus()))) {
            o.setLogisticsStatus(out.logisticsStatus);
            changed = true;
        }
        if (changed) {
            orderRepo.saveAndFlush(o);
            log.info("[QueryTradeInfo] updated orderId={} logisticsId={} trackingNo={} status={}",
                    o.getOrderId(), o.getLogisticsId(), o.getTrackingNo(), o.getLogisticsStatus());
        } else {
            log.info("[QueryTradeInfo] no change for orderId={} (tracking/status unchanged)", o.getOrderId());
        }

        return out;
    }

    // ====== CheckMac（Logistics：MD5） ======
    /** 物流用 CheckMac：UrlEncode → toLower → 7 個置換 → MD5 → UpperCase */
    private static String generateCheckMacForLogistics(Map<String, String> params, String hashKey, String hashIv) {
        Map<String, String> signMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : params.entrySet()) {
            String k = e.getKey(), v = e.getValue();
            if ("CheckMacValue".equalsIgnoreCase(k)) continue;
            if (v == null || v.isBlank()) continue;
            signMap.put(k, v);
        }
        StringBuilder kv = new StringBuilder();
        for (var it = signMap.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            kv.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext()) kv.append("&");
        }
        String raw = "HashKey=" + hashKey + "&" + kv + "&HashIV=" + hashIv;

        String encodedLower = URLEncoder.encode(raw, StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT)
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")");

        return md5Hex(encodedLower).toUpperCase(Locale.ROOT);
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(out.length * 2);
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========= Helpers =========
    private static String signBaseForLog(Map<String, String> params) {
        Map<String, String> signMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : params.entrySet()) {
            if ("CheckMacValue".equalsIgnoreCase(e.getKey())) continue;
            if (e.getValue() == null || e.getValue().isBlank()) continue;
            signMap.put(e.getKey(), e.getValue());
        }
        StringBuilder kv = new StringBuilder();
        for (var it = signMap.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            kv.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext()) kv.append("&");
        }
        return "HashKey=***&" + kv + "&HashIV=***";
    }

    private static boolean isValidReceiverName(String s) {
        if (s == null) return false;
        String clean = s.trim().replaceAll("\\s+", "");
        clean = clean.replaceAll("[^A-Za-z\\u4E00-\\u9FFF]", "");
        boolean hasCJK = clean.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
        if (hasCJK) return clean.length() >= 2 && clean.length() <= 5; // 中文 2~5
        return clean.length() >= 4 && clean.length() <= 10; // 英文 4~10
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static int n(Integer i) { return i == null ? 0 : i; }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return "";
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }

    private static HomeCreateResult fail(HomeCreateResult r, String msg) { r.ok = false; r.error = msg; return r; }

    private static String buildNo(String prefix, Integer orderId) {
        String base = prefix + LocalDateTime.now().format(NO) + (orderId == null ? "" : orderId);
        return base.length() > 20 ? base.substring(0, 20) : base;
    }

    private static Map<String, String> parseKv(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.isBlank()) return m;
        for (String part : s.split("&")) {
            int i = part.indexOf('=');
            if (i < 0) continue;
            String k = URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }

    /** 去掉綠界回傳開頭的 "1|" 或 "0|" 前綴 */
    private static String normalizeEcpayBody(String body) {
        if (body == null) return "";
        int bar = body.indexOf('|');
        if (bar >= 0 && (body.startsWith("1|") || body.startsWith("0|"))) {
            return body.substring(bar + 1);
        }
        return body;
    }
}