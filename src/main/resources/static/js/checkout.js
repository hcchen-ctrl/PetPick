let total = 0;
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// ---- DOM ----
const cartBadgeEl = document.getElementById("cart-badge");
const totalPriceEl = document.getElementById("total-price");
const deliveryMethod = document.getElementById("delivery-method");
const addressField = document.getElementById("address-field");
const addressInput = document.getElementById("address");
const phoneInput = document.getElementById("phone");
const phoneErrEl = document.getElementById("phone-error");

// 超商相關
const cvsBrandWrap = document.getElementById("cvs-brand-wrap");   // 外層（選擇超商）
const cvsOptions = document.getElementById("cvs-brand-options");  // 內層（radio 群組）
const storeInfoEl = document.getElementById("store-info");        // <p id="store-info">
const storeInfoWrap = storeInfoEl ? storeInfoEl.parentElement : null; // 「選擇的超商」整塊（在 HTML 中在 wrap 外面）

// ---- 小工具 ----
const safeText = (x) => (x == null ? "" : String(x));

function setBadge(n) {
    if (cartBadgeEl) cartBadgeEl.textContent = String(n ?? 0);
}
function setTotal(n) {
    if (totalPriceEl) totalPriceEl.textContent = `NT$${n ?? 0}`;
}

// 即時更新「選擇的超商」文案
function updateStoreInfo() {
    const code = document.querySelector('input[name="cvsBrand"]:checked')?.value;
    const map = { UNIMARTC2C: "7-ELEVEN", FAMIC2C: "全家", HILIFEC2C: "萊爾富", OKMARTC2C: "OK" };
    if (storeInfoEl) storeInfoEl.textContent = code ? `已選擇：${map[code] || code}` : "";
}

// 共用：POST 要求 HTML 表單，並做基本驗證
async function postForHtmlForm(url, payload) {
    const resp = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload || {})
    });
    const text = await resp.text();

    if (!resp.ok) {
        // 嘗試把 JSON 錯誤訊息攤出來
        try {
            const j = JSON.parse(text);
            throw new Error(j.message || j.error || text);
        } catch {
            throw new Error(text || `HTTP ${resp.status}`);
        }
    }
    if (!/<form[\s>]/i.test(text)) {
        console.error("[Form DEBUG] 非表單回應：", text.slice(0, 1000));
        throw new Error("伺服器未回傳第三方支付表單（請查看後端日誌）");
    }
    return text;
}

