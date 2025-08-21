// 讀取 URL 的 orderId（或用 sessionStorage 後備）
const params = new URLSearchParams(window.location.search);
const orderId = params.get("orderId");
const fallbackOrderId = sessionStorage.getItem("last_order_id");
const idToUse = orderId || fallbackOrderId;

const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

document.addEventListener("DOMContentLoaded", async () => {
  fixBs5NavbarToggler();

  if (!idToUse) {
    document.getElementById("order-info").innerHTML =
      `<div class="alert alert-warning">找不到訂單編號，請從<a href="order.html">訂單總覽</a>進入。</div>`;
    return;
  }

  await loadOrder(idToUse);
  updateCartBadge();
});

async function loadOrder(id) {
  try {
    // 1) 先抓整張訂單（含抬頭 + 明細）
    const res = await fetch(`/api/orders/${encodeURIComponent(id)}`);
    if (!res.ok) throw new Error(await res.text().catch(() => "讀取訂單失敗"));
    const o = await res.json();

    // 2) 渲染抬頭
    renderHeader(o);

    // 3) 明細：優先用 o.items；沒有就後備抓 /details
    let items = Array.isArray(o.items) ? o.items : null;
    if (!items) {
      const r2 = await fetch(`/api/orders/${encodeURIComponent(id)}/details`);
      if (r2.ok) items = await r2.json();
    }
    items = items || [];

    // 4) 渲染明細（Integer 金額）
    renderItems(items);

    // 5) 總金額：優先用 order 紀錄；否則用明細小計加總
    const total = (o.totalPrice != null)
      ? Number(o.totalPrice)
      : items.reduce((sum, it) => sum + num(it.subtotal, num(it.unitPrice) * num(it.quantity)), 0);
    document.getElementById("order-total").textContent = fmtCurrency(total);

  } catch (err) {
    console.error(err);
    document.getElementById("order-info").innerHTML =
      `<div class="alert alert-danger">載入訂單失敗：${escapeHtml(err.message || "請稍後再試")}</div>`;
  }
}

function renderHeader(o) {
  document.getElementById("order-info").innerHTML = `
    <p><strong>訂單編號：</strong> #${escapeHtml(o.orderId)}</p>
    <p><strong>訂購日期：</strong> ${fmtDateTime(o.createdAt)}</p>
    <p><strong>狀態：</strong> ${escapeHtml(o.status ?? "")}</p>
    <p><strong>收件人：</strong> ${escapeHtml(o.receiverName ?? "")}（${escapeHtml(o.receiverPhone ?? "")}）</p>
    <p><strong>配送：</strong> ${escapeHtml(o.shippingType ?? "")}
      ${escapeHtml(o.addr ?? o.storeName ?? "")}
      ${o.storeAddress ? `（${escapeHtml(o.storeAddress)}）` : ""}</p>
  `;
}

function renderItems(items) {
  const tbody = document.getElementById("order-items");
  const rows = items.map(it => {
    // 後端 Integer 欄位：unitPrice / subtotal；若沒有再用 price/quantity 推算
    const unit = num(it.unitPrice, it.price);
    const qty  = num(it.quantity);
    const sub  = num(it.subtotal, unit * qty);

    return `
      <tr>
        <td>${escapeHtml(it.pname ?? it.productId)}</td>
        <td>${fmtCurrency(unit)}</td>
        <td>${qty}</td>
        <td>${fmtCurrency(sub)}</td>
      </tr>`;
  }).join('');
  tbody.innerHTML = rows;
}

// 工具：數字處理／格式化
function num(...vals) {
  for (const v of vals) {
    const n = Number(v);
    if (!isNaN(n)) return n;
  }
  return 0;
}
function fmtCurrency(n) {
  const x = num(n);
  return `NT$${x.toLocaleString("zh-Hant-TW")}`;
}
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

// 你的頁面 Navbar 仍是 v4 寫法，補上 v5 data 屬性
function fixBs5NavbarToggler() {
  const toggler = document.querySelector('.navbar-toggler');
  if (toggler && !toggler.hasAttribute('data-bs-toggle')) {
    toggler.setAttribute('data-bs-toggle', 'collapse');
    toggler.setAttribute('data-bs-target', '#navbarNav');
  }
}

// 右上角購物車徽章（付款成功後應為 0）
async function updateCartBadge() {
  try {
    const r = await fetch(`/api/cart/withProduct/${userId}`);
    if (!r.ok) return;
    const data = await r.json();
    const badge = document.getElementById('cart-badge');
    if (badge) badge.textContent = (Array.isArray(data) ? data.length : 0);
  } catch { /* ignore */ }
}