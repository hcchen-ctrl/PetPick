// === cart.js (直接覆蓋你現有檔案) ===
let cartData = [];
const userId = 1; // TODO：之後接登入改這裡或從 session/localStorage 取

document.addEventListener("DOMContentLoaded", () => {
    wireDeleteConfirmOnce(); // 綁定刪除確認按鈕（只綁一次）
    wireCheckoutButton();    // 綁定去結帳按鈕
    loadCart();
});

// 讀購物車
function loadCart() {
    fetch(`/api/cart/withProduct/${userId}`)
        .then(res => {
            if (!res.ok) throw new Error("載入購物車失敗");
            return res.json();
        })
        .then(data => {
            cartData = data || [];
            renderCart();
            refreshTotalsAndBadge();
            refreshCheckoutButtonState();
        })
        .catch(err => console.error(err));
}

// 繪製列表
function renderCart() {
    const tbody = document.getElementById("cart-items");
    let total = 0;

    const rowsHtml = (cartData || []).map(item => {
        const price = Number(item.price) || 0;
        const quantity = Number(item.quantity) || 0;
        const subtotal = price * quantity;
        const name = item.pname || "";
        const id = item.productId;
        total += subtotal;

        return `
            <tr>
                <td>
                    <a href="productSite.html?id=${encodeURIComponent(id)}" class="text-decoration-none" style="color: black;">
                        <img src="${item.imageUrl || '#'}" class="cart-img rounded" alt="商品圖" />
                    </a>
                </td>
                <td>
                    <a href="productSite.html?id=${encodeURIComponent(id)}" class="text-decoration-none" style="color: black;">
                        ${name}
                    </a>
                </td>
                <td>NT$${price}</td>
                <td>
                    <input type="number" value="${quantity}" min="1"
                        class="form-control form-control-sm w-50"
                        onchange="updateQuantity(${item.cartId}, this.value)">
                </td>
                <td>NT$${subtotal}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="removeItem(${item.cartId})">移除</button>
                </td>
            </tr>
        `;
    }).join("");

    if (tbody) tbody.innerHTML = rowsHtml;
    const totalEl = document.getElementById("total-price");
    if (totalEl) totalEl.textContent = `NT$${total}`;
}

// 更新數量
function updateQuantity(cartId, newQuantity) {
    newQuantity = parseInt(newQuantity, 10);
    if (isNaN(newQuantity) || newQuantity < 1) return;

    fetch(`/api/cart/update`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cartId, quantity: newQuantity })
    })
        .then(res => {
            if (!res.ok) throw new Error("更新失敗");
            return res.json();
        })
        .then(updatedItem => {
            const index = cartData.findIndex(p => p.cartId === updatedItem.cartId);
            if (index !== -1) {
                cartData[index] = { ...cartData[index], quantity: updatedItem.quantity };
            }
            renderCart();
            refreshTotalsAndBadge();
            refreshCheckoutButtonState();
        })
        .catch(err => console.error("更新數量失敗", err));
}

// ===== 刪除（改成只綁一次確認按鈕，避免重複觸發）=====
let deleteCartId = null;

function removeItem(cartId) {
    deleteCartId = cartId;
    const modalEl = document.getElementById('confirmDeleteModal');
    if (!modalEl) return;
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
}

function wireDeleteConfirmOnce() {
    const btn = document.getElementById("confirmDeleteBtn");
    if (!btn) return;
    if (btn.dataset.bound === "1") return; // 防重複
    btn.dataset.bound = "1";

    btn.addEventListener("click", () => {
        if (deleteCartId == null) return;

        fetch(`/api/cart/item/${deleteCartId}`, { method: "DELETE" })
            .then(res => {
                if (!res.ok) throw new Error("刪除失敗");
                cartData = cartData.filter(p => p.cartId !== deleteCartId);
                renderCart();
                refreshTotalsAndBadge();
                refreshCheckoutButtonState();
            })
            .catch(err => console.error(err))
            .finally(() => {
                const modalEl = document.getElementById('confirmDeleteModal');
                if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
                deleteCartId = null;
            });
    });
}

// 清空購物車
function getCurrentUserId() {
    return window.CURRENT_USER_ID || 1; // TODO 改成你的登入機制
}

const clearBtn = document.getElementById("clear-cart-btn");
if (clearBtn) {
    clearBtn.addEventListener("click", async () => {
        if (!confirm("確定要移除購物車內所有商品嗎？")) return;

        const uid = getCurrentUserId();
        try {
            const res = await fetch(`/api/cart/user/${uid}`, { method: "DELETE" });
            if (!res.ok && res.status !== 204) throw new Error("清空失敗");

            cartData = [];
            renderCart();
            refreshTotalsAndBadge();
            refreshCheckoutButtonState();
        } catch (err) {
            console.error(err);
            alert("清空購物車發生錯誤，請稍後再試");
        }
    });
}

// ===== 徽章 & 總金額 =====
function refreshTotalsAndBadge() {
    // 總金額
    const total = (cartData || []).reduce(
        (sum, it) => sum + (Number(it.price) || 0) * (Number(it.quantity) || 0),
        0
    );
    const totalEl = document.getElementById("total-price");
    if (totalEl) totalEl.textContent = `NT$${total}`;

    // 🔹徽章顯示「品項數」（行數），而不是數量總和
    const count = (cartData || []).length;
    const badge = document.getElementById("cart-badge");
    if (badge) badge.textContent = count;
}

// ===== 去結帳（導到 checkout.html）=====
function wireCheckoutButton() {
    const btn = document.getElementById("checkout-btn") || document.getElementById("go-checkout-btn");
    if (!btn) return;
    btn.addEventListener("click", (e) => {
        e.preventDefault();
        if (!cartData || cartData.length === 0) {
            alert("購物車是空的，無法進入結帳。");
            return;
        }
        // 可選：把 userId / cart 快照暫存，checkout 頁也能用
        sessionStorage.setItem("checkout_user_id", String(userId));
        sessionStorage.setItem("cart_snapshot", JSON.stringify(cartData));

        // 導到結帳頁
        window.location.href = "checkout.html";
    });
}

function refreshCheckoutButtonState() {
    const btn = document.getElementById("checkout-btn") || document.getElementById("go-checkout-btn");
    if (!btn) return;
    const disabled = (!cartData || cartData.length === 0);
    btn.disabled = disabled;
    btn.classList.toggle("disabled", disabled);
}
