// === js/order.js（訂單總覽＋取消訂單 Modal）===

// 從結帳或登入流程存的 userId；沒有就先用 1
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

const tbody = document.getElementById("order-table-body");
const cartBadge = document.getElementById("cart-badge");
const backToTopBtn = document.getElementById("backToTop");

// 進頁就載入
document.addEventListener("DOMContentLoaded", () => {
  fixBs5NavbarToggler();
  ensureCancelModal();       // ★ 沒有就動態建立 Modal
  wireCancelModalHandlers(); // ★ 綁定 Modal 行為
  loadOrders();
  updateCartBadge();
  wireBackToTop();
});

// 撈使用者的訂單列表
async function loadOrders() {
  try {
    const res = await fetch(`/api/orders/user/${encodeURIComponent(userId)}`);
    if (!res.ok) throw new Error(await res.text().catch(() => `HTTP ${res.status}`));
    const list = await res.json();
    renderTable(Array.isArray(list) ? list : []);
  } catch (err) {
    console.error(err);
    renderTable([]); // 顯示空狀態
  }
}

// 渲染表格（最後一欄同時放 取消＋查看）
function renderTable(orders) {
  if (!orders.length) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="text-center text-muted py-4">
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
    const can = canCancel(status, o.logisticsStatus);
    const tip = cancelTooltip(status, o.logisticsStatus);

    const cancelBtnHtml = can
      ? `<button
           class="btn btn-danger btn-sm cancel-btn"
           data-id="${escapeAttr(id)}"
           data-mtn="${escapeAttr(mtn)}"
           title="取消訂單">取消訂單</button>`
      : `<span class="d-inline-block"
               tabindex="0"
               data-bs-toggle="tooltip"
               data-bs-placement="top"
               title="${escapeAttr(tip)}">
           <button
             class="btn btn-outline-danger btn-sm cancel-btn"
             data-id="${escapeAttr(id)}"
             data-mtn="${escapeAttr(mtn)}"
             disabled
             style="pointer-events: none;">取消訂單</button>
         </span>`;

    return `
      <tr data-order-id="${escapeAttr(id)}">
        <td class="lh-sm">
          <div class="font-monospace">#${escapeHtml(id)}${mtn ? ` / ${escapeHtml(mtn)}` : ""}</div>
        </td>
        <td>${created}</td>
        <td>NT$${total.toLocaleString("zh-Hant-TW")}</td>
        <td><span class="badge ${statusBadgeClass(status)}">${escapeHtml(status)}</span></td>
        <td>
          <a class="btn btn-sm btn-custom" href="orderdetail.html?orderId=${encodeURIComponent(id)}">查看</a>
        </td>
        <td class="text-end">
          ${cancelBtnHtml}
        </td>
      </tr>`;
  }).join('');

  // 重新初始化工具提示（每次重繪表格後都要）
  initTooltips();
}
// 狀態對應顏色（可按你的實際狀態調整）
function statusBadgeClass(s) {
  const k = String(s || "").toLowerCase();
  if (["paid", "已付款"].includes(k)) return "bg-success";
  if (["pending", "待付款"].includes(k)) return "bg-warning text-dark";
  if (["shipped", "已出貨"].includes(k)) return "bg-info text-dark";
  if (["delivered", "已配達"].includes(k)) return "bg-primary";
  if (["cancelled", "取消"].includes(k)) return "bg-secondary";
  if (["failed", "失敗"].includes(k)) return "bg-dark";
  return "bg-light text-dark";
}

// 是否可取消：Pending / Paid 才能；Shipped / Delivered / Cancelled / Failed 不行
function canCancel(status, logisticsStatus) {
  const s = String(status || "").toUpperCase();
  const l = String(logisticsStatus || "").toUpperCase(); // 訂單摘要通常不會有 l，但保留判斷
  if (["CANCELLED", "FAILED", "SHIPPED", "DELIVERED"].includes(s)) return false;
  if (["IN_TRANSIT", "DELIVERED", "PICKED_UP"].includes(l)) return false;
  return true;
}

/* ---------------- 取消訂單：Modal 與事件 ---------------- */

// 動態建立 Modal（若 HTML 未事先放入）
function ensureCancelModal() {
  if (document.getElementById("cancelModal")) return;

  const tpl = document.createElement("div");
  tpl.innerHTML = `
  <div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelModalLabel" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="cancelModalLabel">取消訂單確認</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="關閉"></button>
        </div>
        <div class="modal-body">
          <div class="mb-2">確定要取消這筆訂單嗎？</div>
          <div class="text-muted mb-3">訂單：<span id="cm-order-no" class="font-monospace"></span></div>
          <label for="cm-reason" class="form-label">取消原因（選填）</label>
          <textarea id="cm-reason" class="form-control" rows="3" placeholder="範例：重複下單／想更改付款方式"></textarea>
          <div id="cm-alert" class="alert alert-danger d-none mt-3"></div>
        </div>
        <div class="modal-footer">
          <span id="cm-spin" class="me-auto d-none">
            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            處理中…
          </span>
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">返回</button>
          <button type="button" id="cm-confirm" class="btn btn-danger">確認取消</button>
        </div>
      </div>
    </div>
  </div>`;
  document.body.appendChild(tpl.firstElementChild);
}

