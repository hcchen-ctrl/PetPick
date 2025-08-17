package com.petpick.petpick.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.OrderDTO;
import com.petpick.petpick.mac.EcpayCheckMac;
import com.petpick.petpick.repository.OrderRepository;
import com.petpick.petpick.service.EcpayService;
import com.petpick.petpick.service.OrderQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcpayServiceImpl implements EcpayService {

    private final EcpayProperties prop;
    private final OrderQueryService orderQueryService;
    private final OrderRepository orderRepo; // 用於避免重複付款（可選）

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public String buildAioCheckoutForm(Integer orderId) {
        return buildAioCheckoutForm(orderId, null);
    }

    @Override
    public String buildAioCheckoutForm(Integer orderId, String origin) {
        // 1) 讀訂單資料（含明細）
        OrderDTO o = orderQueryService.getOne(orderId);

        // （可選）避免重複付款
        orderRepo.findById(orderId).ifPresent(ord -> {
            if ("Paid".equalsIgnoreCase(ord.getStatus())) {
                throw new IllegalStateException("此訂單已完成付款");
            }
        });

        // 2) 必填參數（用原始值，不要先做 encode）
        String itemName = (o.getItems() == null || o.getItems().isEmpty())
                ? "PetPick 訂單"
                : o.getItems().stream()
                        .map(it -> {
                            String n = it.getPname();
                            return (n == null || n.isBlank()) ? ("商品" + it.getProductId()) : n;
                        })
                        .collect(Collectors.joining("#")); // 多品項用 # 串

        LocalDateTime tradeTime = (o.getCreatedAt() != null) ? o.getCreatedAt() : LocalDateTime.now();
        int totalAmount = (o.getTotalPrice() == null) ? 0 : o.getTotalPrice();

        String merchantId = prop.getPayment().getMerchantId();
        String hashKey = prop.getPayment().getHashKey();
        String hashIv = prop.getPayment().getHashIv();

        String returnUrl = prop.getReturnUrl(); // 伺服器回拋（需可被外部存取且 HTTPS）
        String orderResultUrl = (origin == null || origin.isBlank())
                ? prop.getOrderResultUrl()
                : origin + "/payment/result";
        String clientBackUrl = (origin == null || origin.isBlank())
                ? prop.getClientBackUrl()
                : origin + "/cart.html";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", buildMerchantTradeNo(orderId)); // <= 20 碼英數且唯一
        params.put("MerchantTradeDate", tradeTime.format(FMT));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(totalAmount));       // 整數字串
        params.put("TradeDesc", "PetPickCheckout");                   // 避免中文導致誤差
        params.put("ItemName", itemName);
        params.put("ReturnURL", returnUrl);
        params.put("OrderResultURL", orderResultUrl);
        params.put("ClientBackURL", clientBackUrl);
        params.put("ChoosePayment", "Credit");
        params.put("EncryptType", "1");

        // 自訂欄位：帶回 orderId 以更新狀態
        params.put("CustomField1", String.valueOf(orderId));

        // 3) 產生 CheckMacValue（依你專案的工具類）
        String checkMac = EcpayCheckMac.generate(params, hashKey, hashIv);
        params.put("CheckMacValue", checkMac);

        if (log.isDebugEnabled()) {
            log.debug("[ECPay] params(before action)={}", params);
        }

        // 4) 綠界送出端點（測試或正式）
        String action = prop.isStage()
                ? "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"
                : "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5";

        // 5) 回傳不含 inline script 的表單，讓前端解析後自行 submit()
        return buildFormHtml(action, params);
    }

    /**
     * 生成唯一且 <= 20 碼的訂單編號（僅英數）
     */
    private String buildMerchantTradeNo(Integer orderId) {
        String base = "PP" + orderId + System.currentTimeMillis();
        return base.length() > 20 ? base.substring(0, 20) : base;
    }

    /**
     * 回傳只有 form 的 HTML（無 inline JS，避免 CSP 問題）
     */
    private String buildFormHtml(String action, Map<String, String> params) {
        StringBuilder inputs = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey();
            String v = e.getValue() == null ? "" : e.getValue();
            inputs.append("<input type=\"hidden\" name=\"")
                    .append(escapeHtml(k))
                    .append("\" value=\"")
                    .append(escapeHtml(v))
                    .append("\"/>\n");
        }
        String safeAction = escapeHtml(action);

        return """
        <!doctype html>
        <html lang="zh-Hant">
          <head><meta charset="utf-8"><title>Redirecting…</title></head>
          <body style="font-family:sans-serif">
            <p>正在導向綠界安全頁面，請稍候…</p>
            <form id="ecpayForm" method="post" action="%s">
              %s
            </form>
            <p>若未自動跳轉，請按下方按鈕完成付款：</p>
            <button type="submit" form="ecpayForm">前往付款</button>
          </body>
        </html>
        """.formatted(safeAction, inputs.toString());
    }

    /**
     * 僅供輸出到 HTML input 的簡單轉義（不影響簽章計算）
     */
    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#x27;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
