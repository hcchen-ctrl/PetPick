// src/main/java/com/petpick/petpick/controller/LogisticsHomeController.java
package com.petpick.petpick.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.HomeCreateRequest;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.LogisticsEcpayService;
import com.petpick.petpick.service.OrderService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/logistics/home") // ★ 統一前綴
public class LogisticsHomeController {

  private final EcpayProperties prop;
  private final LogisticsEcpayService ecpaySvc;
  private final OrderRepository orderRepository;
  private final OrderService orderService;

  // ---- 建單（支援 /ecpay/create 與 /create 兩種路徑，避免前端舊程式打不到）
  @PostMapping(path = { "/ecpay/create",
      "/create" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<HomeCreateResp> createHome(@RequestBody HomeCreateRequest req,
      @RequestHeader(value = "X-Demo-UserId", required = false) String demoUid) {
    log.info("[HomeCreate][in] orderId={} demoUid={}", req.getOrderId(), demoUid);
    var r = ecpaySvc.createHomeShipment(req);
    log.info("[HomeCreate][out] ok={} err={} logisticsId={} trackingNo={}",
        r.ok, r.error, r.logisticsId, r.trackingNo);
    HomeCreateResp out = new HomeCreateResp();
    out.ok = r.ok;
    out.error = r.error;
    out.logisticsId = r.logisticsId;
    out.trackingNo = r.trackingNo;
    return ResponseEntity.status(r.ok ? 200 : 400).body(out);
  }

  // ---- 綠界宅配 Server-to-Server 回拋（務必回 "1|OK"）
  @PostMapping(path = { "/ecpay/reply",
      "/reply" }, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> homeReply(@RequestParam MultiValueMap<String, String> form) {
    Map<String, String> m = toFlat(form);

    var logi = prop.getLogistics();
    String key = nvl(logi.getHashKey());
    String iv = nvl(logi.getHashIv());
    String logisticsId = nvl(m.getOrDefault("LogisticsID", m.get("AllPayLogisticsID")));
    String shipmentNo = nvl(m.get("ShipmentNo")); // 宅配才會有的追蹤碼
    String status = nvl(m.get("LogisticsStatus")); // IN_TRANSIT / DELIVERED / ...
    String mtn = nvl(m.get("MerchantTradeNo")); // 你送出去的單號(若有帶)

    if (!logisticsId.isBlank()) {
      // 1) 先以 LogisticsID 找到訂單
      orderRepository.findByLogisticsId(logisticsId).ifPresent(o -> {
        // 2) 補寫追蹤碼（若有）
        if (!shipmentNo.isBlank()) {
          orderService.setLogisticsInfo(o.getOrderId(), logisticsId, shipmentNo);
        }
        // 3) 更新狀態（可依你的欄位命名）
        if (!status.isBlank()) {
          o.setLogisticsStatus(status);
          if ("DELIVERED".equalsIgnoreCase(status)) {
            o.setDeliveredAt(LocalDateTime.now());
          }
          orderRepository.save(o);
        }
      });
    }
    boolean macOk = true;
    try {
      macOk = EcpayCheckMac.verify(m, key, iv);
    } catch (Exception ignore) {
      macOk = false;
    }
    log.info("[HomeReply] macOk={} data={}", macOk, m);

    // TODO: 依 m.get("LogisticsStatus") 更新訂單物流狀態/時間（IN_TRANSIT/DELIVERED…）
    return ResponseEntity.ok("1|OK");
  }

  // ===== helpers =====
  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  private static Map<String, String> toFlat(MultiValueMap<String, String> form) {
    Map<String, String> out = new LinkedHashMap<>();
    form.forEach((k, v) -> out.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));
    return out;
  }

  @Data
  public static class HomeCreateResp {
    public boolean ok;
    public String error;
    public String logisticsId;
    public String trackingNo;
  }
}