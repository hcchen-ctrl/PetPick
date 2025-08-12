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
function filterAndRender(type, keyword = "", sortOrder = "default") {
    const list = document.getElementById("product-list");

    let filtered = allProducts.filter(p => {
        const matchType = (type === "all") || (p.type === type);
        const matchKeyword = !keyword || (
            p.pname.toLowerCase().includes(keyword.toLowerCase()) ||
            p.description.toLowerCase().includes(keyword.toLowerCase())
        );
        return matchType && matchKeyword;
    });

    // 價格排序
    if (sortOrder === "asc") {
        filtered.sort((a, b) => a.price - b.price);
    } else if (sortOrder === "desc") {
        filtered.sort((a, b) => b.price - a.price);
    }

    list.innerHTML = filtered.map(p => `
        <div class="col-4 col-md-2 mb-3">
            <div class="card h-100">
                <img src="${p.imageUrl || 'https://i.ibb.co/7d1QYqY2/image-50dp-000000-FILL0-wght400-GRAD0-opsz48.png'}" 
                    alt="圖" 
                    class="card-img-top" 
                    style="height: 200px; object-fit: cover;" />
                <div class="card-body d-flex flex-column">
                    <div>
                        <h5 class="card-title">${p.pname}</h5>
                        <p class="card-text">${p.description}</p>
                    </div>
                    <div class="mt-auto d-flex justify-content-between align-items-center">
                        <div style="font-size: 18px;">
                            <u class="fw-bold text-danger">NT$${p.price}</u>
                        </div>
                        <button class="btn btn-material" onclick="addToCart(${userId}, ${p.productId}, 1)">
                            <span class="card-material-icons">add_shopping_cart</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
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