// ---- 初始載入購物車合計 ----
fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`)
    .then((res) => (res.ok ? res.json() : Promise.reject()))
    .then((items) => {
        const list = Array.isArray(items) ? items : [];
        total = list.reduce((sum, it) => sum + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0);
        setTotal(total);
        setBadge(list.length);
    })
    .catch(() => {
        setTotal(0);
        setBadge(0);
    });

// ---- 表單送出：建單 → 分流 ----
document.getElementById("checkout-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = safeText(document.getElementById("name")?.value).trim();
    const phone = safeText(phoneInput?.value).trim();
    const delivery = safeText(deliveryMethod?.value); // store / cvs_cod / address
    const payment = safeText(document.getElementById("payment")?.value);
    const cvsBrand = document.querySelector('input[name="cvsBrand"]:checked')?.value;

    const addr =
        delivery === "address"
            ? safeText(addressInput?.value).trim()
            : delivery === "store"
                ? "到店取貨"
                : "超商取貨付款";

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
        // 1) 建立訂單
        const res = await fetch("/api/orders/checkout", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userId,
                addr,
                receiverName: name,
                receiverPhone: phone,
                shippingType: delivery
            })
        });
        if (!res.ok) throw new Error((await res.text().catch(() => "")) || "訂單建立失敗");
        const order = await res.json();
        const orderId = order?.orderId;
        setBadge(0); // 後端通常會清空購物車，前端一起清零

        // 2) 分流：信用卡
        if (payment === "credit") {
            const html = await postForHtmlForm("/api/pay/ecpay/checkout", {
                orderId,
                origin: window.location.origin
            });
            submitEcpayFormFromHtml(html);
            return;
        }

        // 2) 分流：超商取貨付款 → 選店頁（★ 修正路徑：/api/logistics/cvs/map）
        if (delivery === 'cvs_cod') {
            const html = await postForHtmlForm('/api/logistics/cvs/map', {
                orderId,
                subType: 'FAMIC2C',     // ★ 測試請固定全家
                isCollection: 'N'       // ★ 測試先不代收
            });
            submitEcpayFormFromHtml(html);
            return;
        }

        // 其他：顯示成功 modal
        const link = document.querySelector("#checkoutModal .modal-footer a");
        if (link && orderId) link.href = `order.html?orderId=${orderId}`;
        new bootstrap.Modal(document.getElementById("checkoutModal")).show();
    } catch (err) {
        console.error(err);
        alert(`付款流程發生錯誤：${err?.message || "請稍後再試"}`);
    }
});

// ---- 將 ECPay/物流 回來的 HTML 解析成 form 並自動送出（避免 inline script 觸發 CSP） ----
function submitEcpayFormFromHtml(html) {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, "text/html");
    const srcForm = doc.querySelector("form");
    if (!srcForm) {
        alert("未取得第三方支付表單");
        return;
    }

    const form = document.createElement("form");
    form.method = (srcForm.getAttribute("method") || "post").toLowerCase();
    form.action = srcForm.getAttribute("action") || "";
    form.style.display = "none";

    srcForm.querySelectorAll("input, select, textarea").forEach((el) => {
        const name = el.getAttribute("name");
        if (!name || el.disabled) return;
        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = name;
        hidden.value = el.value ?? "";
        form.appendChild(hidden);
    });

    document.body.appendChild(form);
    form.submit();
}

// ---- 配送方式切換（控制地址 & 超商區塊 & 付款選項）----
deliveryMethod?.addEventListener("change", () => {
    const val = deliveryMethod.value;
    const paymentSelect = document.getElementById("payment");
    const cashOption = document.getElementById("cash-option");
    const codOption = document.getElementById("cod-option");

    if (val === "address") {
        // 宅配
        addressField.style.display = "block";
        addressInput.required = true;

        cashOption.style.display = "none";
        codOption.style.display = "block";
        if (paymentSelect.value === "cash") paymentSelect.value = "credit";
        paymentSelect.disabled = false;

        if (cvsBrandWrap) cvsBrandWrap.style.display = "none";
        if (cvsOptions) cvsOptions.style.display = "none";
        if (storeInfoWrap) storeInfoWrap.style.display = "none";
    } else if (val === "cvs_cod") {
        // 超商取貨付款
        addressField.style.display = "none";
        addressInput.required = false;
        addressInput.value = "";

        cashOption.style.display = "none";
        codOption.style.display = "block";
        paymentSelect.value = "cod";
        paymentSelect.disabled = true;

        if (cvsBrandWrap) cvsBrandWrap.style.display = "block";
        if (cvsOptions) cvsOptions.style.display = "block";
        if (storeInfoWrap) {
            storeInfoWrap.style.display = "block";
            updateStoreInfo();
        }
    } else {
        // 到店取貨
        addressField.style.display = "none";
        addressInput.required = false;
        addressInput.value = "";

        cashOption.style.display = "block";
        codOption.style.display = "none";
        if (paymentSelect.value === "cod") paymentSelect.value = "cash";
        paymentSelect.disabled = false;

        if (cvsBrandWrap) cvsBrandWrap.style.display = "none";
        if (cvsOptions) cvsOptions.style.display = "none";
        if (storeInfoWrap) storeInfoWrap.style.display = "none";
    }
});

// ---- 超商品牌切換 → 立即更新文案 ----
document.querySelectorAll('input[name="cvsBrand"]').forEach((radio) => {
    radio.addEventListener("change", updateStoreInfo);
});

// ---- 手機即時驗證 ----
phoneInput?.addEventListener("input", () => {
    if (!phoneErrEl) return;
    phoneErrEl.style.display =
        /^09\d{8}$/.test(phoneInput.value) || phoneInput.value === "" ? "none" : "block";
});

// ---- 初始化 ----
window.addEventListener("DOMContentLoaded", () => {
    deliveryMethod?.dispatchEvent(new Event("change"));
});
