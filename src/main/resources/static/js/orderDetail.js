// === orderDetail.js（穩健版：支援 orderId / mtn / tradeNo，多層回退）===

// 讀 URL 參數 & 既有暫存
const q = new URLSearchParams(location.search);
const orderIdParam = q.get("orderId");
const mtnParam = q.get("mtn") || q.get("MerchantTradeNo");
const tradeNoParam = q.get("tradeNo") || q.get("TradeNo");
const lastOrderId = sessionStorage.getItem("last_order_id");
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

document.addEventListener("DOMContentLoaded", async () => {
  fixBs5NavbarToggler();
  try {
    const id = await resolveOrderId();
    if (!id) {
      showWarn("找不到訂單編號，請從<a href='order.html'>訂單總覽</a>進入。");
      return;
    }
    await loadOrder(id);
    await updateCartBadge();
  } catch (err) {
    console.error(err);
    showErr(`載入訂單失敗：${escapeHtml(err?.message || "請稍後再試")}`);
  }
});

/** 核心：決定要用哪個訂單 ID 呼叫後端 */
async function resolveOrderId() {
  // 1) 直接帶 orderId
  if (orderIdParam) return orderIdParam;

  // 2) 有 MTN（綠界訂單編號）→ 後端解析/查詢並回傳 OrderDTO
  if (mtnParam) {
    const res = await fetch(`/api/orders/by-mtn/${encodeURIComponent(mtnParam)}`);
    if (res.ok) {
      const dto = await res.json();
      if (dto?.orderId) return String(dto.orderId);
    }
  }

  // 3) 有 TradeNo（綠界交易序號）
  if (tradeNoParam) {
    const res = await fetch(`/api/orders/by-tradeno/${encodeURIComponent(tradeNoParam)}`);
    if (res.ok) {
      const dto = await res.json();
      if (dto?.orderId) return String(dto.orderId);
    }
  }

  // 4) 有 session 裡最後一次下單的 id
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
  } catch { }

  return null;
}

async function loadOrder(id) {
  // 1) 先抓整張訂單（抬頭＋可能含 items）
  const res = await fetch(`/api/orders/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(await res.text().catch(() => "讀取訂單失敗"));
  const o = await res.json();

  // 2) 渲染抬頭（若後端有帶 merchantTradeNo / tradeNo，也順便顯示）
  renderHeader(o);

  // 3) 明細：優先 o.items；無則撈 /details
  let items = Array.isArray(o.items) ? o.items : null;
  if (!items) {
    const r2 = await fetch(`/api/orders/${encodeURIComponent(id)}/details`);
    if (r2.ok) items = await r2.json();
  }
  items = items || [];
  renderItems(items);

  // 4) 總金額：優先用 order.totalPrice；否則以明細合計
  const total = (o.totalPrice != null)
    ? Number(o.totalPrice)
    : items.reduce((sum, it) => sum + num(it.subtotal, num(it.unitPrice) * num(it.quantity)), 0);
  setText("order-total", fmtCurrency(total));
}

function renderHeader(o) {
  const lines = [];
  // 顯示優先：綠界訂單編號（MerchantTradeNo）
  const displayNo = o.orderId ? escapeHtml(o.orderId) : `#${escapeHtml(o.orderId)}`;
  lines.push(`<p><strong>訂單編號：</strong> ${displayNo}</p>`);
  if (o.tradeNo) {
    lines.push(`<p><strong>綠界交易序號：</strong> <span class="font-monospace">${escapeHtml(o.tradeNo)}</span></p>`);
  }
  lines.push(`<p><strong>訂購日期：</strong> ${fmtDateTime(o.createdAt)}</p>`);
  lines.push(`<p><strong>狀態：</strong> ${escapeHtml(o.status ?? "")}</p>`);
  lines.push(`<p><strong>收件人：</strong> ${escapeHtml(o.receiverName ?? "")}（${escapeHtml(o.receiverPhone ?? "")}）</p>`);
  lines.push(`<p><strong>配送：</strong> ${escapeHtml(o.shippingType ?? "")} ${escapeHtml(o.addr ?? o.storeName ?? "")} ${o.storeAddress ? `（${escapeHtml(o.storeAddress)}）` : ""}</p>`);

  document.getElementById("order-info").innerHTML = lines.join("\n");
}

function renderItems(items) {
  const tbody = document.getElementById("order-items");
  const rows = items.map(it => {
    const unit = num(it.unitPrice, it.price);
    const qty = num(it.quantity);
    const sub = num(it.subtotal, unit * qty);
    return `
      <tr>
        <td>${escapeHtml(it.pname ?? it.productId)}</td>
        <td>${fmtCurrency(unit)}</td>
        <td>${qty}</td>
        <td>${fmtCurrency(sub)}</td>
      </tr>`;
  }).join('');
  tbody.innerHTML = rows || `<tr><td colspan="4" class="text-muted text-center py-4">此訂單沒有明細資料</td></tr>`;
}

// ---------- 工具區 ----------
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v || "—"; }
function num(...vals) { for (const v of vals) { const n = Number(v); if (Number.isFinite(n)) return n; } return 0; }
function fmtCurrency(n) { return `NT$${num(n).toLocaleString("zh-Hant-TW")}`; }
function fmtDateTime(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d)) return escapeHtml(iso);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${day} ${hh}:${mm}`;
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
  } catch { }
}