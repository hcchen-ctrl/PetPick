package com.petpick.petpick.controller;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.CvsSelectRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.mac.EcpayPaymentCheckMac;
import com.petpick.petpick.repository.OrderRepository;
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
    private final OrderService orderService;     // 寫回由這支 service 處理
    private final OrderRepository orderRepo;     // 查單純為了讀 receiver/store

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // =========================================================================
    // 1) 開啟超商選店地圖
    // =========================================================================
    @PostMapping(path = "/map", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> openMap(@RequestBody CvsSelectRequest req) {
        if (req == null || req.getOrderId() == null) {
            return ResponseEntity.badRequest().body("orderId is required");
        }

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
        String iv  = nz(logi == null ? null : logi.getHashIv());
        String serverReplyUrl = nz(logi == null ? null : logi.getCvsMapReturnUrl());

        if (blank(mid) || blank(key) || blank(iv) || blank(serverReplyUrl)) {
            log.error("[Logi-ERROR] Missing logistics config. MID={}, Key(4)={}, IV(4)={}, ServerReplyURL={}",
                    mid, safe4(key), safe4(iv), serverReplyUrl);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Logistics config missing: please check MerchantID/HashKey/HashIV/ServerReplyURL");
        }

        String merchantTradeNo = "L" + LocalDateTime.now().format(NO);

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", subType);
        p.put("IsCollection", isCollection);
        p.put("ServerReplyURL", serverReplyUrl);
        p.put("Device", "0"); // 0=PC
        p.put("ExtraData", String.valueOf(req.getOrderId())); // 讓回拋知道訂單

        String mac = EcpayPaymentCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        log.info("[Logi-MAP] stage={}, action={}, MID={}, subType={}, isCollection={}, reply={}",
                prop.isStage(), action, mid, subType, isCollection, serverReplyUrl);

        String html = buildFormHtml(action, p);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // =========================================================================
    // 2) 超商選店回拋（browser → server）
    // =========================================================================
    @PostMapping(path = "/store-return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> storeReturn(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> m = toFlatMap(form);

        String orderIdStr = nz(m.get("ExtraData"));
        String brandCode  = nz(m.getOrDefault("LogisticsSubType",
                                m.getOrDefault("SubType", m.getOrDefault("CVSSubType", ""))));
        String storeId    = nz(m.getOrDefault("CVSStoreID", m.getOrDefault("StoreID", "")));
        String storeNm    = nz(m.getOrDefault("CVSStoreName", m.getOrDefault("StoreName", "")));
        String addr       = nz(m.getOrDefault("CVSAddress", m.getOrDefault("StoreAddress", "")));

        try {
            Integer oid = Integer.valueOf(orderIdStr);
            if (!storeId.isBlank()) {
                // 寫回訂單與 shipment
                orderService.setStoreInfo(oid, brandCode, storeId, storeNm, addr);
            }
        } catch (Exception e) {
            log.warn("store-return save failed, oid={}", orderIdStr, e);
        }

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

    // =========================================================================
    // 3) 建立「超商取貨(付款)」託運單（B2C 全家 FAMI）
    //    前端：POST /api/logistics/cvs/ecpay/create-b2c
    //    body: { "orderId": 123 }   // subType、isCollection 可選；預設 FAMI / Y
    // =========================================================================
    @PostMapping(
      path = "/ecpay/create-b2c",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createCvsB2C(@RequestBody CvsSelectRequest req) {
        try {
            if (req == null || req.getOrderId() == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "orderId is required"));
            }

            Order order = orderRepo.findById(req.getOrderId()).orElse(null);
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "order not found"));
            }

            // 必備：門市代號 + 收件人
            String storeId       = nz(order.getStoreId());
            String receiverName  = nz(order.getReceiverName());
            String receiverPhone = nz(order.getReceiverPhone());

            if (storeId.isBlank()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "storeId is empty (請先選店)"));
            }

            var logi = prop.getLogistics();
            String mid = nz(logi == null ? null : logi.getMerchantId());
            String key = nz(logi == null ? null : logi.getHashKey());
            String iv  = nz(logi == null ? null : logi.getHashIv());
            if (blank(mid) || blank(key) || blank(iv)) {
                return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", "Logistics config missing"));
            }

            String action = prop.isStage()
                    ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                    : "https://logistics.ecpay.com.tw/Express/Create";

            String merchantTradeNo   = "L" + LocalDateTime.now().format(NO);
            String merchantTradeDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

            // 預設：全家 FAMI、取貨付款 Y
            String subType = blank(req.getSubType()) ? "FAMI" : req.getSubType(); // FAMI/UNIMART/HILIFE/OKMART
            boolean collect = !"N".equalsIgnoreCase(nz(req.getIsCollection()));
            int amount = Math.max(1, order.getTotalPrice());

            // ===== Sender（寄件人）— 為避免 10500035，做清理與長度校正 =====
            String senderName = sanitizeSenderName(nz(logi == null ? null : logi.getSenderName()));
            if (senderName.isBlank()) senderName = "PetPickTW"; // fallback：英文 8 碼
            String senderCell = nz(logi == null ? null : logi.getSenderCellPhone());
            String senderPhone = nz(logi == null ? null : logi.getSenderPhone());
            String senderTel = senderCell.isBlank() ? (senderPhone.isBlank() ? "0912345678" : senderPhone) : senderCell;

            // ===== Receiver（收件人 / 取貨人）=====
            String rcvName = receiverName.isBlank() ? "客戶" : receiverName;
            String rcvCell = receiverPhone.isBlank() ? "0912345678" : receiverPhone;

            Map<String, String> p = new LinkedHashMap<>();
            p.put("MerchantID", mid);
            p.put("MerchantTradeNo", merchantTradeNo);
            p.put("MerchantTradeDate", merchantTradeDate);
            p.put("LogisticsType", "CVS");
            p.put("LogisticsSubType", subType);
            p.put("GoodsAmount", String.valueOf(amount));
            p.put("IsCollection", collect ? "Y" : "N");
            if (collect) p.put("CollectionAmount", String.valueOf(amount));
            p.put("GoodsName", "PetPick商品");                 // 保險帶一筆貨名
            // Receiver（B2C 需要）
            p.put("ReceiverName", rcvName);
            p.put("ReceiverCellPhone", rcvCell);
            p.put("ReceiverStoreID", storeId);
            // Sender（避免 10500035）
            p.put("SenderName", senderName);
            p.put("SenderCellPhone", senderTel);

            // S2S 回拋
            String rtnUrl = nz(logi == null ? null : logi.getCvsCreateReturnUrl());
            if (blank(rtnUrl)) rtnUrl = nz(logi == null ? null : logi.getCvsMapReturnUrl()); // 備用
            if (!blank(rtnUrl)) {
                p.put("ServerReplyURL", rtnUrl);
                p.put("RtnURL", rtnUrl);
            }

            // 讓回拋知道訂單
            p.put("ExtraData", String.valueOf(order.getOrderId()));

            // 簽名
            String mac = EcpayPaymentCheckMac.generate(p, key, iv);
            p.put("CheckMacValue", mac);

            if (log.isDebugEnabled()) {
                Map<String, String> dbg = new LinkedHashMap<>(p);
                dbg.put("HashKey", "**");
                dbg.put("HashIV", "**");
                log.debug("[ECpay-Create-B2C] sending params: {}", dbg);
            }

            // 送出 x-www-form-urlencoded
            var form = new LinkedMultiValueMap<String, String>();
            p.forEach(form::add);

            var rest = new RestTemplate();
            String body = rest.postForObject(action, form, String.class);
            Map<String, String> rtn = parseKv(body);

            if (!"1".equals(rtn.get("RtnCode"))) {
                String msg = nz(rtn.get("RtnMsg"));
                log.warn("[ECpay-Create] failed code={}, msg={}, raw={}", rtn.get("RtnCode"), msg, body);
                return ResponseEntity.ok(Map.of("ok", false, "error", msg.isBlank() ? "ECpay Create failed" : msg));
            }

            String logisticsId     = nz(rtn.getOrDefault("AllPayLogisticsID", rtn.getOrDefault("LogisticsID","")));
            String trackingNo      = nz(rtn.getOrDefault("ShipmentNo", ""));
            String cvsPaymentNo    = nz(rtn.get("CVSPaymentNo"));
            String cvsValidationNo = nz(rtn.get("CVSValidationNo"));

            // 寫回 DB
            try {
                orderService.setLogisticsInfo(order.getOrderId(), logisticsId, trackingNo);
            } catch (Exception e) {
                log.warn("DB update after create failed, oid={}", order.getOrderId(), e);
            }

            log.info("[ECpay-Create OK] oid={}, APLID={}, ShipNo={}, PayNo={}, ValidNo={}",
                    order.getOrderId(), logisticsId, trackingNo, cvsPaymentNo, cvsValidationNo);

            return ResponseEntity.ok(Map.of(
                "ok", true,
                "logisticsId", logisticsId,
                "trackingNo", trackingNo,
                "cvsPaymentNo", cvsPaymentNo,
                "cvsValidationNo", cvsValidationNo
            ));
        } catch (Exception e) {
            log.error("createCvsB2C error", e);
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // =========================================================================
    // 4) 建單 S2S 回拋（綠界 → 你的伺服器）
    // =========================================================================
    @PostMapping(path = "/ecpay/create-return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> createReturn(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> m = toFlatMap(form);

        String rtnCode        = nz(m.get("RtnCode"));
        String rtnMsg         = nz(m.get("RtnMsg"));
        String logisticsId    = nz(m.getOrDefault("AllPayLogisticsID", m.getOrDefault("LogisticsID","")));
        String shipmentNo     = nz(m.get("ShipmentNo"));           // trackingNo
        String cvsPaymentNo   = nz(m.get("CVSPaymentNo"));
        String cvsValidNo     = nz(m.get("CVSValidationNo"));
        String extraOrderId   = nz(m.get("ExtraData"));

        log.info("[ECpay-Create-Return] code={}, msg={}, APLID={}, shipNo={}, payNo={}, validNo={}, extra={}",
                rtnCode, rtnMsg, logisticsId, shipmentNo, cvsPaymentNo, cvsValidNo, extraOrderId);

        try {
            Integer oid = Integer.valueOf(extraOrderId);
            orderService.setLogisticsInfo(oid, logisticsId, shipmentNo);
        } catch (Exception e) {
            log.warn("create-return DB update failed, extra={}", extraOrderId, e);
        }

        return ResponseEntity.ok("1|OK");
    }

    // ===== Helpers =====
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s; }

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
          <script>document.getElementById('cvsForm').submit();</script>
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

    /** 解析 key=value&key=value 形式（綠界常見格式） */
    private static Map<String, String> parseKv(String body) {
        Map<String, String> m = new LinkedHashMap<>();
        if (body == null) return m;
        for (String part : body.split("&")) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            String k = part.substring(0, i);
            String v = part.substring(i + 1);
            m.put(k, URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    /** 針對綠界規則正規化寄件人姓名：中文 2–5，英文 4–10（超出裁切，不足則改用 fallback） */
    private static String sanitizeSenderName(String raw) {
        String s = nz(raw).trim();
        // 僅保留中英數與空白；再移除空白（B2C 建議無空白）
        s = s.replaceAll("[^\\p{IsHan}A-Za-z0-9 ]", "")
             .replaceAll("\\s+", "");
        if (s.isBlank()) return "";

        boolean allHan = s.codePoints().allMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);

        if (allHan) {
            if (s.length() < 2) return "";
            if (s.length() > 5) return s.substring(0, 5);
            return s;
        } else {
            if (s.length() < 4) return "";
            if (s.length() > 10) return s.substring(0, 10);
            return s;
        }
    }
}