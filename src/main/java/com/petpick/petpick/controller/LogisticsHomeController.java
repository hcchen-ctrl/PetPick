// src/main/java/com/petpick/petpick/controller/LogisticsHomeController.java
package com.petpick.petpick.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.HomeCreateRequest;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.service.LogisticsEcpayService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/logistics/home")        // ★ 統一前綴
public class LogisticsHomeController {

  private final EcpayProperties prop;
  private final LogisticsEcpayService ecpaySvc;

  // ---- 建單（支援 /ecpay/create 與 /create 兩種路徑，避免前端舊程式打不到）
  @PostMapping(
      path = {"/ecpay/create", "/create"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<HomeCreateResp> createHome(@RequestBody HomeCreateRequest req) {
    var r = ecpaySvc.createHomeShipment(req);
    HomeCreateResp out = new HomeCreateResp();
    out.ok = r.ok;
    out.error = r.error;
    out.logisticsId = r.logisticsId;
    out.trackingNo = r.trackingNo;
    return ResponseEntity.status(r.ok ? 200 : 400).body(out);
  }

  // ---- 綠界宅配 Server-to-Server 回拋（務必回 "1|OK"）
  @PostMapping(
      path = {"/ecpay/reply", "/reply"},
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> homeReply(@RequestParam MultiValueMap<String, String> form) {
    Map<String, String> m = toFlat(form);

    var logi = prop.getLogistics();
    String key = nvl(logi.getHashKey());
    String iv  = nvl(logi.getHashIv());

    boolean macOk = true;
    try { macOk = EcpayCheckMac.verify(m, key, iv); } catch (Exception ignore) { macOk = false; }
    log.info("[HomeReply] macOk={} data={}", macOk, m);

    // TODO: 依 m.get("LogisticsStatus") 更新訂單物流狀態/時間（IN_TRANSIT/DELIVERED…）
    return ResponseEntity.ok("1|OK");
  }

  // ===== helpers =====
  private static String nvl(String s) { return s == null ? "" : s; }
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