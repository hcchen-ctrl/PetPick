let cartData = [];
const userId = 1;

document.addEventListener("DOMContentLoaded", () => {
    loadCart();
});

function loadCart() {
    fetch(`/api/cart/withProduct/${userId}`)
        .then(res => {
            if (!res.ok) throw new Error("載入購物車失敗");
            return res.json();
        })
        .then(data => {
            cartData = data;
            renderCart();
            updateCartBadge();
        })
        .catch(err => console.error(err));
}
function renderCart() {
    const tbody = document.getElementById("cart-items");
    let total = 0;

    const rowsHtml = cartData.map(item => {
        const price = item.price || 0;
        const quantity = item.quantity || 0;
        const subtotal = price * quantity;
        total += subtotal;

        return `
            <tr>
                <td><img src="${item.imageUrl || '#'}" class="cart-img rounded" alt="商品圖" /></td>
                <td>${item.pname || ''}</td>
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

    tbody.innerHTML = rowsHtml;
    document.getElementById("total-price").textContent = `NT$${total}`;
}

function updateQuantity(cartId, newQuantity) {
    newQuantity = parseInt(newQuantity);
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
            updateCartBadge();
        })
        .catch(err => console.error("更新數量失敗", err));
}

let deleteCartId = null;

function removeItem(cartId) {
    deleteCartId = cartId;
    const modal = new bootstrap.Modal(document.getElementById('confirmDeleteModal'));
    modal.show();

    document.getElementById("confirmDeleteBtn").addEventListener("click", () => {
        if (deleteCartId === null) return;
        fetch(`/api/cart/${deleteCartId}`, { method: "DELETE" })
            .then(res => {
                if (!res.ok) throw new Error("刪除失敗");
                cartData = cartData.filter(p => p.cartId !== deleteCartId);
                renderCart();
                updateCartBadge();
            })
            .catch(err => console.error(err))
            .finally(() => {
                bootstrap.Modal.getInstance(document.getElementById('confirmDeleteModal')).hide();
                deleteCartId = null;
            });
    });
}

function updateCartBadge() {
    const count = cartData.length;
    const badge = document.getElementById("cart-badge");
    if (badge) badge.textContent = count;
}
