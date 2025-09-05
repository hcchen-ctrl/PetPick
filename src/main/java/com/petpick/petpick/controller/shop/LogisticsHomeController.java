// File: src/main/java/com/petpick/petpick/controller/LogisticsHomeController.java
package com.petpick.petpick.controller.shop;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping; // === NEW ===
import org.springframework.web.bind.annotation.RequestParam; // === NEW ===
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.DTO.shop.HomeCreateRequest;
import com.petpick.petpick.MAC.EcpayLogisticsCheckMac;
import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.entity.shop.Order;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.LogisticsEcpayService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logistics/home")
public class LogisticsHomeController {

  private final EcpayProperties prop;
  private final LogisticsEcpayService ecpaySvc;
  private final OrderRepository orderRepository;

  // ---- 建單（/ecpay/create 與 /create 兩種路徑）
  @PostMapping(path = { "/ecpay/create",
      "/create" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<HomeCreateResp> createHome(
      @RequestBody HomeCreateRequest req,
      @RequestHeader(value = "X-Demo-UserId", required = false) String demoUid) {

    log.info("[HomeCreate][in] orderId={} demoUid={}", req.getOrderId(), demoUid);

    var r = ecpaySvc.createHomeShipment(req);

    log.info("[HomeCreate][out] ok={} err={} logisticsId={} trackingNo={}",
        r.ok, r.error, r.logisticsId, r.trackingNo);

    HomeCreateResp out = new HomeCreateResp();
    out.ok = r.ok;
    out.error = r.error;
    out.logisticsId = r.logisticsId; // 若沒有會是 null
    out.trackingNo = r.trackingNo; // 若沒有會是 null

    return ResponseEntity.status(r.ok ? 200 : 400).body(out);
  }

  // ---- 綠界宅配 Server-to-Server 回拋（務必回 "1|OK"）
  @PostMapping(path = { "/ecpay/reply",
      "/reply" }, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.ALL_VALUE)
  public ResponseEntity<String> homeReply(@RequestParam MultiValueMap<String, String> form) {
    // 攤平並轉大小寫不敏感 Map
    Map<String, String> raw = toFlat(form);
    Map<String, String> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    raw.forEach(m::put);

    final boolean isStage = prop.isStage();
    log.info("[HomeReply] env={} recv={}", isStage ? "STAGE" : "PROD", m);

    // 取必要欄位（大小寫不敏感）
    String logisticsId = nv(firstNonBlank(m.get("LogisticsID"), m.get("AllPayLogisticsID")));
    String trackingNoEc = nv(m.get("ShipmentNo")); // 宅配追蹤碼（可能空）
    String status = nv(m.get("LogisticsStatus")); // IN_TRANSIT / DELIVERED / ...
    String rtnCode = nv(m.get("RtnCode")); // 1 / 300 / ...
    String mtn = nv(m.get("MerchantTradeNo"));
    String extraData = nv(m.get("ExtraData")); // 建單時放的 orderId

    // 1) 驗證 CheckMacValue（正式必須通過；測試環境放寬）
    boolean macOk;
    try {
      var logi = prop.getLogistics();
      macOk = EcpayLogisticsCheckMac.verify(m, nv(logi.getHashKey()), nv(logi.getHashIv()));
    } catch (Exception ignore) {
      macOk = false;
    }

    if (!macOk) {
      if (isStage) {
        log.warn("[HomeReply] MAC NG (STAGE relaxed). data={}", m);
      } else {
        log.warn("[HomeReply] MAC NG (PROD blocked). data={}", m);
        // 仍回 1|OK 避免綠界無限重送，但不更新資料
        return ResponseEntity.ok("1|OK");
      }
    }

    // 2) 找訂單：優先 ExtraData（orderId），找不到再用 LogisticsID
    Optional<Order> opt = Optional.empty();
    Integer orderIdFromExtra = parseInt(extraData);
    if (orderIdFromExtra != null)
      opt = orderRepository.findById(orderIdFromExtra);
    if (opt.isEmpty() && !logisticsId.isBlank())
      opt = orderRepository.findByLogisticsId(logisticsId);

    if (opt.isEmpty()) {
      log.warn("[HomeReply] 無法對應訂單 extraData(orderId)={} logisticsId={} mtn={}",
          extraData, logisticsId, mtn);
      return ResponseEntity.ok("1|OK");
    }

    Order o = opt.get();

    // 3) 僅在有值時才更新，避免把 NULL 蓋成 ""
    boolean changed = false;

    if (!logisticsId.isBlank() && !logisticsId.equals(nv(o.getLogisticsId()))) {
      o.setLogisticsId(logisticsId);
      changed = true;
    }
    if (!trackingNoEc.isBlank() && !trackingNoEc.equals(nv(o.getTrackingNo()))) {
      o.setTrackingNo(trackingNoEc); // ← 寫進 trackingNo 欄位
      changed = true;
    }
    if (!status.isBlank()) {
      o.setLogisticsStatus(status);
      changed = true;
      if ("DELIVERED".equalsIgnoreCase(status)) {
        try {
          o.setDeliveredAt(LocalDateTime.now());
        } catch (Exception ignore) {
        }
      }
    } else if ("1".equals(rtnCode) || "300".equals(rtnCode)) {
      // 沒帶 LogisticsStatus 但 RtnCode 表示已受理 → 若目前空白則補為 CREATED
      if (nv(o.getLogisticsStatus()).isBlank()) {
        o.setLogisticsStatus("CREATED");
        changed = true;
      }
    }

    if (changed) {
      orderRepository.saveAndFlush(o);
      log.info("[HomeReply] 更新完成 orderId={} logisticsId={} trackingNo={} status={}",
          o.getOrderId(), o.getLogisticsId(), o.getTrackingNo(), o.getLogisticsStatus());
    } else {
      log.info("[HomeReply] 無需更新 orderId={}（資料相同或缺值）", o.getOrderId());
    }

    // 綠界要求固定回覆
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body("1|OK");
  }

  // === NEW === 查詢宅配單（補追蹤碼／狀態），後台可綁「刷新」按鈕呼叫
  @GetMapping(path = "/query/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LogisticsEcpayService.QueryResult> queryTradeInfo(@PathVariable Integer orderId) {
    var r = ecpaySvc.queryHomeTradeInfoByOrder(orderId);
    // r.ok = true 才代表有查到且（若有變化）已回寫 DB
    return ResponseEntity.status(r.ok ? 200 : 400).body(r);
  }

  // 模擬已配達
  // === DEV/STAGE ONLY: 一鍵模擬「已配達」 ===
  @PutMapping(path = "/mock/delivered/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> mockDelivered(@PathVariable Integer orderId) {
    // 僅允許在 STAGE 使用，避免正式環境誤觸
    if (!prop.isStage()) {
      Map<String, Object> resp = new LinkedHashMap<>();
      resp.put("ok", false);
      resp.put("error", "FORBIDDEN_IN_PROD");
      return ResponseEntity.status(403).body(resp);
    }

    var opt = orderRepository.findById(orderId);
    if (opt.isEmpty()) {
      Map<String, Object> resp = new LinkedHashMap<>();
      resp.put("ok", false);
      resp.put("error", "ORDER_NOT_FOUND");
      return ResponseEntity.status(404).body(resp);
    }

    Order o = opt.get();

    // 若尚未出貨，順手補一個出貨時間與中途狀態，讓前端進度條完整
    if (o.getShippedAt() == null) {
      try {
        o.setShippedAt(LocalDateTime.now().minusDays(1));
      } catch (Exception ignore) {
      }
      if (o.getLogisticsStatus() == null || o.getLogisticsStatus().isBlank()) {
        o.setLogisticsStatus("IN_TRANSIT");
      }
    }

    // 直設為已配達並補 deliveredAt
    o.setLogisticsStatus("DELIVERED");
    try {
      o.setDeliveredAt(LocalDateTime.now());
    } catch (Exception ignore) {
    }

    // 沒追蹤碼就補一個假的，方便前端顯示
    if (o.getTrackingNo() == null || o.getTrackingNo().isBlank()) {
      o.setTrackingNo("MOCK-" + System.currentTimeMillis());
    }

    orderRepository.saveAndFlush(o);

    Map<String, Object> resp = new LinkedHashMap<>();
    resp.put("ok", true);
    resp.put("orderId", o.getOrderId());
    resp.put("logisticsStatus", o.getLogisticsStatus());
    resp.put("deliveredAt", o.getDeliveredAt());
    resp.put("trackingNo", o.getTrackingNo());
    return ResponseEntity.ok(resp);
  }
  // === NEW END ===

  // ===== helpers =====
  private static String nv(String s) {
    return s == null ? "" : s;
  }

  private static Map<String, String> toFlat(MultiValueMap<String, String> form) {
    Map<String, String> out = new LinkedHashMap<>();
    form.forEach((k, v) -> out.put(k, (v == null || v.isEmpty()) ? "" : v.get(0)));
    return out;
  }

  private static Integer parseInt(String s) {
    try {
      if (s == null || s.isBlank())
        return null;
      return Integer.valueOf(s.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private static String firstNonBlank(String... vals) {
    if (vals == null)
      return "";
    for (String v : vals)
      if (v != null && !v.isBlank())
        return v;
    return "";
  }

  @Data
  public static class HomeCreateResp {
    public boolean ok;
    public String error;
    public String logisticsId;
    public String trackingNo;
  }
}