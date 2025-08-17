// === checkout.js (信用卡直跳綠界 + 顯示品項數 badge / 修正變數重複與作用域) ===
let total = 0;

// 從購物車頁帶過來；沒有就先用 1
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// ===== DOM =====
const backToTopBtn = document.getElementById("backToTop");
const navbar = document.querySelector(".navbar");
const cartBadgeEl = document.getElementById("cart-badge");
const totalPriceEl = document.getElementById("total-price");

const deliveryMethod = document.getElementById("delivery-method"); // store / cvs_cod / address
const addressField = document.getElementById("address-field");
const addressInput = document.getElementById("address");

const phoneInput = document.getElementById('phone');
const phoneErrEl = document.getElementById('phone-error');

// ===== 工具：更新 badge（顯示品項數）=====
function setBadge(count) {
    if (cartBadgeEl) cartBadgeEl.textContent = String(count ?? 0);
}

// ===== 工具：把後端回來的 HTML 解析成 form 並送出（避免 inline script）=====
function submitEcpayFormFromHtml(html) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const srcForm = doc.querySelector('form');
    if (!srcForm) {
        console.error('未在回應中找到 <form>');
        alert('支付頁面建立失敗（無表單）');
        return;
    }

    const form = document.createElement('form');
    form.method = (srcForm.getAttribute('method') || 'post').toLowerCase();
    form.action = srcForm.getAttribute('action') || '';
    form.style.display = 'none';

    // 複製所有可提交欄位
    const fields = srcForm.querySelectorAll('input, select, textarea');
    fields.forEach(el => {
        const tag = el.tagName.toLowerCase();
        const type = (el.getAttribute('type') || '').toLowerCase();
        const name = el.getAttribute('name');
        if (!name || el.disabled) return;
        if (tag === 'input' && (type === 'button' || type === 'submit' || type === 'reset')) return;

        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = name;
        hidden.value = el.value ?? '';
        form.appendChild(hidden);
    });

    document.body.appendChild(form);
    form.submit();
}

// ===== 取得購物車金額 + 品項數（badge）=====
fetch(`/api/cart/withProduct/${userId}`)
    .then(res => {
        if (!res.ok) throw new Error("載入購物車失敗");
        return res.json();
    })
    .then(items => {
        const list = Array.isArray(items) ? items : [];
        total = list.reduce((sum, it) => sum + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0);
        if (totalPriceEl) totalPriceEl.textContent = `NT$${total}`;
        setBadge(list.length); // 品項數
    })
    .catch(() => {
        if (totalPriceEl) totalPriceEl.textContent = `NT$${total}`;
        setBadge(0);
    });

// ===== 表單送出：建單 → 分流 =====
document.getElementById("checkout-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = phoneInput.value.trim();
    const delivery = deliveryMethod.value;     // store / cvs_cod / address
    const payment = document.getElementById("payment").value; // credit / cash / cod

    const addr = (delivery === "address")
        ? (addressInput.value || "").trim()
        : (delivery === "store" ? "到店取貨" : "超商取貨付款");

    // 基本驗證
    if (!name) {
        alert("請填寫姓名");
        return;
    }
    if (!/^09\d{8}$/.test(phone)) {
        alert("請輸入正確手機號碼");
        return;
    }
    if (delivery === "address" && !addr) {
        alert("請填寫收件地址");
        return;
    }

    try {
        // 1) 建立訂單（購物車 → 訂單/明細）
        const res = await fetch('/api/orders/checkout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId,
                addr,
                receiverName: name,
                receiverPhone: phone,
                shippingType: delivery
            })
        });
        if (!res.ok) throw new Error(await res.text().catch(() => '') || '結帳失敗');
        const order = await res.json(); // 期望有 orderId

        // 後端通常此時已清空購物車 → 前端 badge 也清零，避免殘留
        setBadge(0);

        // 2) 分流
        if (payment === 'credit') {
            // 向後端要綠界的表單 HTML，解析後自動送出
            const resp = await fetch('/api/pay/ecpay/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    orderId: order.orderId,
                    origin: window.location.origin // 讓後端動態組回跳網址（可保留）
                })
            });
            if (!resp.ok) {
                const msg = await resp.text().catch(() => '');
                throw new Error(msg || '建立綠界表單失敗');
            }
            const html = await resp.text();
            submitEcpayFormFromHtml(html);
            return;
        }

        // 其他（到店/宅配COD）：顯示成功 modal 或導訂單頁
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

// ===== 配送方式顯示/隱藏地址；到店時隱藏 COD =====
deliveryMethod.addEventListener("change", function () {
    const paymentSelect = document.getElementById("payment");
    const cashOption = document.getElementById("cash-option");
    const codOption = document.getElementById("cod-option");

    if (this.value === "address") {
        addressField.style.display = "block";
        cashOption.style.display = "none";
        addressInput.setAttribute("required", "required");
        if (paymentSelect.value === "cash") paymentSelect.value = "credit";
        codOption.style.display = "block";
        paymentSelect.disabled = false;
    } else {
        addressField.style.display = "none";
        addressInput.removeAttribute("required");
        addressInput.value = "";

        if (this.value === "cvs_cod") {
            // 超商取貨付款 → 只能 COD
            cashOption.style.display = "none";
            codOption.style.display = "block";
            paymentSelect.value = "cod";
            paymentSelect.disabled = true;
        } else {
            // 到店取貨 → 可到店現金，不允許 COD
            cashOption.style.display = "block";
            codOption.style.display = "none";
            if (paymentSelect.value === "cod") paymentSelect.value = "cash";
            paymentSelect.disabled = false;
        }
    }
});

// 手機即時驗證
phoneInput.addEventListener('input', () => {
    const ok = /^09\d{8}$/.test(phoneInput.value);
    if (phoneErrEl) phoneErrEl.style.display = (phoneInput.value === '' || ok) ? 'none' : 'block';
});

// 返回頂部 / navbar 顯示隱藏
window.addEventListener("scroll", () => {
    if (backToTopBtn) backToTopBtn.style.display = (window.scrollY > 50) ? "flex" : "none";
});
backToTopBtn?.addEventListener("click", () => window.scrollTo({ top: 0, behavior: 'smooth' }));
let lastScrollTop = 0;
window.addEventListener("scroll", () => {
    const st = window.pageYOffset || document.documentElement.scrollTop;
    if (st > lastScrollTop) navbar?.classList.add("hide-navbar"); else navbar?.classList.remove("hide-navbar");
    lastScrollTop = Math.max(st, 0);
}, false);

// 初始化
window.addEventListener("DOMContentLoaded", () => {
    const evt = new Event("change");
    deliveryMethod.dispatchEvent(evt);
});
