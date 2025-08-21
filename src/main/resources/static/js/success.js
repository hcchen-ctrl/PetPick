(function () {
    const q = new URLSearchParams(location.search);
    const get = (k, d = "") => q.get(k) || d;
    const setText = (id, v) => {
        const el = document.getElementById(id);
        if (el) el.textContent = v || "—";
    };
    const num = (v, d = 0) => {
        const n = Number(v);
        return Number.isFinite(n) ? n : d;
    };
    const fmtNTD = (n) => `NT$${num(n).toLocaleString("zh-Hant-TW")}`;

    // 若帶了 ok 且不是 1，導去失敗頁（保險）
    const ok = get("ok");
    if (ok && ok !== "1") {
        location.replace("/fail.html?" + q.toString());
        return;
    }

    // 讀參數：優先顯示綠界的 MerchantTradeNo（mtn）
    const mtn = get("mtn") || get("MerchantTradeNo") || ""; // 綠界訂單編號（你送出去的）
    const tradeNo = get("tradeNo") || get("TradeNo") || "";     // 綠界交易序號
    const orderId = get("orderId") || get("CustomField1") || ""; // 內站 orderId（連結用）

    const paymentType = get("PaymentType");     // 例：Credit, WEBATM, CVS...
    const payTime = get("PaymentDate");     // 例：2025/08/20 15:32:10
    const amount = get("TradeAmt");        // 例：1000（字串）
    const msg = get("RtnMsg", "交易成功");

    // 顯示欄位：把「訂單編號」改顯示 mtn（若空則退回顯示 orderId）
    setText("orderId", mtn || orderId);
    setText("tradeNo", tradeNo);
    setText("paymentType", paymentType);
    setText("payTime", payTime);
    setText("amount", amount ? fmtNTD(amount) : "");
    setText("msg", msg);

    // 查看訂單按鈕 → 仍用內站 orderId
    const viewBtn = document.getElementById("viewOrder");
    if (viewBtn) {
        viewBtn.href = orderId ? ("/order.html?orderId=" + encodeURIComponent(orderId)) : "/order.html";
    }

    // 複製按鈕：訂單編號→複製 mtn（若空則複製 orderId）；交易序號→複製 tradeNo
    const copy = async (text) => {
        try {
            if (navigator.clipboard && text) {
                await navigator.clipboard.writeText(text);
                return true;
            }
        } catch { }
        return false;
    };
    const copyOrder = document.getElementById("copyOrderId");
    const copyTrade = document.getElementById("copyTradeNo");
    if (copyOrder) copyOrder.addEventListener("click", async () => {
        const ok = await copy(mtn || orderId);
        if (ok) copyOrder.textContent = "已複製 ✓";
    });
    if (copyTrade) copyTrade.addEventListener("click", async () => {
        const ok = await copy(tradeNo);
        if (ok) copyTrade.textContent = "已複製 ✓";
    });

    // ---- 購物車清空 + 徽章刷新（冪等、安全回退）----
    const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;
    const setBadge = (n) => {
        const el = document.getElementById("cart-badge");
        if (el) el.textContent = String(n ?? 0);
    };

    async function tryDelete(url) {
        try {
            const r = await fetch(url, { method: "DELETE" });
            return r.ok;
        } catch { return false; }
    }

    async function clearCart(userId) {
        // 首選：DELETE /api/cart/user/{id}
        if (await tryDelete(`/api/cart/user/${encodeURIComponent(userId)}`)) return true;
        // 回退：DELETE /api/cart/clear/{id}
        if (await tryDelete(`/api/cart/clear/${encodeURIComponent(userId)}`)) return true;
        return false;
    }

    async function refreshBadge(userId) {
        try {
            const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`);
            const items = r.ok ? await r.json() : [];
            setBadge((Array.isArray(items) ? items.length : 0));
        } catch {
            setBadge(0);
        }
    }

    (async () => {
        await clearCart(userId);     // 後端已清空也沒關係（冪等）
        await refreshBadge(userId);  // 讀一次最新數量
    })();
})();