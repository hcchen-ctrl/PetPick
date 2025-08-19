package com.petpick.petpick.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.entity.Order;
import com.petpick.petpick.entity.Shipment;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final EcpayProperties prop;
    private final OrderRepository orderRepo;
    private final ShipmentRepository shipmentRepo;

    /**
     * 回傳一個自動提交的HTML表單，導去綠界【超商選店】頁面
     */
    public String buildCvsMapForm(Integer orderId, String subType, String isCollection) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 綠界選店(測試/正式)端點
        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";

        // 必填參數（以物流商店代號與金鑰）
        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", prop.getLogistics().getMerchantId());
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", subType);                 // UNIMARTC2C/FAMIC2C/HILIFEC2C/OKMARTC2C
        p.put("IsCollection", isCollection);                // Y/N
        p.put("ServerReplyURL", prop.getLogistics().getCvsMapReturnUrl()); // 經常是導回頁(也可另設 Client 端 URL)
        p.put("ExtraData", String.valueOf(orderId));        // 放 orderId，以便回來辨識
        p.put("Device", "0"); // 0:PC 1:Mobile

        // 檢查碼（⚠️ 用物流金鑰）
        String mac = EcpayCheckMac.generate(p,
                prop.getLogistics().getHashKey(),
                prop.getLogistics().getHashIv());
        p.put("CheckMacValue", mac);

        return buildAutoSubmitHtml(action, p);
    }

    /**
     * 儲存選店回拋（簡化版）
     */
    public Shipment saveStoreSelection(Map<String, String> bodyKV) {
        Integer orderId = Integer.valueOf(bodyKV.getOrDefault("ExtraData", "0"));
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 可驗證 CheckMacValue（選店頁常見不回，若有就驗）
        // boolean ok = EcpayCheckMac.verify(bodyKV, prop.getLogistics().getHashKey(), prop.getLogistics().getHashIv());
        Shipment s = shipmentRepo.findFirstByOrderIdOrderByIdDesc(orderId).orElseGet(Shipment::new);
        s.setOrderId(orderId);
        s.setType("CVS_C2C");
        s.setStatus("STORE_SELECT");
        s.setLogisticsSubType(bodyKV.get("LogisticsSubType"));
        s.setIsCollection(bodyKV.get("IsCollection"));
        s.setCvsStoreId(bodyKV.get("CVSStoreID"));
        s.setCvsStoreName(bodyKV.get("CVSStoreName"));
        s.setCvsAddress(bodyKV.get("CVSAddress"));
        s.setCvsTelephone(bodyKV.get("CVSTelephone"));
        s.setRaw(bodyKV.toString());
        return shipmentRepo.save(s);
    }

    private String buildAutoSubmitHtml(String action, Map<String, String> params) {
        StringBuilder inputs = new StringBuilder();
        for (var e : params.entrySet()) {
            inputs.append("<input type=\"hidden\" name=\"")
                    .append(escapeHtml(e.getKey())).append("\" value=\"")
                    .append(escapeHtml(e.getValue())).append("\"/>\n");
        }
        return """
        <!doctype html>
        <html lang="zh-Hant"><head><meta charset="utf-8"><title>Redirecting…</title></head>
        <body onload="document.forms[0].submit()" style="font-family:sans-serif">
          <p>正在開啟超商選店頁，請稍候…</p>
          <form method="post" action="%s">%s</form>
        </body></html>
        """.formatted(escapeHtml(action), inputs.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
