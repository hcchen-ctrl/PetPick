document.addEventListener('DOMContentLoaded', () => {
    const userId = 1;

    // 必填元素工具
    const must = (id) => {
        const el = document.getElementById(id);
        if (!el) throw new Error(`#${id} not found in DOM`);
        return el;
    };
    const toInt = (v, def = 0) => {
        const n = Number(v);
        return Number.isFinite(n) ? n : def;
    };

    // 取得商品 id
    const params = new URLSearchParams(location.search);
    const productId = params.get('id');
    if (!productId) {
        alert('缺少商品編號');
        location.href = 'commodity.html';
        return;
    }

    // 徽章：顯示「總數量」
    function updateCartBadge() {
        fetch(`/api/cart/withProduct/${userId}`)
            .then(r => r.ok ? r.json() : [])
            .then(list => {
                const totalQty = Array.isArray(list)
                    ? list.reduce((s, it) => s + toInt(it.quantity ?? it.qty, 0), 0)
                    : 0;
                const b = document.getElementById('cart-badge');
                if (b) b.textContent = totalQty;
            })
            .catch(() => { });
    }

    // 若已在購物車就「更新數量=舊+新」，否則「新增」
    async function addOrUpdateCart(userId, productId, addQty, maxStock) {
        const cart = await fetch(`/api/cart/withProduct/${userId}`).then(r => r.json());
        const found = Array.isArray(cart)
            ? cart.find(it => toInt(it.productId ?? it.product_id) === toInt(productId))
            : null;

        if (found) {
            const cartId = toInt(found.cartId ?? found.cart_id);
            const currentQ = toInt(found.quantity ?? found.qty, 0);
            const targetQ = currentQ + toInt(addQty, 0);

            if (typeof maxStock === 'number' && targetQ > maxStock) {
                throw new Error('超過庫存數量');
            }

            const res = await fetch('/api/cart/update', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cartId, quantity: Math.max(1, targetQ) })
            });
            if (!res.ok) throw new Error('更新購物車失敗');
            return res.json();
        } else {
            // 新增
            if (typeof maxStock === 'number' && addQty > maxStock) {
                throw new Error('超過庫存數量');
            }
            const res = await fetch('/api/cart/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, productId, quantity: Math.max(1, toInt(addQty, 1)) })
            });
            if (!res.ok) throw new Error('加入購物車失敗');
            return res.json();
        }
    }

    // 讀商品
    fetch(`/api/products/${encodeURIComponent(productId)}`)
        .then(r => {
            if (r.status === 404) throw new Error('商品不存在或已下架');
            if (!r.ok) throw new Error('讀取商品失敗');
            return r.json();
        })
        .then(p => {
            // 同時相容 name / pname
            const { productId: pid, name, pname, description, price, stock, imageUrl } = p;
            const displayName = pname ?? name ?? '商品';

            // 基本文案
            must('pname').textContent = displayName;
            must('price').textContent = (price ?? 0).toLocaleString();
            must('desc').textContent = description || '—';

            // 圖片
            const mainImgEl = must('mainImg');
            const main = imageUrl || 'https://placehold.co/800x600?text=No+Image';
            mainImgEl.src = main;
            mainImgEl.alt = displayName;

            // 庫存徽章
            if (typeof stock === 'number') {
                const ok = must('stockTag'), low = must('stockLow'), out = must('stockOut');
                ok.classList.add('d-none'); low.classList.add('d-none'); out.classList.add('d-none');
                if (stock > 10) ok.classList.remove('d-none');
                else if (stock > 0) low.classList.remove('d-none');
                else out.classList.remove('d-none');
            }

            // 縮圖（單圖）
            const thumbs = must('thumbs');
            thumbs.innerHTML = '';
            const t = document.createElement('img');
            t.src = main; t.alt = `${displayName} 縮圖`;
            t.width = 72; t.height = 72; t.className = 'thumb rounded active';
            t.addEventListener('click', () => { mainImgEl.src = t.src; });
            thumbs.appendChild(t);

            // 數量 + 庫存提示
            const qtyInput = must('qty');
            const btnAdd = must('btn-addToCart');
            const stockInfoEl = document.getElementById('stockInfo');

            function renderStockText() {
                if (!stockInfoEl || typeof stock !== 'number') return;
                const selected = Math.max(1, parseInt(qtyInput.value || '1', 10));
                if (stock <= 0) {
                    stockInfoEl.innerHTML = '庫存：<b>0</b>（補貨中）';
                    btnAdd.disabled = true;
                    btnAdd.textContent = '補貨中';
                } else {
                    stockInfoEl.innerHTML = `庫存：<b>${stock}</b> 件（已選 <b>${selected}</b>）`;
                    btnAdd.disabled = false;
                    btnAdd.textContent = '加入購物車';
                }
            }

            // 初次顯示庫存
            renderStockText();

            must('qtyMinus').addEventListener('click', () => {
                qtyInput.stepDown();
                if (+qtyInput.value < 1) qtyInput.value = 1;
                if (typeof stock === 'number' && +qtyInput.value > stock) qtyInput.value = stock;
                renderStockText();
            });
            must('qtyPlus').addEventListener('click', () => {
                qtyInput.stepUp();
                if (typeof stock === 'number' && +qtyInput.value > stock) qtyInput.value = stock;
                renderStockText();
            });
            qtyInput.addEventListener('input', () => {
                let v = parseInt(qtyInput.value || '1', 10);
                if (isNaN(v) || v < 1) v = 1;
                if (typeof stock === 'number' && v > stock) v = stock;
                qtyInput.value = v;
                renderStockText();
            });

            // 加入購物車（依 Controller 規則：存在→update，不存在→add）
            btnAdd.addEventListener('click', async () => {
                const qty = Math.max(1, parseInt(qtyInput.value || '1', 10));
                try {
                    await addOrUpdateCart(userId, pid ?? Number(productId), qty, stock);
                    showToast('✅ 已加入購物車');
                    updateCartBadge();
                } catch (e) {
                    showToast(e?.message || '❌ 加入失敗，請稍後再試');
                }
            });

            updateCartBadge();
        })
        .catch(err => {
            alert(err.message || '讀取商品失敗');
            location.href = 'commodity.html';
        });

    // Toast
    function showToast(msg) {
        const el = document.getElementById('tipToast');
        const text = document.getElementById('toastText');
        if (!el || !text) { alert(msg); return; }
        text.textContent = msg;
        new bootstrap.Toast(el, { delay: 2000 }).show();
    }
});
