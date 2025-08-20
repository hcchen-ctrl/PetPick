(function () {
    const q = new URLSearchParams(location.search);
    const get = (k, d = "") => q.get(k) || d;
    const setText = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v || "—"; };

    // 若帶了 ok 且不是 1，導去失敗頁（保險）
    const ok = get("ok");
    if (ok && ok !== "1") {
        location.replace("/fail.html?" + q.toString());
        return;
    }

    // 讀取參數（controller 有帶什麼就顯示什麼；沒帶則留空）
    const orderId = get("orderId");
    const tradeNo = get("TradeNo");
    const paymentType = get("PaymentType");   // 例：Credit, WEBATM, CVS...
    const payTime = get("PaymentDate");   // 例：2025/08/20 15:32:10
    const amount = get("TradeAmt");      // 例：1000
    const msg = get("RtnMsg", "交易成功");

    setText("orderId", orderId);
    setText("tradeNo", tradeNo);
    setText("paymentType", paymentType);
    setText("payTime", payTime);
    setText("amount", amount);
    setText("msg", msg);

    // 查看訂單按鈕（依你的實際路徑調整）
    const viewBtn = document.getElementById("viewOrder");
    if (viewBtn) {
        // 若你的詳情頁是 /order-detail.html?orderId=xxx，請保留這行
        viewBtn.href = orderId ? ("/order-detail.html?orderId=" + encodeURIComponent(orderId)) : "/order.html";
    }

    // 複製按鈕
    const copy = (text) => navigator.clipboard && text ? navigator.clipboard.writeText(text) : Promise.reject();
    const copyOrder = document.getElementById("copyOrderId");
    const copyTrade = document.getElementById("copyTradeNo");
    if (copyOrder) copyOrder.addEventListener("click", () => copy(orderId).then(() => copyOrder.textContent = "已複製 ✓"));
    if (copyTrade) copyTrade.addEventListener("click", () => copy(tradeNo).then(() => copyTrade.textContent = "已複製 ✓"));
})();


(async () => {
    const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

    // 1) 先嘗試清空（後端已清也沒關係，這是冪等的）
    try {
        await fetch(`/api/cart/clear/${encodeURIComponent(userId)}`, { method: "DELETE" });
    } catch (e) { }

    // 2) 再抓一次購物車數量，更新頁面徽章
    const setBadge = (n) => (document.getElementById("cart-badge") || {}).textContent = String(n ?? 0);
    try {
        const resp = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`);
        const items = resp.ok ? await resp.json() : [];
        setBadge((items || []).length);
    } catch { setBadge(0); }

})();

(async () => {
  const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;
  try { await fetch(`/api/cart/user/${encodeURIComponent(userId)}`, { method: "DELETE" }); } catch {}
  const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`);
  const items = r.ok ? await r.json() : [];
  const el = document.getElementById("cart-badge"); if (el) el.textContent = String((items||[]).length);
})();