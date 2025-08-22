// LogisticsHomeController.java
package com.petpick.petpick.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.OrderService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/logistics/home")
@RequiredArgsConstructor
public class LogisticsHomeController {

    private final EcpayProperties prop;
    private final OrderRepository orderRepo;
    private final OrderService orderService;

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 建立黑貓宅配託運單（測試站）
    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> createHome(@RequestBody CreateHomeReq req) {
        if (req == null || req.getOrderId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderId is required"));
        }
        Order o = orderRepo.findById(req.getOrderId()).orElse(null);
        if (o == null) return ResponseEntity.badRequest().body(Map.of("error","order not found"));

        // 基本檢查：宅配需要收件人與地址
        if (isBlank(o.getReceiverName()) || isBlank(o.getReceiverPhone()) || isBlank(o.getAddr())) {
            return ResponseEntity.badRequest().body(Map.of("error","receiverName/receiverPhone/addr required for home delivery"));
        }

        var lg = prop.getLogistics();
        String mid = nz(lg.getMerchantId());
        String key = nz(lg.getHashKey());
        String iv  = nz(lg.getHashIv());
        String action = nz(lg.getHomeCreateUrl());
        String serverReplyUrl = nz(lg.getHomeServerReplyUrl());

        // 唯一單號（≤20）
        String merchantTradeNo = "H" + LocalDateTime.now().format(NO);

        // 建立宅配參數（HOME + TCAT）
        // 註：下面欄位名稱以綠界物流常見命名為例；若你的文件不同，請依文件微調
        Map<String,String> p = new LinkedHashMap<>();
        p.put("MerchantID", mid);
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "Home");         // 宅配
        p.put("LogisticsSubType", "TCAT");      // 黑貓
        p.put("GoodsAmount", String.valueOf(o.getTotalPrice()));    // 託運金額（整數）
        p.put("IsCollection", req.isCollection ? "Y" : "N");        // 是否代收
        p.put("CollectionAmount", req.isCollection ? String.valueOf(o.getTotalPrice()) : "0");
        p.put("ServerReplyURL", serverReplyUrl); // S2S 回拋（必須可對外連線）
        p.put("SenderName", "PetPick");          // 寄件者資訊（可放你們公司）
        p.put("SenderPhone", "");
        p.put("SenderCellPhone", "0911222333");  // 可填測試
        p.put("SenderZipCode", "100");
        p.put("SenderAddress", "台北市中正區測試路 1 號");
        p.put("ReceiverName", nz(o.getReceiverName()));
        p.put("ReceiverPhone", "");              // 固話沒有可空
        p.put("ReceiverCellPhone", nz(o.getReceiverPhone()));
        p.put("ReceiverZipCode", "100");         // 測試可固定
        p.put("ReceiverAddress", nz(o.getAddr()));
        p.put("TradeDesc", "PetPick Home");
        p.put("Remark", "測試訂單");
        p.put("ExtraData", String.valueOf(o.getOrderId())); // 讓回拋知道是哪張訂單

        // 產簽
        String mac = EcpayCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        // 送出（伺服器對伺服器）
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        p.forEach(form::add);

        // 你可用 WebClient / RestTemplate，這裡用簡化的 Java 11 HttpClient 伪碼：
        // 為了讓你能直接編譯，改用 Spring 的 RestTemplate：
        var rt = new org.springframework.web.client.RestTemplate();
        String resp = rt.postForObject(action, form, String.class);

        log.warn("[Home-Create-RESP]\n{}", resp);

        // 解析回應（綠界通常回 key=value&key=value 格式）
        Map<String,String> r = parseKv(resp);
        String rtnCode = r.getOrDefault("RtnCode","-1");
        String rtnMsg  = r.getOrDefault("RtnMsg","");

        if (!"1".equals(rtnCode)) {
            return ResponseEntity.status(502).body(Map.of("error","create failed","code",rtnCode,"msg",rtnMsg));
        }

        // 常見回傳欄位（實際以文件為準）
        String allPayLogisticsId = r.get("AllPayLogisticsID"); // 託運單號
        String shipmentNo        = r.get("ShipmentNo");        // 黑貓託運編號（若有）
        // 寫回訂單
        orderService.setLogisticsInfo(o.getOrderId(), allPayLogisticsId, shipmentNo);
        // 確保是宅配類型
        if (!"address".equalsIgnoreCase(o.getShippingType())) {
            o.setShippingType("address");
            orderRepo.save(o);
        }

        return ResponseEntity.ok(Map.of(
            "ok", true,
            "logisticsId", nz(allPayLogisticsId),
            "trackingNo", nz(shipmentNo),
            "message", rtnMsg
        ));
    }

    // 宅配建立的 S2S 回拋（ECPay 會打到這裡）
    @PostMapping(path = "/reply", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<String> homeReply(@RequestParam MultiValueMap<String,String> form) {
        Map<String,String> m = new LinkedHashMap<>();
        form.forEach((k,v)-> m.put(k, (v==null||v.isEmpty())? "" : v.get(0)));

        var lg = prop.getLogistics();
        boolean macOk = true;
        try { macOk = EcpayCheckMac.verify(m, lg.getHashKey(), lg.getHashIv()); } catch (Exception ignore){ macOk = false; }

        log.warn("[Home-Reply] macOk={}, data={}", macOk, m);

        // 取得我們在建立時塞的 ExtraData
        Integer orderId = null;
        try { orderId = Integer.valueOf(m.getOrDefault("ExtraData","").replaceAll("\\D+","")); } catch (Exception ignore) {}

        // 有時回拋會帶最終的託運/追蹤號
        String logisticsId = nz(m.get("AllPayLogisticsID"));
        String trackingNo  = nz(m.get("ShipmentNo"));

        if (orderId != null && (!isBlank(logisticsId) || !isBlank(trackingNo))) {
            orderService.setLogisticsInfo(orderId, logisticsId, trackingNo);
        }

        // 綠界物流 S2S 回覆需要 "1|OK"
        return ResponseEntity.ok("1|OK");
    }

    // ===== helpers =====
    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
    private static String nz(String s){ return s==null? "" : s; }

    private static Map<String,String> parseKv(String s){
        Map<String,String> out = new LinkedHashMap<>();
        if (s==null) return out;
        for (String part : s.split("&")) {
            int i = part.indexOf('=');
            if (i<=0) continue;
            out.put(part.substring(0,i), part.substring(i+1));
        }
        return out;
    }

    @Data
    public static class CreateHomeReq {
        private Integer orderId;
        private boolean isCollection; // 是否貨到收款（COD）
    }
}