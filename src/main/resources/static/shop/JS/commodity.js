// === commodity.js（僅顯示已上架） ===
const userId = 1;
const backToTopBtn = document.getElementById("backToTop");
const navbar = document.querySelector('.navbar');
let allProducts = [];
let lastScrollTop = 0;

// 取得 cookie（取 CSRF 用）
function getCookie(name) {
  return document.cookie.split('; ').find(row => row.startsWith(name + '='))?.split('=')[1];
}
function getXsrfToken() {
  const v = getCookie('XSRF-TOKEN');
  return v ? decodeURIComponent(v) : '';
}

// ===== Toast =====
function ensureToastContainer() {
  let c = document.getElementById("toastContainer");
  if (!c) {
    const html = `<div id="toastContainer" class="toast-container position-fixed top-0 end-0 p-3" style="z-index:2000;"></div>`;
    document.body.insertAdjacentHTML("beforeend", html);
    c = document.getElementById("toastContainer");
  }
  return c;
}
function showToast(message, type = "success") {
  const container = ensureToastContainer();
  const id = "t" + Math.random().toString(36).slice(2);
  const html = `
  <div id="${id}" class="toast align-items-center text-bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
    <div class="d-flex">
      <div class="toast-body">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
    </div>
  </div>`;
  container.insertAdjacentHTML("beforeend", html);
  const el = document.getElementById(id);
  const t = new bootstrap.Toast(el, { delay: 2200 });
  t.show();
  el.addEventListener("hidden.bs.toast", () => el.remove());
}

// ===== UI：返回頂部 + navbar 顯示隱藏 =====
window.addEventListener("scroll", () => {
  backToTopBtn.style.display = window.scrollY > 200 ? "flex" : "none";
  const st = window.pageYOffset || document.documentElement.scrollTop;
  if (st > lastScrollTop) navbar.classList.add("hide-navbar"); else navbar.classList.remove("hide-navbar");
  lastScrollTop = Math.max(st, 0);
});
backToTopBtn.addEventListener("click", () => window.scrollTo({ top: 0, behavior: 'smooth' }));

// ===== 讀商品 =====
// 如果你的後端已支援只回上架：可改 fetch("/api/products?active=true")
fetch("/api/products?active=true")
  .then(res => {
    if (!res.ok) throw new Error("載入商品失敗");
    return res.json();
  })
  .then(products => {
    allProducts = products || [];
    filterAndRender("all", ""); // 預設顯示全部（但僅上架的）
    updateCartBadge();
  })
  .catch(err => console.error(err));

// ===== 工具：判斷是否上架 =====
function pickActive(p) {
  if (typeof p.active === "boolean") return p.active;
  if (typeof p.isActive === "boolean") return p.isActive;
  // 若後端用 status 字串，也嘗試判斷：出現「下架/停售/inactive/off」視為未上架
  if (typeof p.status === "string") return !/下架|停售|inactive|off/i.test(p.status);
  // 若完全沒有欄位，預設視為「上架中」以保相容（需要嚴格只顯示上架，請在後端補齊 active）
  return true;
}

// ===== 篩選 + 排序 + 渲染（只顯示已上架）=====
function filterAndRender(type, keyword = "", sortOrder = "default") {
  const list = document.getElementById("product-list");

  let filtered = (allProducts || [])
    // 先過濾「上架」
    .filter(p => pickActive(p))
    // 再依分類/關鍵字
    .filter(p => {
      const matchType = (type === "all") || (p.type === type);
      const name = (p.pname ?? p.name ?? "").toString();
      const desc = (p.description ?? "").toString();
      const matchKeyword = !keyword || name.toLowerCase().includes(keyword.toLowerCase()) || desc.toLowerCase().includes(keyword.toLowerCase());
      return matchType && matchKeyword;
    });

  if (sortOrder === "asc") filtered.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
  if (sortOrder === "desc") filtered.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));

  list.innerHTML = filtered.map(p => {
    const pid = p.productId ?? p.id;
    const pname = p.pname ?? p.name ?? "";
    const img = p.imageUrl || "https://i.ibb.co/7d1QYqY2/image-50dp-000000-FILL0-wght400-GRAD0-opsz48.png";
    const desc = p.description ?? "";
    const price = p.price ?? 0;

    return `
    <div class="col-6 col-md-3 col-lg-2 mb-3">
      <div class="card h-100">
        <div class="clickable-area position-relative">
          <img src="${img}" alt="圖" class="card-img-top" style="height:200px;object-fit:cover;">
          <div class="card-body">
            <h5 class="card-title">${pname}</h5>
            <p class="card-text">${desc}</p>
          </div>
          <a class="stretched-link" href="productSite.html?id=${encodeURIComponent(pid)}" aria-label="查看 ${pname} 詳情"></a>
        </div>
        <div class="mt-auto d-flex justify-content-between align-items-center p-3">
          <div class="fw-bold text-danger">NT$${price}</div>
          <button class="btn btn-material" onclick="addToCart(${userId}, ${pid}, 1)">
            <span class="card-material-icons">add_shopping_cart</span>
          </button>
        </div>
      </div>
    </div>`;
  }).join('');
}

// 當前分類
let currentType = "all";

// 切換分類
document.querySelectorAll('input[name="productFilter"]').forEach(radio => {
  radio.addEventListener("change", function () {
    currentType = this.value;
    const keyword = document.getElementById("searchInput").value;
    filterAndRender(currentType, keyword, document.getElementById("sortSelect").value);
  });
});

// 搜尋
document.getElementById("searchInput").addEventListener("input", function () {
  filterAndRender(currentType, this.value, document.getElementById("sortSelect").value);
});

// 排序
function onSortChange() {
  const sortOrder = document.getElementById("sortSelect").value;
  const keyword = document.getElementById("searchInput")?.value || "";
  filterAndRender(currentType, keyword, sortOrder);
}
window.onSortChange = onSortChange; // 若用 inline onchange 需要暴露

// ===== 加入購物車（相容 CSRF；同源時自動帶 cookie）=====
async function addToCart(userId, productId, quantity = 1) {
  try {
    const xsrf = getXsrfToken();
    const res = await fetch('/api/cart/add', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {})
      },
      body: JSON.stringify({ userId, productId, quantity })
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      throw new Error(`加入購物車失敗（${res.status}）${txt ? `：${txt}` : ''}`);
    }

    await res.json();
    showToast('✅ 已加入購物車', 'success');
    updateCartBadge();
  } catch (err) {
    console.error(err);
    showToast('❌ 加入失敗，請稍後再試', 'danger');
  }
}
window.addToCart = addToCart; // 若用 inline onclick 需要暴露

// 徽章：顯示「項目數」（非數量總和）
function updateCartBadge() {
  fetch(`/api/cart/withProduct/${userId}`)
    .then(res => {
      if (!res.ok) throw new Error('讀取購物車資料失敗');
      return res.json();
    })
    .then(cart => {
      document.getElementById('cart-badge').textContent = (cart || []).length;
    })
    .catch(err => console.error(err));
}