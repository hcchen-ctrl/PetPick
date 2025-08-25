package com.petpick.petpick.service.impl;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.mac.EcpayPaymentCheckMac;
import com.petpick.petpick.service.EcpayHomeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcpayLogisticsServiceImpl implements EcpayHomeService {

    private final EcpayProperties prop;

    private static final List<String> SUCCESS_CODES = List.of("1", "300"); // 1=成功；(保險起見接受 300 類似成功碼)

    @Override
    public Map<String, String> createHomeShipment(Map<String, String> params) {
        var logi = prop.getLogistics();
        String mid = nz(logi == null ? null : logi.getMerchantId());
        String key = nz(logi == null ? null : logi.getHashKey());
        String iv  = nz(logi == null ? null : logi.getHashIv());

        // 端點（測試/正式）
        String url = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                : "https://logistics.ecpay.com.tw/Express/Create";

        // ---- 組參數（補齊必要欄位 + 產簽）----
        Map<String, String> p = new LinkedHashMap<>(params == null ? Map.of() : params);
        p.putIfAbsent("MerchantID", mid);
        p.putIfAbsent("LogisticsType", "HOME"); // 宅配
        // LogisticsSubType: TCAT(黑貓)/ECAN(新竹)/POST(郵局) — 由呼叫端決定；預設 TCAT
        p.putIfAbsent("LogisticsSubType", "TCAT");

        // 若未帶金額/收發件則拒絕
        require(p, "GoodsAmount"); // 整數金額
        require(p, "SenderName");
        require(p, "SenderPhone");
        require(p, "SenderZipCode");
        require(p, "SenderAddress");
        require(p, "ReceiverName");
        require(p, "ReceiverPhone");
        require(p, "ReceiverZipCode");
        require(p, "ReceiverAddress");

        // 常見預設
        p.putIfAbsent("IsCollection", "Y");      // 是否代收：N
        p.putIfAbsent("CollectionAmount", "0");
        p.putIfAbsent("Temperature", "0001");    // 0001:常溫
        p.putIfAbsent("Distance", "00");         // 00:同縣市/一般
        p.putIfAbsent("Specification", "0001");  // 0001:60cm/5kg(範例)
        p.putIfAbsent("ScheduledPickupTime", "1");   // 1:不限
        p.putIfAbsent("ScheduledDeliveryTime", "4"); // 4:不限

        // 簽章
        String mac = EcpayPaymentCheckMac.generate(p, key, iv);
        p.put("CheckMacValue", mac);

        // 送出 x-www-form-urlencoded
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        p.forEach(body::add);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> resp = rt.postForEntity(URI.create(url), new HttpEntity<>(body, headers), String.class);
        String raw = resp.getBody() == null ? "" : resp.getBody();

        log.info("[ECPay HOME Create] status={}, body={}", resp.getStatusCodeValue(), raw);

        Map<String, String> out = parseToMap(raw);

        String rtnCode = out.getOrDefault("RtnCode", "");
        if (!SUCCESS_CODES.contains(rtnCode)) {
            String msg = out.getOrDefault("RtnMsg", "create failed");
            throw new IllegalStateException("ECPay HOME Create fail: " + rtnCode + " " + msg);
        }
        return out;
    }

    // ---- helpers ----
    private static void require(Map<String, String> p, String key) {
        if (p.getOrDefault(key, "").isBlank()) {
            throw new IllegalArgumentException("missing param: " + key);
        }
    }
    private static String nz(String s) { return s == null ? "" : s; }

    /** 同時支援 JSON 或 key=value&... 的回應 */
    private static Map<String, String> parseToMap(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;

        try {
            var m = new ObjectMapper().readValue(raw, Map.class);
            m.forEach((k, v) -> out.put(String.valueOf(k), String.valueOf(v)));
            return out;
        } catch (Exception ignore) { }

        // 退回解析 querystring 形式
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) continue;
            String k = pair.substring(0, i);
            String v = pair.substring(i + 1);
            out.put(k, v);
        }
        return out;
    }
}