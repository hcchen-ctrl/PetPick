let total = 0;
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;
const cartBadgeEl = document.getElementById("cart-badge");
const totalPriceEl = document.getElementById("total-price");
const deliveryMethod = document.getElementById("delivery-method");
const addressField = document.getElementById("address-field");
const addressInput = document.getElementById("address");
const phoneInput = document.getElementById('phone');
const phoneErrEl = document.getElementById('phone-error');

// 顯示購物車資料
fetch(`/api/cart/withProduct/${userId}`)
    .then(res => res.json())
    .then(items => {
        const list = Array.isArray(items) ? items : [];
        total = list.reduce((sum, it) => sum + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0);
        totalPriceEl.textContent = `NT$${total}`;
        cartBadgeEl.textContent = list.length;
    }).catch(() => {
        totalPriceEl.textContent = "NT$0";
        cartBadgeEl.textContent = "0";
    });

// 表單送出：建單 → 分流付款方式
document.getElementById("checkout-form").addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = phoneInput.value.trim();
    const delivery = deliveryMethod.value;
    const payment = document.getElementById("payment").value;
    const cvsBrand = document.querySelector('input[name="cvsBrand"]:checked')?.value;
    const addr = (delivery === "address")
        ? addressInput.value.trim()
        : (delivery === "store" ? "到店取貨" : "超商取貨付款");

    if (!name || !/^09\d{8}$/.test(phone) || (delivery === "address" && !addr)) {
        alert("請確認姓名、手機與地址（若需要）皆正確填寫");
        return;
    }

    try {
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

        if (!res.ok) throw new Error("訂單建立失敗");
        const order = await res.json();
        const orderId = order?.orderId;
        cartBadgeEl.textContent = "0";

        // 信用卡付款
        if (payment === 'credit') {
            const payRes = await fetch('/api/pay/ecpay/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId, origin: window.location.origin })
            });
            const html = await payRes.text();
            submitEcpayFormFromHtml(html);
            return;
        }

        // 超商取貨付款 → 跳轉選店頁
        if (delivery === 'cvs_cod') {
            const mapRes = await fetch('/api/logistics/cvs/map', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    orderId,
                    cvsType: cvsBrand || "UNIMARTC2C", // fallback to 7-11
                    isCollection: "Y"
                })
            });
            const html = await mapRes.text();
            submitEcpayFormFromHtml(html);
            return;
        }

        // 到店 / 宅配：顯示成功 modal
        const link = document.querySelector('#checkoutModal .modal-footer a');
        if (link && orderId) link.href = `order.html?orderId=${orderId}`;
        new bootstrap.Modal(document.getElementById("checkoutModal")).show();

    } catch (err) {
        console.error(err);
        alert("付款流程發生錯誤，請稍後再試");
    }
});

// 將綠界回傳的 HTML 表單解析並自動送出
function submitEcpayFormFromHtml(html) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const srcForm = doc.querySelector('form');
    if (!srcForm) return alert("未取得綠界表單");

    const form = document.createElement("form");
    form.method = srcForm.method || 'post';
    form.action = srcForm.action || '';
    form.style.display = 'none';

    srcForm.querySelectorAll("input").forEach(el => {
        if (!el.name || el.disabled) return;
        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = el.name;
        hidden.value = el.value;
        form.appendChild(hidden);
    });

    document.body.appendChild(form);
    form.submit();
}

// 配送方式切換
deliveryMethod.addEventListener("change", () => {
    const val = deliveryMethod.value;
    const paymentSelect = document.getElementById("payment");
    const cashOption = document.getElementById("cash-option");
    const codOption = document.getElementById("cod-option");
    const cvsOptions = document.getElementById("cvs-brand-options");

    if (val === "address") {
        addressField.style.display = "block";
        cashOption.style.display = "none";
        codOption.style.display = "block";
        cvsOptions.style.display = "none";
        addressInput.required = true;
        paymentSelect.disabled = false;
        if (paymentSelect.value === "cash") paymentSelect.value = "credit";
    } else if (val === "cvs_cod") {
        addressField.style.display = "none";
        cashOption.style.display = "none";
        codOption.style.display = "block";
        cvsOptions.style.display = "block";
        paymentSelect.value = "cod";
        paymentSelect.disabled = true;
    } else {
        addressField.style.display = "none";
        cashOption.style.display = "block";
        codOption.style.display = "none";
        cvsOptions.style.display = "none";
        addressInput.required = false;
        paymentSelect.disabled = false;
        if (paymentSelect.value === "cod") paymentSelect.value = "cash";
    }
});

// 手機即時驗證
phoneInput.addEventListener('input', () => {
    phoneErrEl.style.display = (/^09\d{8}$/.test(phoneInput.value) || phoneInput.value === "") ? 'none' : 'block';
});

// 初始化
window.addEventListener("DOMContentLoaded", () => {
    deliveryMethod.dispatchEvent(new Event("change"));
});
