// 讀取 URL 的 orderId
const params = new URLSearchParams(window.location.search);
const orderId = params.get("orderId");

// 若你有在結帳頁存 last_order_id，沒帶參數時可退而求其次
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
        const res = await fetch(`/api/orders/${id}`);
        if (!res.ok) throw new Error(await res.text());
        const o = await res.json();

        // 基本資訊
        document.getElementById("order-info").innerHTML = `
      <p><strong>訂單編號：</strong> #${o.orderId}</p>
      <p><strong>訂購日期：</strong> ${fmtDateTime(o.createdAt)}</p>
      <p><strong>狀態：</strong> ${o.status ?? ""}</p>
      <p><strong>收件人：</strong> ${escapeHtml(o.receiverName ?? "")}（${escapeHtml(o.receiverPhone ?? "")}）</p>
      <p><strong>配送：</strong> ${escapeHtml(o.shippingType ?? "")}　${escapeHtml(o.addr ?? o.storeName ?? "")}
      ${o.storeAddress ? `（${escapeHtml(o.storeAddress)}）` : ""}</p>
    `;

        // 明細
        const tbody = document.getElementById("order-items");
        const rows = (o.items || []).map(it => {
            const subtotal = (Number(it.price) || 0) * (Number(it.quantity) || 0);
            return `
        <tr>
          <td>${escapeHtml(it.pname ?? it.productId)}</td>
          <td>NT$${it.price}</td>
          <td>${it.quantity}</td>
          <td>NT$${subtotal}</td>
        </tr>`;
        }).join('');
        tbody.innerHTML = rows;

        // 總金額
        const total = o.totalPrice ?? (o.items || []).reduce((s, it) =>
            s + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0);
        document.getElementById("order-total").textContent = `NT$${total}`;

    } catch (err) {
        console.error(err);
        document.getElementById("order-info").innerHTML =
            `<div class="alert alert-danger">載入訂單失敗：${err.message || "請稍後再試"}</div>`;
    }
}

function fmtDateTime(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    return `${y}-${m}-${day} ${hh}:${mm}`;
}

async function updateCartBadge() {
    try {
        const r = await fetch(`/api/cart/withProduct/${userId}`);
        if (!r.ok) return;
        const data = await r.json();
        const badge = document.getElementById('cart-badge');
        if (badge) badge.textContent = (data || []).length; // 品項數
    } catch { }
}

function escapeHtml(s) {
    return String(s)
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;').replaceAll('"', '&quot;')
        .replaceAll("'", "&#039;");
}

// 你的頁面 Navbar 仍是 v4 寫法，補上 v5 data 屬性
function fixBs5NavbarToggler() {
    const toggler = document.querySelector('.navbar-toggler');
    if (toggler && !toggler.hasAttribute('data-bs-toggle')) {
        toggler.setAttribute('data-bs-toggle', 'collapse');
        toggler.setAttribute('data-bs-target', '#navbarNav');
    }
}
