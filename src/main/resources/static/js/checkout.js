// === checkout.js ===
let total = 0;
const userId = Number(sessionStorage.getItem("checkout_user_id")) || 1;

// Demo 使用者（後端會讀這個 Header）
const DEMO_UID = 1;
const DEMO_HEADERS = { "X-Demo-UserId": String(DEMO_UID) };

// ---- DOM ----
const form = document.getElementById("checkout-form");
const submitBtn = form?.querySelector('button[type="submit"]');

const cartBadgeEl = document.getElementById("cart-badge");
const totalPriceEl = document.getElementById("total-price");

const deliveryMethod = document.getElementById("delivery-method");
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

function setBadge(n) {
  if (cartBadgeEl) cartBadgeEl.textContent = String(n ?? 0);
}
function setTotal(n) {
  if (totalPriceEl) totalPriceEl.textContent = `NT$${(n ?? 0).toLocaleString("zh-Hant-TW")}`;
}
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

// ★ 姓名格式驗證：中文 2~5 或 英文 4~10（不含空白與符號）
function isValidReceiverName(name) {
  if (!name) return false;
  const clean = name.trim().replace(/\s+/g, "").replace(/[^A-Za-z\u4E00-\u9FFF]/g, "");
  const hasCJK = /[\u4E00-\u9FFF]/.test(clean);
  if (hasCJK) return clean.length >= 2 && clean.length <= 5;
  return clean.length >= 4 && clean.length <= 10;
}

// ★ 統一顯示「訂單失敗」的 Modal（若頁面沒有，會動態建立）
function showFailModal(message) {
  let modalEl = document.getElementById("checkoutFailModal");
  let msgEl;
  if (!modalEl) {
    const id = "checkoutFailModal";
    const html = `
      <div class="modal fade" id="${id}" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header bg-danger text-white">
              <h5 class="modal-title">訂單失敗</h5>
              <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body"><div id="failMessage" class="text-danger fw-semibold"></div></div>
            <div class="modal-footer">
              <a href="cart.html" class="btn btn-outline-secondary">回購物車</a>
              <button type="button" class="btn btn-danger" data-bs-dismiss="modal">關閉</button>
            </div>
          </div>
        </div>
      </div>`;
    document.body.insertAdjacentHTML("beforeend", html);
    modalEl = document.getElementById(id);
  }
  msgEl = modalEl.querySelector("#failMessage");
  if (msgEl) msgEl.textContent = message || "付款 / 建單流程發生錯誤，請稍後再試。";
  new bootstrap.Modal(modalEl).show();
}

// ★ 標記訂單為失敗（盡力而為）
// 1) POST /api/orders/{id}/fail  (建議你後端提供這支)
// 2) Fallback: PATCH /api/orders/{id}/status { status:"Failed", note:reason }
async function markOrderFailed(orderId, reason) {
  if (!orderId) return false;
  const payload1 = { reason: reason || "" };
  const payload2 = { status: "Failed", note: reason || "" };

  // 先嘗試專用 fail 端點
  try {
    const r = await fetch(`/api/orders/${encodeURIComponent(orderId)}/fail`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...DEMO_HEADERS },
      credentials: "include",
      body: JSON.stringify(payload1),
    });
    if (r.ok) return true;
  } catch (_) {}

  // 回退：一般狀態更新
  try {
    const r = await fetch(`/api/orders/${encodeURIComponent(orderId)}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...DEMO_HEADERS },
      credentials: "include",
      body: JSON.stringify(payload2),
    });
    return r.ok;
  } catch (_) {
    return false;
  }
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

// ---- 表單送出：建單 → 分流（任何錯誤都會標記訂單失敗 + 顯示失敗 Modal）----
form?.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (submitBtn) submitBtn.disabled = true;

  let createdOrderId = null; // ★ 用來在任何錯誤時標記失敗
  try {
    const name = safeText(document.getElementById("name")?.value).trim();
    const phone = safeText(phoneInput?.value).trim();
    const delivery = safeText(deliveryMethod?.value);  // address | cvs_cod
    const payment = safeText(document.getElementById("payment")?.value); // credit | cod

    const effectivePayment = delivery === "cvs_cod" ? "cod" : (payment || "").toLowerCase();
    sessionStorage.setItem("last_payment", effectivePayment);

    const addr = delivery === "address" ? safeText(addressInput?.value).trim() : "超商取貨付款";
    const zip = delivery === "address" ? safeText(zipInput?.value).trim() : "";

    // 基本驗證
    if (!name) throw new Error("請填寫姓名");
    if (!isValidReceiverName(name)) throw new Error("姓名格式不符：中文 2~5、英文 4~10（不含空白與符號）");
    if (!/^09\d{8}$/.test(phone)) throw new Error("請輸入正確手機號碼");
    if (delivery === "address") {
      if (!addr || addr.length < 3) throw new Error("請填寫正確收件地址");
      if (zip && !/^\d{3,5}$/.test(zip)) throw new Error("郵遞區號格式不正確");
    }

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
    createdOrderId = orderId; // ★ 之後任一步錯誤都會標失敗

    // 2) 分流
    // 2-1) 超商取貨付款（固定全家 FAMIC2C 測試）
    if (delivery === "cvs_cod") {
      const html = await postForHtmlForm("/api/logistics/cvs/map", {
        orderId,
        subType: "FAMIC2C",
        isCollection: "N"
      });
      submitEcpayFormFromHtml(html);
      return; // 進第三方流程
    }

    // 2-2) 宅配 + 信用卡 → 綠界金流
    if (delivery === "address" && effectivePayment === "credit") {
      const html = await postForHtmlForm("/api/pay/ecpay/checkout", {
        orderId,
        origin: window.location.origin
      });
      submitEcpayFormFromHtml(html);
      return; // 進第三方流程
    }

    // 2-3) 宅配 + 貨到付款 → 建綠界宅配託運單
    if (delivery === "address" && effectivePayment === "cod") {
      try {
        const j = await postJson("/api/logistics/home/ecpay/create", {
          orderId,
          receiverName: name,
          receiverPhone: phone,
          receiverZip: zip || null,
          receiverAddr: addr,
          isCollection: true
        });
        showToast(`已建立宅配託運單：${j.trackingNo || j.logisticsId || "已送出"}`, "success");
      } catch (e) {
        // ★ 只要宅配建單失敗 → 標記訂單失敗 + 失敗 Modal
        await markOrderFailed(createdOrderId, e.message);
        showFailModal(`宅配建單失敗：${e.message}`);
        return;
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
    if (link && createdOrderId) link.href = `order.html?orderId=${createdOrderId}`;
    new bootstrap.Modal(document.getElementById("checkoutModal")).show();

  } catch (err) {
    console.error(err);
    // ★ 只要在建立完訂單之後發生任何錯誤，就標記為失敗
    if (createdOrderId) {
      await markOrderFailed(createdOrderId, err?.message || "Checkout Error");
      showFailModal(err?.message || "付款 / 建單流程發生錯誤，請稍後再試。");
    } else {
      // 連訂單都沒建成功，就直接彈失敗 Modal
      showFailModal(err?.message || "訂單建立失敗，請稍後再試。");
    }
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