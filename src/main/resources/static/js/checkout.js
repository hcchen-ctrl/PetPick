// === checkout.js ===
let total = 0;
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// 方案 A：開發用 Demo 使用者（後端 OrderCommandController 會讀取這個 Header）
const DEMO_UID = 1;
const DEMO_HEADERS = { "X-Demo-UserId": String(DEMO_UID) };

// ---- DOM ----
const form = document.getElementById("checkout-form");
const submitBtn = form?.querySelector('button[type="submit"]');

const cartBadgeEl = document.getElementById("cart-badge");
const totalPriceEl = document.getElementById("total-price");

const deliveryMethod = document.getElementById("delivery-method");
const nameInput = document.getElementById("name");
const nameErrEl = document.getElementById("name-error"); // 若頁面有此提示元素會自動顯示
const phoneInput = document.getElementById("phone");
const phoneErrEl = document.getElementById("phone-error");

// 宅配欄位
const addressField = document.getElementById("address-field");
const addressInput = document.getElementById("address");
const zipField = document.getElementById("zip-field");
const zipInput = document.getElementById("receiver-zip");

// 超商相關（cvs_cod）
const cvsBrandWrap = document.getElementById("cvs-brand-wrap");
const cvsOptions = document.getElementById("cvs-brand-options");
const storeInfoEl = document.getElementById("store-info");
const storeInfoWrap = storeInfoEl ? storeInfoEl.parentElement : null;

// ---- 小工具 ----
const safeText = (x) => (x == null ? "" : String(x));
function setBadge(n) { if (cartBadgeEl) cartBadgeEl.textContent = String(n ?? 0); }
function setTotal(n) { if (totalPriceEl) totalPriceEl.textContent = `NT$${(n ?? 0).toLocaleString("zh-Hant-TW")}`; }
function showToast(message, type = "primary") {
  const id = "t" + Math.random().toString(36).slice(2);
  const html = `
   <div id="${id}" class="toast align-items-center text-bg-${type} border-0 position-fixed top-0 end-0 m-3"
        role="alert" aria-live="assertive" aria-atomic="true" style="z-index:2000;">
     <div class="d-flex">
       <div class="toast-body">${message}</div>
       <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
     </div>
   </div>`;
  document.body.insertAdjacentHTML("beforeend", html);
  const el = document.getElementById(id);
  new bootstrap.Toast(el, { delay: 2800 }).show();
  el.addEventListener("hidden.bs.toast", () => el.remove());
}

// ---- 姓名規則（與後端一致） ----
// 中文 2~5 或 英文 4~10（不含空白與符號）
function isValidReceiverName(name) {
  if (!name) return false;
  const clean = name.trim().replace(/\s+/g, "").replace(/[^A-Za-z\u4E00-\u9FFF]/g, "");
  const hasCJK = /[\u4E00-\u9FFF]/.test(clean);
  if (hasCJK) return clean.length >= 2 && clean.length <= 5;
  return clean.length >= 4 && clean.length <= 10;
}
// 送綠界前，將姓名做相同清洗（避免被拒）
function sanitizeNameForEcpay(name) {
  return (name || "").trim().replace(/\s+/g, "").replace(/[^A-Za-z\u4E00-\u9FFF]/g, "");
}
function setInvalid(el, show) {
  if (!el) return;
  el.classList[show ? "add" : "remove"]("is-invalid");
  // 若頁面有對應的錯誤提示元素，順便切換顯示
  if (el === nameInput && nameErrEl) nameErrEl.style.display = show ? "block" : "none";
  if (el === phoneInput && phoneErrEl) phoneErrEl.style.display = show ? "block" : "none";
}

// 解析第三方回傳 HTML → 自動提交
function submitEcpayFormFromHtml(html) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const srcForm = doc.querySelector("form");
  if (!srcForm) { showToast("未取得第三方表單", "danger"); return; }

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

// 共用：POST 期望回 HTML form 的端點（加 DEMO_HEADERS）
async function postForHtmlForm(url, payload) {
  const resp = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...DEMO_HEADERS },
    credentials: "include",
    body: JSON.stringify(payload || {})
  });
  const text = await resp.text();
  if (!resp.ok) {
    try {
      const j = JSON.parse(text);
      throw new Error(j.message || j.error || text);
    } catch {
      throw new Error(text || `HTTP ${resp.status}`);
    }
  }
  if (!/<form[\s>]/i.test(text)) throw new Error("伺服器未回傳第三方支付表單");
  return text;
}

// 共用：POST JSON 並把錯誤訊息抽出
async function postJson(url, body) {
  const resp = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...DEMO_HEADERS },
    credentials: "include",
    body: JSON.stringify(body || {})
  });
  const text = await resp.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { raw: text }; }
  if (!resp.ok) {
    const msg = data.message || data.error || text || `HTTP ${resp.status}`;
    throw new Error(msg);
  }
  return data;
}

