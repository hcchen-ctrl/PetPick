// === orderDetail.js（穩健版 + 三段進度條 + 輕量輪詢10秒＋分頁隱藏自動暫停）===

// 讀 URL 參數 & 既有暫存
const q = new URLSearchParams(location.search);
const orderIdParam = q.get("orderId");
const mtnParam = q.get("mtn") || q.get("MerchantTradeNo");
const tradeNoParam = q.get("tradeNo") || q.get("TradeNo");
const lastOrderId = sessionStorage.getItem("last_order_id");
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// ---------- 輕量輪詢（全域狀態） ----------
let __currentOrderId = null;
let __progressTimer = null;

document.addEventListener("DOMContentLoaded", async () => {
  fixBs5NavbarToggler();
  try {
    const id = await resolveOrderId();
    if (!id) {
      showWarn("找不到訂單編號，請從 <a href='order.html'>訂單總覽</a> 進入。");
      return;
    }

    await loadOrder(id);

    // ★ 記住目前訂單並啟動 10 秒輪詢（僅在前景）
    __currentOrderId = String(id);
    startProgressPolling(__currentOrderId, 10000);

    // ★ 分頁隱藏就停；回前景立即刷新一次並恢復輪詢
    document.addEventListener("visibilitychange", () => {
      if (!__currentOrderId) return;
      if (document.hidden) {
        stopProgressPolling();
      } else {
        // 回到前景：立即刷新一次畫面，然後恢復輪詢
        loadOrder(__currentOrderId).catch(() => {});
        startProgressPolling(__currentOrderId, 10000);
      }
    });

    await updateCartBadge();
  } catch (err) {
    console.error(err);
    showErr(`載入訂單失敗：${escapeHtml(err?.message || "請稍後再試")}`);
  }
});

/** 決定要用哪個訂單 ID 呼叫後端 */
async function resolveOrderId() {
  // 1) 直接帶 orderId
  if (orderIdParam) return orderIdParam;

  // 2) 用 MTN（綠界訂單編號）
  if (mtnParam) {
    const res = await fetch(`/api/orders/by-mtn/${encodeURIComponent(mtnParam)}`);
    if (res.ok) {
      const dto = await res.json();
      if (dto?.orderId) return String(dto.orderId);
    }
  }

  // 3) 用 TradeNo（綠界交易序號）
  if (tradeNoParam) {
    const res = await fetch(`/api/orders/by-tradeno/${encodeURIComponent(tradeNoParam)}`);
    if (res.ok) {
      const dto = await res.json();
      if (dto?.orderId) return String(dto.orderId);
    }
  }

  // 4) session 記錄的最後一筆
  if (lastOrderId) return lastOrderId;

  // 5) 最後退路：抓使用者訂單列表，取最新一筆
  try {
    const res = await fetch(`/api/orders/user/${encodeURIComponent(userId)}`);
    if (res.ok) {
      const list = await res.json();
      if (Array.isArray(list) && list.length > 0) {
        list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
        return String(list[0].orderId ?? list[0].id);
      }
    }
  } catch { /* ignore */ }

  return null;
}

