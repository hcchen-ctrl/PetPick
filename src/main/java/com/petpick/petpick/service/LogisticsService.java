package com.petpick.petpick.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.CvsMapReturnDTO;
import com.petpick.petpick.dto.CvsSelectRequest;
import com.petpick.petpick.dto.LogisticsCreateRequest;
import com.petpick.petpick.entity.Shipment;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final EcpayProperties prop;
    private final ShipmentRepository shipmentRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    /** 1) 回傳「地圖選店」的自動提交 HTML（POST 到綠界） */
    public String buildCvsMapForm(CvsSelectRequest req) {
        String action = (propHostStage())
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";

        // 產生你平台唯一碼（<=20）
        String merchantTradeNo = "L" + System.currentTimeMillis();

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", prop.getLogistics().getMerchantId());
        p.put("MerchantTradeNo", merchantTradeNo);
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", req.getSubType());
        p.put("IsCollection", req.getIsCollection()); // Y:取貨付款
        p.put("ServerReplyURL", prop.getLogistics().getCvsMapReturnUrl());  // 使用者瀏覽器會 POST 回來
        p.put("ExtraData", String.valueOf(req.getOrderId())); // 帶 orderId 回來

        // 綠界地圖 API 不需 CheckMacValue（注意：Create/PrintLabel 會需要）

        // 新增暫時記錄（地圖前）
        Shipment ship = Shipment.builder()
                .orderId(req.getOrderId())
                .type("CVS")
                .subType(req.getSubType())
                .isCollection(req.getIsCollection())
                .status("NEW")
                .merchantTradeNo(merchantTradeNo)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shipmentRepo.save(ship);

        return autoSubmitHtml(action, p);
    }

    /** 2) 處理地圖選店回傳（瀏覽器 POST 回來）→ 驗簽 → 存店資料 */
    public String handleMapReturn(CvsMapReturnDTO dto) {
        // 地圖 map 回傳通常不含 CheckMacValue；若你改為 Create/其他操作才要 verify。
        // 這裡先把回傳的門市資訊寫入 DB
        Integer orderId = safeInt(dto.getExtraData());

        Shipment ship = shipmentRepo.findTopByOrderIdOrderByIdDesc(orderId)
                .orElse(Shipment.builder()
                        .orderId(orderId).type("CVS").status("NEW")
                        .createdAt(LocalDateTime.now())
                        .build());

        ship.setStoreId(dto.getStoreID());
        ship.setStoreName(dto.getStoreName());
        ship.setStoreAddress(dto.getStoreAddress());
        ship.setStoreTel(dto.getStoreTel());
        ship.setStatus("MAP_SELECTED");
        ship.setUpdatedAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        // 回顯簡單頁面：把選店結果寫到 sessionStorage 再導回 checkout（前端即可顯示）
        String back = prop.getClientBackUrl();
        String payload = String.format(
                "{storeId:'%s',storeName:'%s',address:'%s',tel:'%s'}",
                esc(dto.getStoreID()), esc(dto.getStoreName()), esc(dto.getStoreAddress()), esc(dto.getStoreTel())
        );
        return """
        <!doctype html><html lang="zh-Hant"><head><meta charset="utf-8"><title>選店成功</title></head>
        <body>
          <p>門市選擇成功，正在返回…</p>
          <script>
            try {
              sessionStorage.setItem('cvs_selected_store', JSON.stringify(%s));
            } catch(e) {}
            location.replace('%s');
          </script>
        </body></html>
        """.formatted(payload, esc(back));
    }

    /** 3) 建立 C2C 託運（MAP_SELECTED 後）→ 回傳自動提交 HTML（POST 到綠界建立物流單） */
    public String buildC2cCreateForm(LogisticsCreateRequest req) {
        Shipment ship = shipmentRepo.findTopByOrderIdOrderByIdDesc(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("尚未選店"));

        String action = (propHostStage())
                ? "https://logistics-stage.ecpay.com.tw/Express/Create"
                : "https://logistics.ecpay.com.tw/Express/Create";

        String tradeNo = ship.getMerchantTradeNo(); // 之前 map 已存
        String now = LocalDateTime.now().format(FMT);

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", prop.getLogistics().getMerchantId());
        p.put("MerchantTradeNo", tradeNo);
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", ship.getSubType());
        p.put("IsCollection", ship.getIsCollection()); // Y/N
        p.put("GoodsAmount", "1");       // 必填整數（可改為訂單金額，但 ECPay 有各產品規範）
        p.put("CollectionAmount", ship.getIsCollection().equals("Y") ? "1" : "0");
        p.put("SendersName", "Petpick"); // 寄件人資料（可改成你的商店）
        p.put("SendersPhone", "0223456789");
        p.put("ReceiverName", req.getReceiverName());
        p.put("ReceiverPhone", req.getReceiverPhone());
        p.put("ReceiverStoreID", ship.getStoreId());  // 選店回傳
        p.put("ClientReplyURL", prop.getClientBackUrl()); // 建立成功後顯示的返回連結

        // 需要 CheckMac
        String mac = EcpayCheckMac.generate(p, prop.getLogistics().getHashKey(), prop.getLogistics().getHashIv());
        p.put("CheckMacValue", mac);

        ship.setStatus("CREATING");
        ship.setUpdatedAt(LocalDateTime.now());
        shipmentRepo.save(ship);

        return autoSubmitHtml(action, p);
    }

    // ==== helpers ====

    private boolean propHostStage() {
        // 依 stage 控制
        return true; // 你也可以注入一個 ecpay.stage 再決定
    }

    private String autoSubmitHtml(String action, Map<String,String> params) {
        StringBuilder inputs = new StringBuilder();
        for (var e : params.entrySet()) {
            inputs.append("<input type='hidden' name='")
                  .append(escape(e.getKey())).append("' value='")
                  .append(escape(e.getValue())).append("'/>");
        }
        return """
        <!doctype html>
        <html lang="zh-Hant"><head><meta charset="utf-8"><title>Redirecting…</title></head>
        <body>
          <p>資料送出中，請稍候…</p>
          <form id="f" method="post" action="%s">%s</form>
          <script>document.getElementById('f').submit();</script>
        </body></html>
        """.formatted(escape(action), inputs.toString());
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    private String esc(String s){ return (s==null)?"":s.replace("'", "\\'"); }
    private String escape(String s){
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#x27;");
    }
}
