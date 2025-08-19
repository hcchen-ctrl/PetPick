// LogisticsController.java
package com.petpick.petpick.controller;

import java.util.LinkedHashMap; // 若沒用 Lombok，改用 LoggerFactory 示範見下方
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping; // 依你的路徑
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.petpick.petpick.config.EcpayProperties;
import com.petpick.petpick.dto.CvsMapReturnDTO;
import com.petpick.petpick.dto.CvsSelectRequest;
import com.petpick.petpick.mac.EcpayCheckMac;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/logistics")
@CrossOrigin(origins = "*")
public class LogisticsController {

    private final EcpayProperties prop;

    public LogisticsController(EcpayProperties prop) {
        this.prop = prop;
    }

    @PostMapping("/cvs/select-store")
    public ResponseEntity<String> selectStore(@RequestBody CvsSelectRequest req) {
        String action = prop.isStage()
                ? "https://logistics-stage.ecpay.com.tw/Express/map"
                : "https://logistics.ecpay.com.tw/Express/map";

        Map<String, String> p = new LinkedHashMap<>();
        p.put("MerchantID", prop.getLogistics().getMerchantId()); // ← 用 logistics 的組
        p.put("MerchantTradeNo", "L" + System.currentTimeMillis());
        p.put("LogisticsType", "CVS");
        p.put("LogisticsSubType", req.getSubType());     // UNIMARTC2C / FAMIC2C / HILIFEC2C / OKMARTC2C
        p.put("IsCollection", "Y"); // Y / N
        p.put("ServerReplyURL", prop.getLogistics().getCvsMapReturnUrl());

        // ★★ 在算 MAC 前輸出完整診斷資訊（遮罩金鑰）★★
        log.warn("[Logi-DEBUG] stage={}, action={}, MID={}, Key(4)={}, IV(4)={}, subType={}, isCollection={}, serverReply={}",
                prop.isStage(),
                action,
                prop.getLogistics().getMerchantId(),
                safe4(prop.getLogistics().getHashKey()),
                safe4(prop.getLogistics().getHashIv()),
                req.getSubType(),
                req.getIsCollection(),
                prop.getLogistics().getCvsMapReturnUrl()
        );
        p.forEach((k, v) -> log.warn("[Logi-DEBUG] {}={}", k, v));

        // 產生檢查碼（同一支工具，無需修改）
        String mac = EcpayCheckMac.generate(
                p,
                prop.getLogistics().getHashKey(), // ← 必須是物流 Key
                prop.getLogistics().getHashIv() // ← 必須是物流 IV
        );
        p.put("CheckMacValue", mac);

        // 回一個會自動 POST 的表單（略）
        return ResponseEntity.ok(buildAutoPostForm(action, p));
    }

    @PostMapping("/api/logistics/cvs/store-return")
    public String handleStoreReturn(@ModelAttribute CvsMapReturnDTO dto, RedirectAttributes attr) {
        log.info("選店回傳：{}", dto);

        // 轉回 checkout.html 並附帶超商資料
        attr.addAttribute("storeId", dto.getStoreID());
        attr.addAttribute("storeName", dto.getStoreName());
        attr.addAttribute("storeAddr", dto.getStoreAddress());
        return "redirect:/checkout.html";
    }

    private String buildAutoPostForm(String action, Map<String, String> params) {
        StringBuilder inputs = new StringBuilder();
        params.forEach((k, v) -> {
            inputs.append("<input type='hidden' name='")
                    .append(escapeHtml(k))
                    .append("' value='")
                    .append(escapeHtml(v == null ? "" : v))
                    .append("'/>");
        });
        return """
      <!doctype html>
      <html><head><meta charset="utf-8"><title>Redirecting…</title></head>
      <body onload="document.forms[0].submit()" style="font-family:sans-serif">
        <form method="post" action="%s">%s</form>
        <noscript><button type="submit" form="0">前往選店</button></noscript>
      </body></html>
      """.formatted(escapeHtml(action), inputs.toString());
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    // ★ 你問題中的 safe4 就放在這裡
    private String safe4(String s) {
        return (s == null || s.length() < 4) ? "null" : s.substring(0, 2) + "**" + s.substring(s.length() - 2);
    }
}
