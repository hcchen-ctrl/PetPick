package com.petpick.petpick.service.shop.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.petpick.petpick.DTO.shop.OrderDTO;
import com.petpick.petpick.MAC.EcpayPaymentCheckMac;
import com.petpick.petpick.repository.shop.OrderRepository;
import com.petpick.petpick.service.shop.EcpayService;
import com.petpick.petpick.service.shop.OrderQueryService;
import org.springframework.stereotype.Service;

import com.petpick.petpick.config.EcpayProperties;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcpayServiceImpl implements EcpayService {

    private final EcpayProperties prop;
    private final OrderQueryService orderQueryService;
    private final OrderRepository orderRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public String buildAioCheckoutForm(Integer orderId) {
        return buildAioCheckoutForm(orderId, null);
    }

    // 若介面也宣告了兩參數版本可以加 @Override；否則保留 public
    public String buildAioCheckoutForm(Integer orderId, String origin) {
        // 1) 讀訂單（含明細）
        OrderDTO o = orderQueryService.getOne(orderId);

        // 避免重複付款
        orderRepo.findById(orderId).ifPresent(ord -> {
            if ("Paid".equalsIgnoreCase(ord.getStatus())) {
                throw new IllegalStateException("此訂單已完成付款");
            }
        });

        // 2) 組必要欄位
        String itemName = (o.getItems() == null || o.getItems().isEmpty())
                ? "PetPick 訂單"
                : o.getItems().stream()
                    .map(it -> {
                        String n = it.getPname();
                        return (n == null || n.isBlank()) ? ("商品" + it.getProductId()) : n;
                    })
                    .collect(Collectors.joining("#")); // 多品項用 # 接

        LocalDateTime tradeTime = (o.getCreatedAt() != null) ? o.getCreatedAt() : LocalDateTime.now();
        int totalAmount = (o.getTotalPrice() == null) ? 0 : o.getTotalPrice();

        String merchantId = prop.getPayment().getMerchantId();
        String hashKey    = prop.getPayment().getHashKey();
        String hashIv     = prop.getPayment().getHashIv();

        // Server 回拋（ReturnURL）/ 使用者頁（OrderResultURL）
        String returnUrl = nullSafe(prop.getReturnUrl());
        String orderResultUrl = nullSafe(prop.getOrderResultUrl()); // 預設用設定檔
        if (origin != null && !origin.isBlank()) {
            // 與你的 Controller: /payment/v2/result 對齊
            orderResultUrl = origin + "/payment/v2/result";
        }
        String clientBackUrl  = (origin == null || origin.isBlank())
                ? nullSafe(prop.getClientBackUrl())
                : origin + "/cart.html";

        // 3) 取得/寫入唯一 MerchantTradeNo，並回存 DB（關鍵！）
        var ord = orderRepo.findById(orderId).orElseThrow();
        String mtn = ord.getMerchantTradeNo();
        if (mtn == null || mtn.isBlank()) {
            mtn = buildMerchantTradeNo(orderId);       // ≤ 20
            ord.setMerchantTradeNo(mtn);
            orderRepo.save(ord);                       // ★ 必須回存，列表/查詢才看得到
        }

        // 4) 組參數
        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", merchantId);
        p.put("MerchantTradeNo", mtn);                 // ★ 不要再覆蓋
        p.put("MerchantTradeDate", tradeTime.format(FMT));
        p.put("PaymentType", "aio");
        p.put("TotalAmount", String.valueOf(totalAmount));
        p.put("TradeDesc", "PetPickCheckout");
        p.put("ItemName", itemName);
        p.put("ReturnURL", returnUrl);
        p.put("OrderResultURL", orderResultUrl);
        p.put("ClientBackURL", clientBackUrl);
        p.put("ChoosePayment", "Credit");
        p.put("EncryptType", "1");
        p.put("CustomField1", String.valueOf(orderId));

        // 5) 濾空 → 簽章
        p = compact(p);
        String mac = EcpayPaymentCheckMac.generate(p, hashKey, hashIv);
        p.put("CheckMacValue", mac);

        // 6) 端點
        String action = prop.isStage()
                ? "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"
                : "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5";

        // Debug
        log.debug("[Pay-DEBUG] stage={}, action={}, MID={}, Key(4)={}, IV(4)={}",
                prop.isStage(), action, merchantId, safe4(hashKey), safe4(hashIv));
        p.forEach((k, v) -> log.debug("[Pay-DEBUG] {}={}", k, v));

        // 7) 回傳表單（不含 inline script；前端會 parse 成 form 並 submit）
        return buildFormHtml(action, p);
    }

    // ===== Helpers =====
    private static Map<String, String> compact(Map<String, String> src) {
        Map<String, String> out = new LinkedHashMap<>();
        src.forEach((k, v) -> { if (v != null && !v.isBlank()) out.put(k, v); });
        return out;
    }

    private static String buildMerchantTradeNo(Integer orderId) {
        String base = "PP" + orderId + System.currentTimeMillis(); // 唯一性
        return base.length() > 20 ? base.substring(0, 20) : base;
    }

    private static String buildFormHtml(String action, Map<String, String> params) {
        StringBuilder inputs = new StringBuilder();
        params.forEach((k, v) -> {
            inputs.append("<input type=\"hidden\" name=\"")
                  .append(escapeHtml(k)).append("\" value=\"")
                  .append(escapeHtml(v)).append("\"/>\n");
        });

        return """
        <!doctype html>
        <html lang="zh-Hant">
        <head><meta charset="utf-8"><title>ECPay</title></head>
        <body style="font-family:sans-serif">
          <form id="ecpayForm" method="post" action="%s">
            %s
          </form>
          <p>正在前往綠界付款頁…</p>
          <button type="submit" form="ecpayForm">若未自動跳轉請按此</button>
        </body>
        </html>
        """.formatted(escapeHtml(action), inputs.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#x27;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static String safe4(String s){
        return (s == null || s.length() < 4) ? "null" : s.substring(0,2) + "**" + s.substring(s.length()-2);
    }
}