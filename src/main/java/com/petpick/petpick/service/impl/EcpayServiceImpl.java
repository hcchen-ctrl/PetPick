package com.petpick.petpick.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.EcpayService;
import com.petpick.petpick.service.OrderQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EcpayServiceImpl implements EcpayService {

    private final EcpayProperties prop;
    private final OrderQueryService orderQueryService;
    private final OrderRepository orderRepo; // 只為了檢查狀態（可選）

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public String buildAioCheckoutForm(Integer orderId) {
        return buildAioCheckoutForm(orderId, null);
    }

    @Override
    public String buildAioCheckoutForm(Integer orderId, String origin) {
        // 撈訂單與明細
        OrderDTO o = orderQueryService.getOne(orderId);

        // （可選）避免重複付款：若已 Paid 就丟錯或導回
        orderRepo.findById(orderId).ifPresent(ord -> {
            if ("Paid".equalsIgnoreCase(ord.getStatus())) {
                throw new IllegalStateException("此訂單已完成付款");
            }
        });

        // 綠界商品名稱規則：多品項用 # 串
        String itemName = (o.getItems() == null || o.getItems().isEmpty())
                ? "PetPick 訂單"
                : o.getItems().stream()
                    .map(it -> (it.getPname() == null || it.getPname().isBlank()) ? ("商品" + it.getProductId()) : it.getPname())
                    .collect(Collectors.joining("#"));

        String merchantId   = prop.getPayment().getMerchantId();
        String hashKey      = prop.getPayment().getHashKey();
        String hashIv       = prop.getPayment().getHashIv();

        String returnUrl        = prop.getReturnUrl();       // 伺服器回拋
        String orderResultUrl   = (origin == null || origin.isBlank())
                                    ? prop.getOrderResultUrl()
                                    : origin + "/payment/result";
        String clientBackUrl    = (origin == null || origin.isBlank())
                                    ? prop.getClientBackUrl()
                                    : origin + "/cart.html";

        // 必填參數
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", buildMerchantTradeNo(orderId)); // 必須平台唯一
        params.put("MerchantTradeDate", o.getCreatedAt().format(FMT));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(o.getTotalPrice()));  // 金額需為整數
        params.put("TradeDesc", urlEncodeComponent("PetPick 結帳"));
        params.put("ItemName", itemName);
        params.put("ReturnURL", returnUrl);
        params.put("OrderResultURL", orderResultUrl);
        params.put("ClientBackURL", clientBackUrl);
        params.put("ChoosePayment", "Credit");
        params.put("EncryptType", "1");

        // 自訂欄位：帶 orderId 回來更新狀態
        params.put("CustomField1", String.valueOf(orderId));

        // 產生檢查碼
        String checkMac = genCheckMacValue(params, hashKey, hashIv);
        params.put("CheckMacValue", checkMac);

        // 送到綠界的網址（測試/正式）
        String action = prop.isStage()
                ? "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"
                : "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5";

        // 回傳一個會自動送出的表單
        return buildAutoSubmitHtml(action, params);
    }

    private String buildMerchantTradeNo(Integer orderId) {
        // 確保 20 碼內，僅英文數字，且平台唯一。這邊加上時間避免重覆
        String base = "PP" + orderId + System.currentTimeMillis();
        return base.length() > 20 ? base.substring(0, 20) : base;
    }

    private String buildAutoSubmitHtml(String action, Map<String, String> params) {
        String inputs = params.entrySet().stream()
                .map(e -> "<input type=\"hidden\" name=\""+e.getKey()+"\" value=\""+escapeHtml(e.getValue())+"\"/>")
                .collect(Collectors.joining("\n"));
        return """
            <html><body onload="document.forms[0].submit()" style="font-family:sans-serif">
              <p>正在導向綠界安全頁面，請稍候…</p>
              <form method="post" action="%s">
                %s
              </form>
            </body></html>
        """.formatted(action, inputs);
    }

    // ECPay CheckMacValue 產生（SHA256, 大寫）
    private String genCheckMacValue(Map<String, String> params, String hashKey, String hashIV) {
        // 依照 key 名稱升冪排序（大小寫不敏感）
        SortedMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(params);

        StringBuilder sb = new StringBuilder();
        sb.append("HashKey=").append(hashKey);
        sorted.forEach((k,v) -> {
            if (v != null && !v.isBlank()) {
                sb.append('&').append(k).append('=').append(v);
            }
        });
        sb.append("&HashIV=").append(hashIV);

        // URL encode，空白要用 %20（不是 +）
        String encoded = urlEncode(sb.toString()).toLowerCase()
                // ECPay 規範的保留字還原
                .replace("%2d","-").replace("%5f","_")
                .replace("%2e",".").replace("%21","!")
                .replace("%2a","*").replace("%28","(")
                .replace("%29",")");

        return DigestUtils.sha256Hex(encoded).toUpperCase();
    }

    private String urlEncode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8)
                .replace("+", "%20"); // 空白用 %20
    }

    private String urlEncodeComponent(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;")
                .replace("'","&#x27;");
    }
}