async function loadOrder(id) {
  // 1) 撈整張訂單（抬頭＋可能含 items）
  const res = await fetch(`/api/orders/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(await res.text().catch(() => "讀取訂單失敗"));
  const o = await res.json();

  // 2) 抬頭 & 進度條
  renderHeader(o);
  renderProgress(o);  // ★ 三段式進度條

  const progressEl = document.querySelector(".order-progress");
  if (progressEl) {
    if (isCancelledStatus(o.status)) {
      // 隱藏進度條
      progressEl.classList.add("d-none");
    } else {
      // 顯示並更新進度條（若你有 updateOrderProgress / applyOrderProgress）
      progressEl.classList.remove("d-none");
      if (typeof updateOrderProgress === "function") {
        updateOrderProgress(o); // 依你的實作更新「建立訂單/已出貨/已配達」
      }
    }
  }

  // 3) 明細：優先 o.items；無則撈 /details
  let items = Array.isArray(o.items) ? o.items : null;
  if (!items) {
    const r2 = await fetch(`/api/orders/${encodeURIComponent(id)}/details`);
    if (r2.ok) items = await r2.json();
  }
  items = items || [];
  renderItems(items);

  // 4) 總金額：優先 order.totalPrice；否則以明細合計
  const total = (o.totalPrice != null)
    ? Number(o.totalPrice)
    : items.reduce((sum, it) => sum + num(it.subtotal, num(it.unitPrice) * num(it.quantity)), 0);
  setText("order-total", fmtCurrency(total));
}

function renderHeader(o) {
  const lines = [];

  // 訂單編號
  const leftNo = `#${escapeHtml(o.orderId)}`;
  const rightNo = o.merchantTradeNo ? `（${escapeHtml(o.merchantTradeNo)}）` : "";
  lines.push(`<p><strong>訂單編號：</strong> ${leftNo}${rightNo}</p>`);

  // 綠界交易序號
  if (o.tradeNo) {
    lines.push(`<p><strong>綠界交易序號：</strong> <span class="font-monospace">${escapeHtml(o.tradeNo)}</span></p>`);
  }

  // 訂購日期
  lines.push(`<p><strong>訂購日期：</strong> ${fmtDateTime(o.createdAt)}</p>`);

  // ===== 付款方式（加在訂購日期下）=====
  const url = new URLSearchParams(location.search);
  const urlPayType = (url.get("PaymentType") || url.get("paymentType") || "").toUpperCase();
  const lastPay = (sessionStorage.getItem("last_payment") || "").toLowerCase();

  const mapFromCode = (code) => {
    const s = String(code || "").toUpperCase();
    // ECPay 常見型別
    if (s.startsWith("CREDIT")) return "信用卡";
    if (s.startsWith("WEBATM")) return "網路 ATM";
    if (s.startsWith("ATM")) return "ATM 轉帳";
    if (s.startsWith("CVS")) return "超商代碼";
    if (s.startsWith("BARCODE")) return "超商條碼";
    return s || "";
  };

  const mapFromLast = (s) => {
    const k = s.toLowerCase();
    if (k === "credit") return "信用卡";
    if (k === "webatm") return "網路 ATM";
    if (k === "atm") return "ATM 轉帳";
    if (k === "cvs") return "超商代碼";
    if (k === "cod") return "貨到付款";
    if (k === "cash") return "現金";
    return "";
  };

  const paymentLabel = (() => {
    // 1) URL 上帶回的 PaymentType（如 Credit_CreditCard / WEBATM / CVS）
    if (urlPayType) {
      // 只看第一段：CREDIT、WEBATM、CVS、ATM…
      return mapFromCode(urlPayType.split("_")[0]);
    }
    // 2) checkout 時可能寫到 sessionStorage 的 last_payment
    if (lastPay) {
      const m = mapFromLast(lastPay);
      if (m) return m;
    }
    // 3) 後端欄位
    const gw = (o.paymentGateway || o.gateway || "").toLowerCase();
    if (gw.includes("ecpay")) return "線上付款（ECPay）";
    if (gw.includes("cash")) return "現金";
    if (gw.includes("bank") || gw.includes("transfer")) return "銀行轉帳";
    // 4) 推論：有 tradeNo/merchantTradeNo 大多為線上付款；超商取貨付款除外
    const shipType = (o.shippingType || "").toLowerCase();
    if (shipType === "cvs_cod") return "超商取貨付款";
    if (o.tradeNo || o.merchantTradeNo) return "線上付款";
    // 5) 狀態為已付款也給一個合理預設
    if (String(o.status || "").toLowerCase() === "paid") return "線上付款";
    return "—";
  })();

  lines.push(`<p><strong>付款方式：</strong> ${escapeHtml(paymentLabel)}</p>`);

  // 狀態、收件人
  lines.push(`<p><strong>狀態：</strong> ${escapeHtml(o.status ?? "")}</p>`);
  lines.push(`<p><strong>收件人：</strong> ${escapeHtml(o.receiverName ?? "")}（${escapeHtml(o.receiverPhone ?? "")}）</p>`);

  // 配送方式
  let deliveryHtml = "";
  const st = (o.shippingType || "").toLowerCase();
  if (st === "cvs_cod") {
    const brand = o.storeBrand || "";
    const parts = [brand, o.storeName, o.storeAddress].filter(Boolean).join(" ");
    deliveryHtml = `超商取貨付款${parts ? `（${escapeHtml(parts)}）` : ""}`;
  } else if (st === "address") {
    deliveryHtml = `宅配 ${escapeHtml(o.addr || "")}`;
  } else {
    const where = o.addr || o.storeName || "";
    const extra = o.storeAddress ? `（${escapeHtml(o.storeAddress)}）` : "";
    deliveryHtml = `${escapeHtml(o.shippingType || "")} ${escapeHtml(where)}${extra}`;
  }
  lines.push(`<p><strong>配送方式：</strong> ${deliveryHtml}</p>`);

  const box = document.getElementById("order-info");
  if (box) box.innerHTML = lines.join("\n");
}

/** 三段式進度條：建立訂單 → 已出貨 → 已配達 */
function renderProgress(o) {
  // 0) 找/建容器
  let wrap = document.getElementById("order-progress");
  if (!wrap) {
    const info = document.getElementById("order-info");
    wrap = document.createElement("section");
    wrap.id = "order-progress";
    wrap.className = "order-progress my-4";
    wrap.innerHTML = progressSkeleton();
    if (info && info.parentNode) info.parentNode.insertBefore(wrap, info.nextSibling);
  } else {
    wrap.innerHTML = progressSkeleton();
  }

  // 1) 正規化欄位
  const stat  = String(o.status || "").toUpperCase();
  const lstat = String(o.logisticsStatus || "").toUpperCase();
  const shippedAtRaw   = o.shippedAt   ?? o.shipAt   ?? o.shipped_at   ?? o.ship_at   ?? null;
  const deliveredAtRaw = o.deliveredAt ?? o.receivedAt ?? o.delivered_at ?? o.received_at ?? null;

  // 2) 里程碑：是否「已出貨事件」/「已送達事件」
  const shippedReached   = !!shippedAtRaw || lstat === "IN_TRANSIT" || !!o.trackingNo;
  const deliveredReached = lstat === "DELIVERED" || !!deliveredAtRaw;

  // 3) 套用樣式（點與橫桿）
  const steps = wrap.querySelectorAll(".op-step");
  const bars  = wrap.querySelectorAll(".op-bar");

  const s1 = steps[0], s2 = steps[1], s3 = steps[2];
  if (s1) {
    s1.classList.toggle("done",   shippedReached || deliveredReached); // 只要越過出貨就算完成第1步
    s1.classList.toggle("active", !shippedReached && !deliveredReached);
  }
  if (s2) {
    s2.classList.toggle("done",   shippedReached);                     // ★ 已出貨就變綠
    s2.classList.toggle("active", shippedReached && !deliveredReached);// 未配達前維持當前步
  }
  if (s3) {
    s3.classList.toggle("done",   deliveredReached);
    s3.classList.toggle("active", deliveredReached);
  }

  const b1 = bars[0], b2 = bars[1];
  if (b1) {
    b1.classList.toggle("done",   shippedReached);
    b1.classList.toggle("active", !shippedReached && !deliveredReached);
  }
  if (b2) {
    b2.classList.toggle("done",   deliveredReached);
    b2.classList.toggle("active", shippedReached && !deliveredReached);
  }

  // 4) 時間顯示
  setTextEl(wrap, "#op-created",   fmtDT(o.createdAt));
  setTextEl(wrap, "#op-shipped",   fmtDT(shippedAtRaw)   || (shippedReached   ? "出貨處理中…" : "—"));
  setTextEl(wrap, "#op-delivered", fmtDT(deliveredAtRaw) || (deliveredReached ? "已完成"       : "—"));

  // 5) 出貨中 ETA
  if (shippedReached && !deliveredReached) {
    const base = shippedAtRaw || o.createdAt;
    const guess = eta(base, 2);
    if (guess) setTextEl(wrap, "#op-delivered", `預計 ${guess} 送達`);
  }

  // 6) 取消單隱藏
  if (typeof isCancelledStatus === "function" && isCancelledStatus(o.status)) {
    wrap.classList.add("d-none");
  } else {
    wrap.classList.remove("d-none");
  }
}
function progressSkeleton() {
  return `
    <div class="op-steps">
      <div class="op-step" data-step="1">
        <div class="op-dot"></div>
        <div class="op-label">建立訂單</div>
        <div class="op-time" id="op-created">—</div>
      </div>
      <div class="op-bar"></div>
      <div class="op-step" data-step="2">
        <div class="op-dot"></div>
        <div class="op-label">已出貨</div>
        <div class="op-time" id="op-shipped">—</div>
      </div>
      <div class="op-bar"></div>
      <div class="op-step" data-step="3">
        <div class="op-dot"></div>
        <div class="op-label">已配達</div>
        <div class="op-time" id="op-delivered">—</div>
      </div>
    </div>`;
}

function decideStep({ stat, lstat, shippedAt, deliveredAt, trackingNo }) {
  if (lstat === "DELIVERED" || !!deliveredAt) return 3;
  if (stat === "SHIPPED" || lstat === "IN_TRANSIT" || lstat === "CREATED" || !!trackingNo || !!shippedAt) return 2;
  return 1;
}

/** 渲染明細表格（缺少時補上空表列） */
function renderItems(items) {
  const tbody = document.getElementById("order-items");
  if (!tbody) return;

  const rows = (Array.isArray(items) ? items : []).map(it => {
    // 後端欄位兼容：優先 unitPrice/subtotal；沒有就用 price*quantity 推算
    const unit = num(it.unitPrice, it.price);
    const qty = num(it.quantity);
    const sub = num(it.subtotal, unit * qty);

    return `
      <tr>
        <td>${escapeHtml(it.pname ?? it.productId ?? "")}</td>
        <td>${fmtCurrency(unit)}</td>
        <td>${qty}</td>
        <td>${fmtCurrency(sub)}</td>
      </tr>
    `;
  }).join("");

  tbody.innerHTML = rows || `<tr><td colspan="4" class="text-muted text-center py-4">此訂單沒有明細資料</td></tr>`;
}

// ---------- 輕量輪詢（只更新進度條；前景才跑） ----------
function startProgressPolling(orderId, intervalMs = 10000) {
  stopProgressPolling();                 // 防重複
  if (!orderId) return;
  if (document.hidden) return;           // 在背景就不啟動

  __progressTimer = setInterval(async () => {
    // 若中途變背景，立即停掉
    if (document.hidden) {
      stopProgressPolling();
      return;
    }
    try {
      const r = await fetch(`/api/orders/${encodeURIComponent(orderId)}`);
      if (!r.ok) return;
      const o = await r.json();
      renderProgress(o);

      const delivered = String(o.logisticsStatus || "").toUpperCase() === "DELIVERED" || !!o.deliveredAt;
      const cancelled = typeof isCancelledStatus === "function" && isCancelledStatus(o.status);
      if (delivered || cancelled) stopProgressPolling();
    } catch { /* ignore */ }
  }, Math.max(3000, intervalMs));
}

function stopProgressPolling() {
  if (__progressTimer) {
    clearInterval(__progressTimer);
    __progressTimer = null;
  }
}

window.addEventListener("beforeunload", stopProgressPolling);

// ---------- 工具區 ----------
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v || "—"; }
function setTextEl(root, sel, v) { const el = root.querySelector(sel); if (el) el.textContent = v || "—"; }
function num(...vals) { for (const v of vals) { const n = Number(v); if (Number.isFinite(n)) return n; } return 0; }
function fmtCurrency(n) { return `NT$${num(n).toLocaleString("zh-Hant-TW")}`; }
function fmtDateTime(iso) {
  const d = toDateSafe(iso);
  if (!d) return "";
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}`;
}
function fmtDT(iso) {
  const d = toDateSafe(iso);
  if (!d) return "";
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0"), mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}/${m}/${day} ${hh}:${mm}`;
}
function eta(iso, days = 2) {
  if (!iso) return "";
  const d = new Date(iso); if (isNaN(d)) return "";
  d.setDate(d.getDate() + days);
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  return `${y}/${m}/${day}`;
}
function toDateSafe(input) {
  if (!input) return null;
  if (input instanceof Date) return isNaN(input) ? null : input;
  if (typeof input === "number") { const d = new Date(input); return isNaN(d) ? null : d; }
  let s = String(input).trim();
  // 把 "yyyy-MM-dd HH:mm:ss" 轉為 ISO
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/.test(s)) s = s.replace(" ", "T");
  // 將超過 3 碼的小數裁到 3 碼（Safari 對 .123456 可能會炸）
  s = s.replace(/(\.\d{3})\d+/, "$1");
  // 不強加時區，保留本地時間語意
  const d = new Date(s);
  return isNaN(d) ? null : d;
}
function escapeHtml(s) {
  return String(s ?? "")
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
function showWarn(html) { const box = document.getElementById("order-info"); if (box) box.innerHTML = `<div class="alert alert-warning">${html}</div>`; }
function showErr(html) { const box = document.getElementById("order-info"); if (box) box.innerHTML = `<div class="alert alert-danger">${html}</div>`; }

// Navbar v5 修正
function fixBs5NavbarToggler() {
  const toggler = document.querySelector('.navbar-toggler');
  if (toggler && !toggler.hasAttribute('data-bs-toggle')) {
    toggler.setAttribute('data-bs-toggle', 'collapse');
    toggler.setAttribute('data-bs-target', '#navbarNav');
  }
}

// 徽章刷新
async function updateCartBadge() {
  try {
    const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`);
    if (!r.ok) return;
    const data = await r.json();
    const badge = document.getElementById('cart-badge');
    if (badge) badge.textContent = (Array.isArray(data) ? data.length : 0);
  } catch { /* ignore */ }
}

// === 小工具：判斷是否取消狀態 ===
function isCancelledStatus(s) {
  const raw = String(s || "").trim();
  const k = raw.toLowerCase();
  return k === "cancelled" || k === "canceled" || raw === "取消";
}