// 綁定按鈕與 Modal 行為
function wireCancelModalHandlers() {
  // 列表上的取消按鈕（事件委派）
  document.body.addEventListener("click", (e) => {
    const btn = e.target.closest(".cancel-btn");
    if (!btn) return;

    const id = btn.dataset.id;
    const mtn = btn.dataset.mtn || "";
    // 填入 Modal 內容
    document.getElementById("cm-order-no").textContent = mtn ? `#${id} / ${mtn}` : `#${id}`;
    document.getElementById("cm-reason").value = "";
    const alertBox = document.getElementById("cm-alert");
    alertBox.classList.add("d-none");
    alertBox.textContent = "";

    // 把要取消的 orderId 設在確認按鈕上
    const confirmBtn = document.getElementById("cm-confirm");
    confirmBtn.dataset.id = id;

    bootstrap.Modal.getOrCreateInstance(document.getElementById("cancelModal")).show();
  });

  // Modal：確認取消
  document.getElementById("cm-confirm").addEventListener("click", onConfirmCancel);
}

async function onConfirmCancel(ev) {
  const btn = ev.currentTarget;
  const id = btn.dataset.id;
  const reason = document.getElementById("cm-reason").value.trim();
  const spin = document.getElementById("cm-spin");
  const alertBox = document.getElementById("cm-alert");

  if (!id) return;

  btn.disabled = true;
  spin.classList.remove("d-none");
  alertBox.classList.add("d-none");
  alertBox.textContent = "";

  try {
    // 若你有「使用者端」取消 API，改成 /api/orders/{id}/cancel
    const res = await fetch(`/api/admin/orders/${encodeURIComponent(id)}/cancel`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reason })
    });
    if (!res.ok) {
      const msg = await res.text().catch(() => `取消失敗 (${res.status})`);
      throw new Error(msg);
    }

    // 關掉 Modal
    bootstrap.Modal.getInstance(document.getElementById("cancelModal")).hide();

    // 更新該列狀態與按鈕
    const row = document.querySelector(`tr[data-order-id="${CSS.escape(String(id))}"]`);
    if (row) {
      // 狀態徽章
      const badgeCell = row.children[3]; // 第 4 欄是狀態
      if (badgeCell) {
        badgeCell.innerHTML = `<span class="badge ${statusBadgeClass("CANCELLED")}">CANCELLED</span>`;
      }
      // 取消按鈕 disable
      const cancelBtn = row.querySelector(".cancel-btn");
      if (cancelBtn) cancelBtn.disabled = true;
    }
  } catch (err) {
    alertBox.textContent = err?.message || "取消失敗，請稍後再試。";
    alertBox.classList.remove("d-none");
  } finally {
    spin.classList.add("d-none");
    btn.disabled = false;
  }
}

/* ---------------- 其他工具 ---------------- */

function escapeHtml(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;").replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
function escapeAttr(s) {
  return String(s ?? "").replaceAll('"', "&quot;");
}

// 簡單的時間格式
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

// 更新購物車徽章（顯示品項數）
async function updateCartBadge() {
  try {
    const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`);
    if (!r.ok) return;
    const data = await r.json();
    if (cartBadge) cartBadge.textContent = (Array.isArray(data) ? data.length : 0);
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

// 你的 Navbar 使用了 Bootstrap 5，但還是 v4 的 data-* 寫法；這裡幫你補一個修正
function fixBs5NavbarToggler() {
  const toggler = document.querySelector(".navbar-toggler");
  if (toggler && !toggler.hasAttribute("data-bs-toggle")) {
    toggler.setAttribute("data-bs-toggle", "collapse");
    toggler.setAttribute("data-bs-target", "#navbarNav");
  }
}

function cancelTooltip(status, logisticsStatus) {
  const s = String(status || "").toUpperCase();
  const l = String(logisticsStatus || "").toUpperCase();

  if (s === "SHIPPED" || l === "IN_TRANSIT" || l === "DELIVERED" || l === "PICKED_UP") {
    return `商品已出貨無法取消,請聯繫客服`;
  }
  if (s === "CANCELLED") return "訂單已取消";
  if (s === "FAILED") return "付款失敗的訂單無需取消";
  return "此狀態不可取消";
}

function initTooltips() {
  const els = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
  els.forEach(el => {
    try { new bootstrap.Tooltip(el); } catch (_) { }
  });
}