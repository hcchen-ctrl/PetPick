// ====== Config ======
const API_BASE = "/api/products";
const DEFAULT_IMG = "https://via.placeholder.com/300x200?text=No+Image";

// ====== DOM ======
const tbody = document.getElementById("product-table-body");
const keyword = document.getElementById("keyword");
const btnNew = document.getElementById("btnNew");
const toastContainer = document.getElementById("toastContainer");
const loadingMask = document.getElementById("loadingMask");

// Modals / Forms
const imgModal = new bootstrap.Modal(document.getElementById("imageModal"));
const modalImgEl = document.getElementById("modal-image");

const productModalEl = document.getElementById("productModal");
const productModal = new bootstrap.Modal(productModalEl);
const productForm = document.getElementById("productForm");
const productModalTitle = document.getElementById("productModalTitle");
const productId = document.getElementById("productId");
const productName = document.getElementById("productName");
const productDescription = document.getElementById("productDescription");
const productPrice = document.getElementById("productPrice");
const productStock = document.getElementById("productStock");
const productImage = document.getElementById("productImage");
const imagePreview = document.getElementById("imagePreview");
const productActive = document.getElementById("productActive");

const confirmDeleteModal = new bootstrap.Modal(document.getElementById("confirmDeleteModal"));
const delId = document.getElementById("delId");
const delName = document.getElementById("delName");
const btnDeleteDo = document.getElementById("btnDeleteDo");

// ====== State ======
let allProducts = [];
let filtered = [];
let deletingId = null;

// ====== Utils ======
function showLoading(v) {
  loadingMask.style.display = v ? "flex" : "none";
}
function toast(msg, type = "success") {
  const id = "t" + Math.random().toString(36).slice(2);
  const html = `
    <div id="${id}" class="toast align-items-center text-bg-${type} border-0 mb-2" role="alert" aria-live="assertive" aria-atomic="true">
      <div class="d-flex">
        <div class="toast-body">${msg}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>
    </div>`;
  toastContainer.insertAdjacentHTML("beforeend", html);
  const el = document.getElementById(id);
  new bootstrap.Toast(el, { delay: 2400 }).show();
  el.addEventListener("hidden.bs.toast", () => el.remove());
}
function esc(s) {
  return String(s ?? "").replaceAll("&","&amp;").replaceAll("<","&lt;")
    .replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#039;");
}
function money(n) {
  n = Number(n || 0);
  return "NT$" + n.toLocaleString("zh-Hant-TW");
}
function pickName(p) { return p.pname ?? p.name ?? ""; }
function pickId(p) { return p.productId ?? p.id; }
function pickActive(p) {
  if (typeof p.active === "boolean") return p.active;
  if (typeof p.isActive === "boolean") return p.isActive;
  // 若後端用字串 status，也嘗試判斷
  if (typeof p.status === "string") return !/下架|停售|inactive|off/i.test(p.status);
  return true; // 預設視為上架
}
function badgeActive(active) {
  return active
    ? `<span class="badge bg-success">上架中</span>`
    : `<span class="badge bg-secondary">已下架</span>`;
}
function toggleBtnHtml(active) {
  return active
    ? `<button class="btn btn-sm btn-outline-secondary js-toggle">下架</button>`
    : `<button class="btn btn-sm btn-outline-success js-toggle">上架</button>`;
}

// ====== Data ======
async function loadProducts() {
  showLoading(true);
  try {
    const res = await fetch(API_BASE);
    if (!res.ok) throw new Error(`讀取失敗（${res.status}）`);
    allProducts = await res.json() || [];
    applyFilter();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">載入失敗：${esc(err.message)}</td></tr>`;
  } finally {
    showLoading(false);
  }
}

function applyFilter() {
  const kw = (keyword.value || "").trim().toLowerCase();
  filtered = (allProducts || []).filter(p => {
    const name = pickName(p);
    const desc = p.description ?? "";
    return !kw || name.toLowerCase().includes(kw) || desc.toLowerCase().includes(kw);
  });
  renderTable();
}

