
let total = 0;

// 模擬取得購物車資料
fetch('')
    .then(res => res.json())
    .then(data => {
        total = data.reduce((sum, item) => sum + item.price * item.quantity, 0);
        document.getElementById("total-price").textContent = `NT$${total}`;
    });

// 結帳提交事件
document.getElementById("checkout-form").addEventListener("submit", function (e) {
    e.preventDefault();

    // 模擬建立訂單（你可以將這裡改成呼叫後端 API）
    const order = {
        name: document.getElementById("name").value,
        phone: document.getElementById("phone").value,
        address: document.getElementById("address").value,
        payment: document.getElementById("payment").value,
        total: total
    };

    console.log("模擬送出訂單：", order);

    // 顯示模態框（模擬成功）
    const modal = new bootstrap.Modal(document.getElementById("checkoutModal"));
    modal.show();
});
const backToTopBtn = document.getElementById("backToTop");

window.addEventListener("scroll", () => {
    if (window.scrollY > 50) {
        backToTopBtn.style.display = "flex";
    } else {
        backToTopBtn.style.display = "none";
    }
});

backToTopBtn.addEventListener("click", function () {
    window.scrollTo({ top: 0, behavior: 'smooth' });
});

let lastScrollTop = 0;
const navbar = document.querySelector('.navbar');

window.addEventListener("scroll", function () {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

    if (scrollTop > lastScrollTop) {
        // 往下滑
        navbar.classList.add("hide-navbar");
    } else {
        // 往上滑
        navbar.classList.remove("hide-navbar");
    }

    lastScrollTop = scrollTop <= 0 ? 0 : scrollTop; // 防止負值
}, false);


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
const phoneInput = document.getElementById('phone');
const errorMsg = document.getElementById('phone-error');

phoneInput.addEventListener('input', function () {
    const isValid = /^09\d{8}$/.test(phoneInput.value);
    if (phoneInput.value === '') {
        errorMsg.style.display = 'none';
    } else {
        errorMsg.style.display = isValid ? 'none' : 'block';
    }
});
function simulatePayment(event) {
    event.preventDefault();

    const cardNumber = document.getElementById("cardNumber").value;
    const expiry = document.getElementById("cardExpiry").value;
    const cvv = document.getElementById("cardCvv").value;

    // 簡單驗證（可自訂擴充）
    if (!cardNumber || !expiry || !cvv) {
        alert("請填寫完整付款資訊");
        return;
    }

    // 模擬付款成功跳轉
    alert("付款成功！感謝您的購買");
    window.location.href = "order-confirmation.html"; // 假設付款後跳轉的頁面
}
const paymentSelect = document.getElementById("payment");
const creditInfoSection = document.getElementById("credit-info");

paymentSelect.addEventListener("change", function () {
    if (this.value === "credit") {
        creditInfoSection.style.display = "block";
    } else {
        creditInfoSection.style.display = "none";
    }
});

// 初始判斷（如果預設就是信用卡）
if (paymentSelect.value === "credit") {
    creditInfoSection.style.display = "block";
}
const deliverySelect = document.getElementById("delivery-method");
const codOption = document.getElementById("cod-option");

deliverySelect.addEventListener("change", function () {
    if (this.value === "store") {
        codOption.style.display = "none";
        // 如果原本選的是 COD，就自動切換為信用卡
        const paymentSelect = document.getElementById("payment");
        if (paymentSelect.value === "cod") {
            paymentSelect.value = "credit";
        }
    } else {
        codOption.style.display = "block";
    }
});

// 頁面載入時執行一次
window.addEventListener("DOMContentLoaded", () => {
    const event = new Event("change");
    deliverySelect.dispatchEvent(event);
});