// ---- 初始載入購物車合計 ----
fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`, {
  credentials: "include",
  headers: { ...DEMO_HEADERS }
})
  .then((res) => (res.ok ? res.json() : Promise.reject()))
  .then((items) => {
    const list = Array.isArray(items) ? items : [];
    total = list.reduce((sum, it) => sum + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0);
    setTotal(total);
    setBadge(list.length);
  })
  .catch(() => { setTotal(0); setBadge(0); });

// ---- 表單送出：建單 → 分流 ----
form?.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (submitBtn) submitBtn.disabled = true;

  const name = safeText(nameInput?.value).trim();
  const phone = safeText(phoneInput?.value).trim();
  const delivery = safeText(deliveryMethod?.value);  // address | cvs_cod
  const payment = safeText(document.getElementById("payment")?.value); // credit | cod

  const effectivePayment = delivery === "cvs_cod" ? "cod" : (payment || "").toLowerCase();
  sessionStorage.setItem("last_payment", effectivePayment);

  const addr = delivery === "address" ? safeText(addressInput?.value).trim() : "超商取貨付款";
  const zip = delivery === "address" ? safeText(zipInput?.value).trim() : "";

  // 基本驗證
  if (!name) {
    showToast("請填寫姓名", "danger");
    setInvalid(nameInput, true);
    if (submitBtn) submitBtn.disabled = false;
    return;
  }

  // ★ 姓名即時規則驗證（與後端一致）
  if (!isValidReceiverName(name)) {
    showToast("姓名格式不符：中文 2~5、英文 4~10（不含空白與符號）", "danger");
    setInvalid(nameInput, true);
    if (submitBtn) submitBtn.disabled = false;
    return;
  } else {
    setInvalid(nameInput, false);
  }

  if (!/^09\d{8}$/.test(phone)) {
    showToast("請輸入正確手機號碼", "danger");
    setInvalid(phoneInput, true);
    if (submitBtn) submitBtn.disabled = false;
    return;
  } else {
    setInvalid(phoneInput, false);
  }

  if (delivery === "address") {
    if (!addr || addr.length < 3) {
      showToast("請填寫正確收件地址", "danger");
      if (submitBtn) submitBtn.disabled = false;
      return;
    }
    if (zip && !/^\d{3,5}$/.test(zip)) {
      showToast("郵遞區號格式不正確", "danger");
      if (submitBtn) submitBtn.disabled = false;
      return;
    }
  }

  try {
    // 1) 建立訂單（後端以 Header 判斷 userId）
    const order = await postJson("/api/orders/checkout", {
      addr,
      receiverZip: zip || null,
      receiverName: name,
      receiverPhone: phone,
      shippingType: delivery
    });
    const orderId = order?.orderId;
    if (!orderId) throw new Error("訂單建立失敗（缺少 orderId）");

    // 2) 分流
    // 2-1) 超商取貨付款（固定全家 FAMIC2C 測試）
    if (delivery === "cvs_cod") {
      const html = await postForHtmlForm("/api/logistics/cvs/map", {
        orderId,
        subType: "FAMIC2C",
        isCollection: "N"
      });
      submitEcpayFormFromHtml(html);
      return;
    }

    // 2-2) 宅配 + 信用卡 → 綠界金流
    if (delivery === "address" && effectivePayment === "credit") {
      const html = await postForHtmlForm("/api/pay/ecpay/checkout", {
        orderId,
        origin: window.location.origin
      });
      submitEcpayFormFromHtml(html);
      return;
    }

    // 2-3) 宅配 + 貨到付款 → 建綠界宅配託運單（姓名改用清洗後版本）
    if (delivery === "address" && effectivePayment === "cod") {
      try {
        const cleanName = sanitizeNameForEcpay(name);
        const j = await postJson("/api/logistics/home/ecpay/create", {
          orderId,
          receiverName: cleanName,
          receiverPhone: phone,
          receiverZip: zip || null,
          receiverAddr: addr,
          isCollection: true
        });
        showToast(`已建立宅配託運單：${j.trackingNo || j.logisticsId || "已送出"}`, "success");
      } catch (e) {
        console.error(e);
        showToast(`宅配建單失敗：${e.message}`, "danger");
      }

      // 清空徽章（後端通常也清了，前端同步一下顯示）
      await clearCartOnLocalPayment(userId);
      await refreshCartBadge(userId);

      // 彈成功視窗
      const link = document.querySelector("#checkoutModal .modal-footer a");
      if (link && orderId) link.href = `order.html?orderId=${orderId}`;
      new bootstrap.Modal(document.getElementById("checkoutModal")).show();
      return;
    }

    // 其他（理論上不會進到）
    const link = document.querySelector("#checkoutModal .modal-footer a");
    if (link && orderId) link.href = `order.html?orderId=${orderId}`;
    new bootstrap.Modal(document.getElementById("checkoutModal")).show();

  } catch (err) {
    console.error(err);
    showToast(`付款流程發生錯誤：${err?.message || "請稍後再試"}`, "danger");
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
});

// ---- 配送方式切換（address / cvs_cod）----
deliveryMethod?.addEventListener("change", () => {
  const val = deliveryMethod.value; // address | cvs_cod
  const paymentSelect = document.getElementById("payment");
  const codOption = document.getElementById("cod-option");

  if (val === "address") {
    // 顯示宅配欄位
    zipField.style.display = "block";
    addressField.style.display = "block";
    addressInput.required = true;

    // 支付：允許信用卡與貨到付款
    codOption.style.display = "block";
    if (paymentSelect.value !== "credit" && paymentSelect.value !== "cod") {
      paymentSelect.value = "credit";
    }
    paymentSelect.disabled = false;

    // 隱藏超商區塊
    if (cvsBrandWrap) cvsBrandWrap.style.display = "none";
    if (cvsOptions) cvsOptions.style.display = "none";
    if (storeInfoWrap) storeInfoWrap.style.display = "none";
  } else if (val === "cvs_cod") {
    // 隱藏宅配欄位
    zipField.style.display = "none";
    addressField.style.display = "none";
    addressInput.required = false;
    addressInput.value = "";
    zipInput.value = "";

    // 支付：固定 cod
    codOption.style.display = "block";
    paymentSelect.value = "cod";
    paymentSelect.disabled = true;

    // 顯示超商區塊
    if (cvsBrandWrap) cvsBrandWrap.style.display = "block";
    if (cvsOptions) cvsOptions.style.display = "block";
    if (storeInfoWrap) {
      storeInfoWrap.style.display = "block";
      updateStoreInfo();
    }
  }
  const v = val === "cvs_cod" ? "cod" : (paymentSelect.value || "").toLowerCase();
  sessionStorage.setItem("last_payment", v);
});

// 即時更新「選擇的超商」文案（僅 cvs_cod 用）
function updateStoreInfo() {
  const code = document.querySelector('input[name="cvsBrand"]:checked')?.value;
  const map = { UNIMARTC2C: "7-ELEVEN", FAMIC2C: "全家", HILIFEC2C: "萊爾富", OKMARTC2C: "OK" };
  if (storeInfoEl) storeInfoEl.textContent = code ? `已選擇：${map[code] || code}` : "";
}
document.querySelectorAll('input[name="cvsBrand"]').forEach((radio) => {
  radio.addEventListener("change", updateStoreInfo);
});

// 手機即時驗證（只允許數字）
phoneInput?.addEventListener("input", () => {
  phoneInput.value = phoneInput.value.replace(/\D+/g, "").slice(0, 10);
  if (!phoneErrEl) return;
  phoneErrEl.style.display =
    /^09\d{8}$/.test(phoneInput.value) || phoneInput.value === "" ? "none" : "block";
});

// 姓名即時驗證
nameInput?.addEventListener("input", () => {
  const ok = isValidReceiverName(nameInput.value);
  setInvalid(nameInput, !ok && nameInput.value.trim() !== "");
});

// Back-to-top
const backToTop = document.getElementById("backToTop");
window.addEventListener("scroll", () => {
  backToTop.style.display = window.scrollY > 600 ? "block" : "none";
});
backToTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));

// 初始化
window.addEventListener("DOMContentLoaded", () => {
  deliveryMethod?.dispatchEvent(new Event("change"));
});

// ---- 購物車徽章相關（只打現存 API，帶 DEMO_HEADERS）----
async function clearCartOnLocalPayment(userId) {
  try {
    await fetch(`/api/cart/clear/${encodeURIComponent(userId)}`, {
      method: "DELETE",
      credentials: "include",
      headers: { ...DEMO_HEADERS }
    });
  } catch {}
}
async function refreshCartBadge(userId) {
  const setBadgeSafe = (n) => { const el = document.getElementById("cart-badge"); if (el) el.textContent = String(n ?? 0); };
  try {
    const r = await fetch(`/api/cart/withProduct/${encodeURIComponent(userId)}`, {
      credentials: "include",
      headers: { ...DEMO_HEADERS }
    });
    const items = r.ok ? await r.json() : [];
    setBadgeSafe(Array.isArray(items) ? items.length : 0);
  } catch { setBadgeSafe(0); }
}