function renderTable() {
  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">沒有符合條件的商品</td></tr>`;
    return;
  }
  // 以 id 由新到舊
  filtered.sort((a,b) => (pickId(b) ?? 0) - (pickId(a) ?? 0));

  tbody.innerHTML = filtered.map(p => {
    const id = pickId(p);
    const name = pickName(p);
    const desc = p.description ?? "";
    const price = p.price ?? 0;
    const stock = p.stock ?? 0;
    const img = p.imageUrl || DEFAULT_IMG;
    const active = pickActive(p);

    return `
      <tr data-id="${esc(id)}" data-active="${active ? "1" : "0"}">
        <td class="font-monospace">#${esc(id)}</td>
        <td class="fw-semibold">${esc(name)}</td>
        <td>${esc(desc)}</td>
        <td>${money(price)}</td>
        <td>${esc(stock)}</td>
        <td>
          <a href="#" class="text-primary text-decoration-underline js-img" data-url="${esc(img)}">附圖</a>
        </td>
        <td class="td-active">${badgeActive(active)}</td>
        <td>
          ${toggleBtnHtml(active)}
          <button class="btn btn-sm btn-warning js-edit">編輯</button>
          <button class="btn btn-sm btn-danger js-del">刪除</button>
        </td>
      </tr>`;
  }).join("");

  // 綁定事件
  tbody.querySelectorAll(".js-img").forEach(a => {
    a.addEventListener("click", e => {
      e.preventDefault();
      modalImgEl.src = a.dataset.url || DEFAULT_IMG;
      imgModal.show();
    });
  });
  tbody.querySelectorAll(".js-edit").forEach(btn => {
    btn.addEventListener("click", onEditClick);
  });
  tbody.querySelectorAll(".js-del").forEach(btn => {
    btn.addEventListener("click", onDeleteClick);
  });
  tbody.querySelectorAll(".js-toggle").forEach(btn => {
    btn.addEventListener("click", onToggleActive);
  });
}

// ====== CRUD Handlers ======
btnNew.addEventListener("click", () => openCreateModal());
keyword.addEventListener("input", () => applyFilter());

productImage.addEventListener("input", () => {
  const url = productImage.value.trim();
  if (url) {
    imagePreview.src = url;
    imagePreview.classList.remove("d-none");
  } else {
    imagePreview.classList.add("d-none");
    imagePreview.removeAttribute("src");
  }
});

productForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  // 簡單驗證
  const name = productName.value.trim();
  const desc = productDescription.value.trim();
  const price = Number(productPrice.value);
  const stock = Number(productStock.value);
  const imageUrl = (productImage.value || "").trim();
  const active = !!productActive.checked;

  // 套用 Bootstrap 驗證外觀
  productForm.classList.add("was-validated");
  if (!name || name.length > 50 || !desc || desc.length > 300 || !(price >= 0) || !(Number.isInteger(stock) && stock >= 0)) {
    return;
  }

  const id = productId.value ? Number(productId.value) : null;
  const payload = {
    productId: id ?? undefined,
    id: id ?? undefined,
    pname: name,
    name: name,
    description: desc,
    price: Math.round(price),
    stock: Math.round(stock),
    imageUrl: imageUrl || DEFAULT_IMG,
    active: active,           // ★ 帶上 active
    isActive: active          // ★ 兼容另一種命名
  };

  const url = id ? `${API_BASE}/${id}` : API_BASE;
  const method = id ? "PUT" : "POST";

  showLoading(true);
  try {
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error(`儲存失敗（${res.status}）`);

    await loadProducts();
    productModal.hide();
    productForm.reset();
    productForm.classList.remove("was-validated");
    imagePreview.classList.add("d-none");
    toast(id ? "更新成功" : "新增成功", "success");
  } catch (err) {
    toast(err.message || "儲存失敗", "danger");
  } finally {
    showLoading(false);
  }
});

async function onEditClick(e) {
  const tr = e.currentTarget.closest("tr");
  const id = Number(tr?.dataset?.id);
  if (!id) return;

  showLoading(true);
  try {
    const res = await fetch(`${API_BASE}/${id}`);
    if (!res.ok) throw new Error(`讀取商品失敗（${res.status}）`);
    const p = await res.json();

    productModalTitle.textContent = "編輯商品";
    productId.value = pickId(p);
    productName.value = pickName(p);
    productDescription.value = p.description ?? "";
    productPrice.value = p.price ?? 0;
    productStock.value = p.stock ?? 0;
    productImage.value = p.imageUrl || "";
    const active = pickActive(p);
    productActive.checked = !!active;

    if (p.imageUrl) {
      imagePreview.src = p.imageUrl;
      imagePreview.classList.remove("d-none");
    } else {
      imagePreview.classList.add("d-none");
      imagePreview.removeAttribute("src");
    }
    productForm.classList.remove("was-validated");
    productModal.show();
  } catch (err) {
    toast(err.message || "讀取商品失敗", "danger");
  } finally {
    showLoading(false);
  }
}

