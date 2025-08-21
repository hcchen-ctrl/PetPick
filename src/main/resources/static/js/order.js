// === js/order.js（訂單總覽／列表）===

// 從結帳或登入流程存的 userId；沒有就先用 1
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

const tbody = document.getElementById("order-table-body");
const cartBadge = document.getElementById("cart-badge");
const backToTopBtn = document.getElementById("backToTop");

// 進頁就載入
document.addEventListener("DOMContentLoaded", () => {
    fixBs5NavbarToggler();
    loadOrders();
    updateCartBadge();
    wireBackToTop();
});

// 撈使用者的訂單列表
async function loadOrders() {
  try {
    const res = await fetch(`/api/orders/user/${userId}`);
    if (!res.ok) throw new Error(await res.text());
    const list = await res.json();
    // Debug：看 API 的鍵名
    console.log("orders sample:", list?.[0]);
    console.log("keys:", list?.[0] ? Object.keys(list[0]) : []);
    renderTable(Array.isArray(list) ? list : []);
  } catch (err) {
    console.error(err);
    renderTable([]); // 顯示空狀態
  }
}

// 渲染表格
function escapeHtml(s) {
    return String(s ?? "")
        .replaceAll("&", "&amp;").replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;").replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

// 渲染表格（同欄顯示：第一行 orderId、第二行 merchantTradeNo；無則留空）
function renderTable(orders) {
    if (!orders.length) {
        tbody.innerHTML = `
      <tr>
        <td colspan="5" class="text-center text-muted py-4">
          尚無任何訂單。
          <a href="commodity.html" class="ms-2">去逛逛 →</a>
        </td>
      </tr>`;
        return;
    }

    // 依建立時間新到舊
    orders.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));

    tbody.innerHTML = orders.map(o => {
        const id = o.orderId ?? o.id;
        const mtn = (o.merchantTradeNo ?? o.mtn ?? "").trim(); 
        const created = fmtDateTime(o.createdAt);
        const total = Number(o.totalPrice ?? 0);
        const status = o.status ?? "";

        return `
      <tr>
        <td class="lh-sm">
          <div class="font-monospace">#${escapeHtml(id)}/${mtn ? escapeHtml(mtn) : ""}</div>
        </td>
        <td>${created}</td>
        <td>NT$${total.toLocaleString("zh-Hant-TW")}</td>
        <td><span class="badge ${statusBadgeClass(status)}">${escapeHtml(status)}</span></td>
        <td>
          <a class="btn btn-sm btn-outline-primary" href="orderdetail.html?orderId=${encodeURIComponent(id)}">查看</a>
        </td>
      </tr>
    `;
    }).join('');
}
// 狀態對應顏色（可按你的實際狀態調整）
function statusBadgeClass(s) {
    const k = (s || "").toLowerCase();
    if (["paid", "已付款"].includes(k)) return "bg-success";
    if (["pending", "待付款"].includes(k)) return "bg-warning text-dark";
    if (["shipped", "已出貨"].includes(k)) return "bg-info text-dark";
    if (["cancelled", "取消"].includes(k)) return "bg-secondary";
    return "bg-light text-dark";
}

// 簡單的時間格式
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

// 更新購物車徽章（顯示品項數）
async function updateCartBadge() {
    try {
        const r = await fetch(`/api/cart/withProduct/${userId}`);
        if (!r.ok) return;
        const data = await r.json();
        if (cartBadge) cartBadge.textContent = (data || []).length;
    } catch { }
}

// 回頂部
function wireBackToTop() {
    if (!backToTopBtn) return;
    window.addEventListener("scroll", () => {
        backToTopBtn.style.display = window.scrollY > 200 ? "flex" : "none";
    });
    backToTopBtn.addEventListener("click", () => {
        window.scrollTo({ top: 0, behavior: "smooth" });
    });
}

// 你頁面的 Navbar 使用了 Bootstrap 5，但還是 v4 的 data-* 寫法；這裡幫你補一個修正
function fixBs5NavbarToggler() {
    const toggler = document.querySelector('.navbar-toggler');
    if (toggler && !toggler.hasAttribute('data-bs-toggle')) {
        toggler.setAttribute('data-bs-toggle', 'collapse');
        toggler.setAttribute('data-bs-target', '#navbarNav');
    }
}
