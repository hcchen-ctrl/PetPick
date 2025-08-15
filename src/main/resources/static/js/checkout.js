// === checkout.js (移除卡號驗證/顯示、選信用卡直跳綠界 + 顯示品項數 badge) ===
let total = 0;

// 嘗試從 sessionStorage 取得 userId（購物車頁可能存的），沒有就先用 1
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// DOM
const backToTopBtn = document.getElementById("backToTop");
const navbar = document.querySelector(".navbar");
const cartBadgeEl = document.getElementById("cart-badge");

// 小工具：更新 badge（顯示品項數）
function setBadge(count) {
    if (cartBadgeEl) cartBadgeEl.textContent = String(count ?? 0);
}

// 取得購物車金額 + 品項數（badge）
fetch(`/api/cart/withProduct/${userId}`)
    .then(res => {
        if (!res.ok) throw new Error("載入購物車失敗");
        return res.json();
    })
    .then(data => {
        const items = Array.isArray(data) ? data : [];
        total = items.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 0), 0);
        document.getElementById("total-price").textContent = `NT$${total}`;
        // ★ 更新 badge：顯示「品項數」
        setBadge(items.length);
    })
    .catch(() => {
        document.getElementById("total-price").textContent = `NT$${total}`;
        // 讀取失敗時，先顯示 0
        setBadge(0);
    });

// 結帳提交事件
document.getElementById("checkout-form").addEventListener("submit", async function (e) {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const delivery = document.getElementById("delivery-method").value;
    const payment = document.getElementById("payment").value;

    // 地址：宅配才需要，其它用標記文字
    const addressInput = document.getElementById("address");
    const addr = (delivery === "address")
        ? (addressInput.value || "").trim()
        : (delivery === "store" ? "到店取貨" : "超商取貨付款");

    // 基本驗證
    if (!/^09\d{8}$/.test(phone)) {
        alert("請輸入正確手機號碼");
        return;
    }
    if (delivery === "address" && !addr) {
        alert("請填寫收件地址");
        return;
    }

    try {
        // 1) 先建立訂單（把購物車轉 orders + order_details）
        const res = await fetch('/api/orders/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, addr, receiverName: name, receiverPhone: phone, shippingType: delivery })
        });
        if (!res.ok) throw new Error(await res.text() || '結帳失敗');
        const order = await res.json(); // 期待含有 orderId

        // 訂單建立成功後，後端通常會清空購物車 → 前端也把 badge 清成 0（避免殘留）
        setBadge(0);

        // 2) 依付款方式分流
        if (payment === 'credit') {
            const resp = await fetch('/api/pay/ecpay/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    orderId: order.orderId,
                    origin: window.location.origin // 讓後端用這個組回傳/導回 URL
                })
            });
            const html = await resp.text();

            // 直接覆蓋當前頁面（或用 _blank 開新視窗亦可）
            const w = window.open('', '_self');
            w.document.open();
            w.document.write(html);
            w.document.close();
            return;
        }
        // 其餘：依你原本頁面邏輯（顯示成功 modal）
        const link = document.querySelector('#checkoutModal .modal-footer a');
        if (link && order?.orderId) {
            link.setAttribute('href', `order.html?orderId=${order.orderId}`);
        }
        const modal = new bootstrap.Modal(document.getElementById("checkoutModal"));
        modal.show();

    } catch (err) {
        console.error(err);
        alert(`結帳失敗：${err.message || '請稍後再試'}`);
    }
});

// ===== 介面：返回頂部 / navbar 顯示隱藏（保留原本邏輯）=====
window.addEventListener("scroll", () => {
    backToTopBtn.style.display = (window.scrollY > 50) ? "flex" : "none";
});
backToTopBtn.addEventListener("click", function () {
    window.scrollTo({ top: 0, behavior: 'smooth' });
});

let lastScrollTop = 0;
window.addEventListener("scroll", function () {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    if (scrollTop > lastScrollTop) {
        navbar.classList.add("hide-navbar");
    } else {
        navbar.classList.remove("hide-navbar");
    }
    lastScrollTop = Math.max(scrollTop, 0);
}, false);

// ===== 配送方式：顯示/隱藏地址；到店時隱藏 COD（保留你原本邏輯）=====
const deliveryMethod = document.getElementById("delivery-method");
const addressField = document.getElementById("address-field");
const addressInput = document.getElementById("address");
const cashOption = document.getElementById("cash-option");

deliveryMethod.addEventListener("change", function () {
    if (this.value === "address") {
        addressField.style.display = "block";
        cashOption.style.display = "none";
        addressInput.setAttribute("required", "required");
    } else {
        addressField.style.display = "none";
        addressInput.removeAttribute("required");
        addressInput.value = "";
    }
});

// 手機即時驗證（保留）
const phoneInput = document.getElementById('phone');
const errorMsg = document.getElementById('phone-error');
phoneInput.addEventListener('input', function () {
    const isValid = /^09\d{8}$/.test(phoneInput.value);
    errorMsg.style.display = (phoneInput.value === '' || isValid) ? 'none' : 'block';
});

// ===== 依配送方式控制 COD 顯示（保留）=====
const deliverySelect = document.getElementById("delivery-method");
const codOption = document.getElementById("cod-option");
deliverySelect.addEventListener("change", function () {
    if (this.value === "store") {
        codOption.style.display = "none";
        const paymentSelect = document.getElementById("payment");
        if (paymentSelect.value === "cod") {
            paymentSelect.value = "credit"; // 到店不支援 COD 的原本行為
        }
    } else {
        codOption.style.display = "block";
    }
});

window.addEventListener("DOMContentLoaded", () => {
    const event = new Event("change");
    deliverySelect.dispatchEvent(event);
});
