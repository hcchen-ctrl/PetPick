(function () {
  const q = new URLSearchParams(location.search);
  const get = (k, d = "") => q.get(k) || d;
  const setText = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v || "—"; };
  const num = (v, d = 0) => { const n = Number(v); return Number.isFinite(n) ? n : d; };
  const fmtNTD = (n) => `NT$${num(n).toLocaleString("zh-Hant-TW")}`;
  const fmtDT = (iso) => {
    if (!iso) return "";
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    const y=d.getFullYear(), m=String(d.getMonth()+1).padStart(2,"0"), day=String(d.getDate()).padStart(2,"0");
    const hh=String(d.getHours()).padStart(2,"0"), mm=String(d.getMinutes()).padStart(2,"0"), ss=String(d.getSeconds()).padStart(2,"0");
    return `${y}/${m}/${day} ${hh}:${mm}:${ss}`;
  };

  // 保險：ok!=1 就導到 fail
  const ok = get("ok");
  if (ok && ok !== "1") { location.replace("/fail.html?" + q.toString()); return; }

  // 讀參數
  const mtn     = get("mtn") || get("MerchantTradeNo") || "";  // 綠界訂單編號（顯示）
  const tradeNo = get("tradeNo") || get("TradeNo") || "";      // 綠界交易序號
  const orderId = get("orderId") || get("CustomField1") || ""; // 內站 orderId（明細連結）

  let paymentType = get("PaymentType"); // e.g., Credit / WEBATM / CVS
  let payTime     = get("PaymentDate"); // e.g., 2025/08/20 15:32:10
  let amount      = get("TradeAmt");    // e.g., "1000"

  // 先把可得的直接顯示
  setText("orderId", mtn || orderId);
  setText("tradeNo", tradeNo);
  setText("paymentType", paymentType);
  setText("payTime", payTime);
  setText("amount", amount ? fmtNTD(amount) : "");
  setText("msg", get("RtnMsg", "交易成功"));

  // 查看訂單按鈕 → 用內站 orderId
  const viewBtn = document.getElementById("viewOrder");
  if (viewBtn) viewBtn.href = orderId ? ("/order.html?orderId=" + encodeURIComponent(orderId)) : "/order.html";

  // ---- 後備：若沒有付款方式/時間/金額，就自己補 ----
  (async () => {
    // A) 付款方式：用結帳頁暫存（需在 checkout 頁送單前寫入 sessionStorage.last_payment）
    if (!paymentType) {
      const last = (sessionStorage.getItem("last_payment") || "").toLowerCase();
      const map = { credit: "信用卡", cod: "貨到付款", cash: "現金", webatm: "網路 ATM", cvs: "超商代碼" };
      paymentType = map[last] || last.toUpperCase() || "";
      if (paymentType) setText("paymentType", paymentType);
    }

    // B) 金額/時間：打訂單 API 補
    if ((!amount || !payTime) && orderId) {
      try {
        const r = await fetch(`/api/orders/${encodeURIComponent(orderId)}`);
        if (r.ok) {
          const o = await r.json();
          if (!amount && o.totalPrice != null) {
            amount = String(o.totalPrice);
            setText("amount", fmtNTD(amount));
          }
          // 優先 paidAt，沒有就 createdAt
          const t = o.paidAt || o.createdAt;
          if (!payTime && t) {
            payTime = fmtDT(t);
            setText("payTime", payTime);
          }
          // 若付款方式還是沒有，且 o.status=Paid，可補成「信用卡」；或依你系統欄位補
          if (!paymentType && (o.status || "").toLowerCase() === "paid") {
            setText("paymentType", "信用卡");
          }
        }
      } catch { /* ignore */ }
    }
  })();

  // ---- 清空購物車 + 徽章刷新（冪等）----
  const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;
  const setBadge = (n) => { const el = document.getElementById("cart-badge"); if (el) el.textContent = String(n ?? 0); };
  async function tryDelete(url) { try { const r = await fetch(url, { method: "DELETE" }); return r.ok; } catch { return false; } }
  async function clearCart(uid) {
    if (await tryDelete(`/api/cart/user/${encodeURIComponent(uid)}`)) return true;
    if (await tryDelete(`/api/cart/clear/${encodeURIComponent(uid)}`)) return true;
    return false;
  }
  async function refreshBadge(uid) {
    try {
      const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(uid)}`);
      const items = r.ok ? await r.json() : [];
      setBadge(Array.isArray(items) ? items.length : 0);
    } catch { setBadge(0); }
  }
  (async () => { await clearCart(userId); await refreshBadge(userId); })();
})();