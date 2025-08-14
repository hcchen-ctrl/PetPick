const userId = 1;

const backToTopBtn = document.getElementById("backToTop");
let allProducts = [];


window.addEventListener("scroll", () => {
    if (window.scrollY > 200) {
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



// 初次載入所有商品
fetch("/api/products")
    .then(res => res.json())
    .then(products => {
        allProducts = products;
        filterAndRender("all", ""); // 預設顯示全部
        updateCartBadge();
    });

// 渲染商品資料（根據分類與關鍵字）
// 渲染商品資料（根據分類與關鍵字 + 排序）—— 方法一版本
function filterAndRender(type, keyword = "", sortOrder = "default") {
    const list = document.getElementById("product-list");

    let filtered = allProducts.filter(p => {
        const matchType = (type === "all") || (p.type === type);
        const name = (p.pname ?? p.name ?? "").toString();
        const desc = (p.description ?? "").toString();
        const matchKeyword = !keyword || (
            name.toLowerCase().includes(keyword.toLowerCase()) ||
            desc.toLowerCase().includes(keyword.toLowerCase())
        );
        return matchType && matchKeyword;
    });

    // 價格排序
    if (sortOrder === "asc") {
        filtered.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
    } else if (sortOrder === "desc") {
        filtered.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
    }

    list.innerHTML = filtered.map(p => {
        const pid = p.productId ?? p.id;
        const pname = p.pname ?? p.name ?? "";
        const img = p.imageUrl || "https://i.ibb.co/7d1QYqY2/image-50dp-000000-FILL0-wght400-GRAD0-opsz48.png";
        const desc = p.description ?? "";
        const price = p.price ?? 0;

        return `
      <div class="col-4 col-md-2 mb-3">
    <div class="card h-100">

      <!-- 只讓這塊可點，設定為 positioned 祖先 -->
      <div class="clickable-area position-relative">
        <img src="${img}" alt="圖" class="card-img-top" style="height:200px;object-fit:cover;">
        <div class="card-body">
          <h5 class="card-title">${pname}</h5>
          <p class="card-text">${desc}</p>
        </div>
        <!-- 把 stretched-link 放在這個容器內，僅覆蓋 clickable-area -->
        <a class="stretched-link"
           href="productSite.html?id=${pid}"
           aria-label="查看 ${pname} 詳情"></a>
      </div>

      <!-- 價格與加入購物車按鈕在 clickable-area 之外，不會被覆蓋 -->
      <div class="mt-auto d-flex justify-content-between align-items-center p-3">
        <div style="font-size:18px;">
          <u class="fw-bold text-danger">NT$${price}</u>
        </div>
        <button class="btn btn-material"
                onclick="addToCart(${userId}, ${pid}, 1)">
          <span class="card-material-icons">add_shopping_cart</span>
        </button>
      </div>

    </div>
  </div>
    `;
    }).join('');
}

// 記錄目前分類
let currentType = "all";

// radio 切換分類
document.querySelectorAll('input[name="productFilter"]').forEach(radio => {
    radio.addEventListener("change", function () {
        currentType = this.value;
        const keyword = document.getElementById("searchInput").value;
        filterAndRender(currentType, keyword);
    });
});

// 搜尋欄輸入事件
document.getElementById("searchInput").addEventListener("input", function () {
    filterAndRender(currentType, this.value);
});

function onSortChange() {
    const sortOrder = document.getElementById("sortSelect").value;

    // 假設你有目前的篩選類型與關鍵字，可以存在全域變數
    const currentType = document.querySelector('input[name="productFilter"]:checked').value;
    const keyword = document.getElementById("searchInput")?.value || "";

    filterAndRender(currentType, keyword, sortOrder);
}

function addToCart(userId, productId, quantity) {
    const payload = {
        userId: userId,
        productId: productId,
        quantity: quantity
    };

    fetch('/api/cart/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('加入購物車失敗');
            }
            return response.json();
        })
        .then(data => {
            alert('✅ 已加入購物車');
            updateCartBadge();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('❌ 加入失敗，請稍後再試');
        });
}

function updateCartBadge() {
    fetch(`/api/cart/withProduct/${userId}`) // 依 userId 查詢
        .then(res => {
            if (!res.ok) {
                throw new Error('讀取購物車資料失敗');
            }
            return res.json();
        })
        .then(cart => {
            document.getElementById('cart-badge').textContent = cart.length;
        })
        .catch(error => {
            console.error('Error:', error);
        });
}