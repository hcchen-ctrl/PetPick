// === commodity.js（連後端＋CSRF 相容） ===
const userId = 1;
const backToTopBtn = document.getElementById("backToTop");
const navbar = document.querySelector('.navbar');
let allProducts = [];
let lastScrollTop = 0;

// 取得 cookie（取 CSRF 用）
function getCookie(name) {
    return document.cookie.split('; ')
        .find(row => row.startsWith(name + '='))?.split('=')[1];
}
// 取出 Spring CSRF（若你啟用 CookieCsrfTokenRepository 才會有）
function getXsrfToken() {
    const v = getCookie('XSRF-TOKEN');
    return v ? decodeURIComponent(v) : '';
}

// ===== UI：返回頂部 + navbar 顯示隱藏 =====
window.addEventListener("scroll", () => {
    backToTopBtn.style.display = window.scrollY > 200 ? "flex" : "none";
    const st = window.pageYOffset || document.documentElement.scrollTop;
    if (st > lastScrollTop) navbar.classList.add("hide-navbar"); else navbar.classList.remove("hide-navbar");
    lastScrollTop = Math.max(st, 0);
});
backToTopBtn.addEventListener("click", () => window.scrollTo({ top: 0, behavior: 'smooth' }));

// ===== 載入商品列表 =====
fetch("/api/products")
    .then(res => {
        if (!res.ok) throw new Error("載入商品失敗");
        return res.json();
    })
    .then(products => {
        allProducts = products || [];
        filterAndRender("all", ""); // 預設顯示全部
        updateCartBadge();
    })
    .catch(err => console.error(err));

// ===== 篩選 + 排序 + 渲染 =====
function filterAndRender(type, keyword = "", sortOrder = "default") {
    const list = document.getElementById("product-list");

    let filtered = (allProducts || []).filter(p => {
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
      </div>
    `;
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

// ===== 加入購物車（相容 CSRF；同源時自動帶 cookie）=====
async function addToCart(userId, productId, quantity = 1) {
    try {
        const xsrf = getXsrfToken(); // 若你現在忽略 /api/cart/** 的 CSRF，沒有也沒關係
        const res = await fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}) // 有就帶，沒有略過
            },
            // 同源情況下預設就會帶 cookie；若你日後跨網域，改成 credentials: 'include'
            body: JSON.stringify({ userId, productId, quantity })
        });

        if (!res.ok) {
            const txt = await res.text().catch(() => '');
            throw new Error(`加入購物車失敗（${res.status}）：${txt}`);
        }

        await res.json(); // 可用回傳值更新畫面
        alert('✅ 已加入購物車');
        updateCartBadge();
    } catch (err) {
        console.error(err);
        // 常見 403：CSRF 或 CORS，印更多線索
        alert('❌ 加入失敗，請稍後再試');
    }
}

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
