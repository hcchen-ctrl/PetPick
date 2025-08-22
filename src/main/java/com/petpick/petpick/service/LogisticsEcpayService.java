package com.petpick.petpick.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.HomeCreateRequest;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.mac.EcpayCheckMac;
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

    public static class HomeCreateResult {
        public boolean ok;
        public String error;
        public String logisticsId;   // AllPayLogisticsID
        public String trackingNo;    // ShipmentNo
    }

    /**
     * 綠界宅配建單（TCAT），回傳物流單號/託運單號並寫回 DB。
     */
    public HomeCreateResult createHomeShipment(HomeCreateRequest req) {
        HomeCreateResult r = new HomeCreateResult();

        if (req == null || req.getOrderId() == null) {
            r.ok = false; r.error = "orderId is required";
            return r;
        }

        Order o = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // ===== 1) 端點與金鑰 =====
        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                : "https://logistics.ecpay.com.tw/Express/Create";

        var logi = prop.getLogistics();
        String mid   = nvl(logi.getMerchantId());
        String key   = nvl(logi.getHashKey());
        String iv    = nvl(logi.getHashIv());
        String srvCb = nvl(logi.getHomeServerReplyUrl()); 

        // 寄件者（用你的公司資料）
        String senderName = nvl(logi.getSenderName(), "Petpick");
        String senderPhone = nvl(logi.getSenderPhone(), "0223456789");
        String senderZip = nvl(logi.getSenderZip(), "106");
        String senderAddr = nvl(logi.getSenderAddress(), "台北市大安區科技路 1 號");

        if (blank(mid) || blank(key) || blank(iv) || blank(srvCb)) {
            r.ok = false; r.error = "Logistics config missing: MerchantID/HashKey/HashIV/HomeReplyUrl";
            return r;
        }

        // ===== 2) 組參數 =====
        String merchantTradeNo = buildNo("H", o.getOrderId()); // 長度 <= 20

        boolean collect = Boolean.TRUE.equals(req.getIsCollection());
        int goodsAmt = Math.max(0, n(o.getTotalPrice()));
        int collectAmt = collect ? goodsAmt : 0;

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "Home");
        p.put("LogisticsSubType", "TCAT");           // 黑貓
        p.put("GoodsAmount", String.valueOf(goodsAmt));
        p.put("CollectionAmount", String.valueOf(collectAmt));
        p.put("IsCollection", collect ? "Y" : "N");
        p.put("ReceiverName",  nvl(req.getReceiverName(), nvl(o.getReceiverName())));
        p.put("ReceiverPhone", nvl(req.getReceiverPhone(), nvl(o.getReceiverPhone())));
        p.put("ReceiverZipCode", nvl(req.getReceiverZip(), nvl(o.getReceiverZip())));
        p.put("ReceiverAddress", nvl(req.getReceiverAddr(), nvl(o.getAddr())));
        p.put("ServerReplyURL", srvCb);
        // 額外資訊：讓回拋能找到訂單
        p.put("ExtraData", String.valueOf(o.getOrderId()));

        // 寄件人（必填）
        p.put("SenderName", senderName);
        p.put("SenderPhone", senderPhone);
        p.put("SenderZipCode", senderZip);
        p.put("SenderAddress", senderAddr);

        // 常溫/同縣市/60cm
        p.put("Temperature", "0001");
        p.put("Distance", "00");
        p.put("Specification", "0001");

        // 產簽
        String mac = EcpayCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        // ===== 3) 呼叫綠界（x-www-form-urlencoded） =====
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        p.forEach(form::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> resp = rest.postForEntity(action, new HttpEntity<>(form, headers), String.class);

        String body = nvl(resp.getBody());
        log.info("[HomeCreate] HTTP={} body={}", resp.getStatusCodeValue(), body);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            r.ok = false; r.error = "HTTP " + resp.getStatusCodeValue();
            return r;
        }

        // ===== 4) 解析回應（RtnCode=1 才成功）=====
        Map<String, String> m = parseKv(body);
        String rtn = m.getOrDefault("RtnCode", "");
        String msg = m.getOrDefault("RtnMsg", "");
        if (!"1".equals(rtn)) {
            r.ok = false; r.error = "ECPay Create failed: " + msg;
            return r;
        }

        r.ok = true;
        r.logisticsId = m.get("AllPayLogisticsID");
        r.trackingNo  = m.get("ShipmentNo"); // 黑貓託運單號

        // ===== 5) 寫回 DB =====
        try {
            // 你的 OrderService 若已有方法，建議呼叫 service 統一寫入
            o.setShippingType("address"); // 宅配
            o.setLogisticsId(r.logisticsId);
            o.setTrackingNo(r.trackingNo);
            // 可記錄狀態（不變更訂單主狀態）
            o.setLogisticsStatus("CREATED");
            orderRepo.save(o);
        } catch (Exception ex) {
            log.warn("[HomeCreate] save order failed: {}", ex.getMessage());
        }

        return r;
    }

    // ===== Helpers =====
    private static String nvl(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String nvl(String s, String alt) { return (s == null || s.isBlank()) ? alt : s; }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static int n(Integer i) { return i == null ? 0 : i; }

    private static String buildNo(String prefix, Integer orderId) {
        String base = prefix + LocalDateTime.now().format(NO) + (orderId == null ? "" : String.valueOf(orderId));
        return base.length() > 20 ? base.substring(0, 20) : base;
    }

    private static Map<String, String> parseKv(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : s.split("&")) {
            int i = part.indexOf('=');
            if (i < 0) continue;
            String k = URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }
}