function onDeleteClick(e) {
  const tr = e.currentTarget.closest("tr");
  const id = Number(tr?.dataset?.id);
  if (!id) return;
  const name = tr?.children?.[1]?.textContent ?? "";
  deletingId = id;
  delId.textContent = id;
  delName.textContent = name;
  confirmDeleteModal.show();
}

btnDeleteDo.addEventListener("click", async () => {
  if (!deletingId) return;
  showLoading(true);
  try {
    const res = await fetch(`${API_BASE}/${deletingId}`, { method: "DELETE" });
    if (!res.ok) throw new Error(`刪除失敗（${res.status}）`);
    // 從快取移除並刷新渲染
    allProducts = allProducts.filter(p => pickId(p) !== deletingId);
    applyFilter();
    toast("刪除成功", "success");
  } catch (err) {
    toast(err.message || "刪除失敗", "danger");
  } finally {
    showLoading(false);
    confirmDeleteModal.hide();
    deletingId = null;
  }
});

// ====== Toggle Active ======
async function onToggleActive(e) {
  const tr = e.currentTarget.closest("tr");
  const id = Number(tr?.dataset?.id);
  if (!id) return;

  const currentActive = tr.dataset.active === "1";
  const nextActive = !currentActive;

  showLoading(true);
  try {
    // 1) 嘗試使用後端的 PATCH /{id}/active?active=true|false
    let ok = false;
    try {
      const r = await fetch(`${API_BASE}/${id}/active?active=${nextActive}`, { method: "PATCH" });
      ok = r.ok;
    } catch {}

    // 2) 回退：抓一筆 → 改 active → PUT
    if (!ok) {
      const res = await fetch(`${API_BASE}/${id}`);
      if (!res.ok) throw new Error(`讀取商品失敗（${res.status}）`);
      const p = await res.json();
      p.active = nextActive;
      p.isActive = nextActive;
      p.name = p.name ?? p.pname ?? ""; // 相容
      p.pname = p.name;
      const res2 = await fetch(`${API_BASE}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(p)
      });
      if (!res2.ok) throw new Error(`更新失敗（${res2.status}）`);
    }

    // 更新前端 UI（不重整）
    tr.dataset.active = nextActive ? "1" : "0";
    tr.querySelector(".td-active").innerHTML = badgeActive(nextActive);
    const btnCell = tr.querySelector("td:last-child");
    const editBtn = btnCell.querySelector(".js-edit");
    const delBtn = btnCell.querySelector(".js-del");
    btnCell.innerHTML = `${toggleBtnHtml(nextActive)} ${editBtn.outerHTML} ${delBtn.outerHTML}`;
    // 重新綁定事件
    btnCell.querySelector(".js-toggle").addEventListener("click", onToggleActive);
    btnCell.querySelector(".js-edit").addEventListener("click", onEditClick);
    btnCell.querySelector(".js-del").addEventListener("click", onDeleteClick);

    // 同步快取
    allProducts = allProducts.map(p => (pickId(p) === id ? { ...p, active: nextActive, isActive: nextActive } : p));

    toast(nextActive ? "已上架" : "已下架", "success");
  } catch (err) {
    toast(err.message || "操作失敗", "danger");
  } finally {
    showLoading(false);
  }
}

// ====== Open Create ======
function openCreateModal() {
  productModalTitle.textContent = "新增商品";
  productId.value = "";
  productName.value = "";
  productDescription.value = "";
  productPrice.value = "";
  productStock.value = "";
  productImage.value = "";
  productActive.checked = true;
  imagePreview.classList.add("d-none");
  imagePreview.removeAttribute("src");
  productForm.classList.remove("was-validated");
  productModal.show();
}

// ====== Start ======
loadProducts();