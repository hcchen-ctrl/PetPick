fetch("/api/products")
    .then(res => res.json())
    .then(products => {
        const tbody = document.getElementById("product-table-body");

        tbody.innerHTML = products.map(p => `
                <tr>
                    <td>${p.productId}</td>
                    <td>${p.pname}</td>
                    <td>${p.description}</td>
                    <td>NT$${p.price}</td>
                    <td>${p.stock}</td>
                    <td>
                        <a href="#" class="text-primary text-decoration-underline"
                           data-bs-toggle="modal"
                           data-bs-target="#imageModal"
                           onclick="showImage('${p.imageUrl}')">附圖</a>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="editProduct(${p.productId})">編輯</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProduct(${p.productId})">刪除</button>
                    </td>
                </tr>
            `).join('');
    });

function showImage(url) {
    document.getElementById("modal-image").src = url;
}

function editProduct(productId) {
    fetch(`/api/products/${productId}`)
        .then(res => res.json())
        .then(data => {
            // 填入 modal 中的欄位
            document.getElementById("productId").value = data.productId;
            document.getElementById("productName").value = data.name;
            document.getElementById("productDescription").value = data.description;
            document.getElementById("productPrice").value = data.price;
            document.getElementById("productStock").value = data.stock;
            document.getElementById("productImage").value = data.imageUrl;

            // 顯示 modal
            const modal = new bootstrap.Modal(document.getElementById("productModal"));
            modal.show();
        })
        .catch(err => console.error("讀取商品失敗:", err));
}

function deleteProduct(productId) {
    if (!confirm("確定要刪除這個商品嗎？")) return;

    fetch(`/api/products/${productId}`, {
        method: "DELETE"
    })
        .then(res => {
            if (res.ok) {
                alert("刪除成功");
                location.reload();
            } else {
                alert("刪除失敗");
            }
        })
        .catch(err => console.error("刪除錯誤:", err));
}

document.getElementById('productForm').addEventListener('submit', function (e) {
    e.preventDefault();

    const id = document.getElementById('productId').value;
    const pname = document.getElementById('productName').value;
    const description = document.getElementById('productDescription').value;
    const price = +document.getElementById('productPrice').value;
    const stock = +document.getElementById('productStock').value;
    const imageUrl = document.getElementById('productImage').value || 'https://via.placeholder.com/60';

    const product = {
        id,
        pname,
        description,
        price,
        stock,
        imageUrl
    };

    const isNew = !product.id; // true 代表新增，false 代表編輯
    const method = isNew ? 'POST' : 'PUT';
    const url = isNew ? '/api/products' : `/api/products/${product.id}`;

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(product)
    })
        .then(response => {
            if (response.ok) {
                bootstrap.Modal.getInstance(document.getElementById('productModal')).hide();
                this.reset();
                alert(isNew ? '新增成功' : '更新成功'); // 依情況顯示訊息
                location.reload();
            } else {
                alert(isNew ? '新增失敗' : '更新失敗');
            }
        })
        .catch(err => {
            console.error('Error saving product:', err);
            alert(isNew ? '新增失敗' : '更新失敗');
        });
});
