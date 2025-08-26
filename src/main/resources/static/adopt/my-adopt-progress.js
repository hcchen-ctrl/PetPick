// ========= CSRF Token 處理函數 =========
function getCsrfToken() {
    // 方法1: 從 cookie 取得 CSRF token
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'XSRF-TOKEN') {
            return decodeURIComponent(value);
        }
    }

    // 方法2: 從 meta tag 取得 (如果 HTML 中有設定)
    const metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken) {
        return metaToken.getAttribute('content');
    }

    return null;
}

// 通用的 API 請求函數
async function apiRequest(url, method = 'GET', data = null) {
    const config = {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include'  // 確保發送 cookies
    };

    // 對於需要 CSRF 保護的請求，添加 token
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
        const csrfToken = getCsrfToken();

        if (!csrfToken) {
            // 如果沒有 token，先嘗試獲取一個新的
            try {
                const tokenResponse = await fetch('/api/csrf-token', {
                    method: 'GET',
                    credentials: 'include'
                });
                if (tokenResponse.ok) {
                    const tokenData = await tokenResponse.json();
                    if (tokenData.token) {
                        config.headers['X-Csrf-Token'] = tokenData.token;
                    }
                }
            } catch (e) {
                console.warn('無法獲取 CSRF token:', e);
            }
        } else {
            config.headers['X-Csrf-Token'] = csrfToken;
        }
    }

    if (data && ['POST', 'PUT', 'PATCH'].includes(method)) {
        config.body = JSON.stringify(data);
    }

    const response = await fetch(url, config);

    // 如果是 403 錯誤且是 CSRF 相關，嘗試重新獲取 token 並重試一次
    if (response.status === 403 && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
        console.log('403 錯誤，嘗試重新獲取 CSRF token...');

        try {
            // 重新獲取 CSRF token
            const tokenResponse = await fetch('/api/csrf-token', {
                method: 'GET',
                credentials: 'include'
            });

            if (tokenResponse.ok) {
                const tokenData = await tokenResponse.json();
                if (tokenData.token) {
                    // 用新的 token 重試請求
                    config.headers['X-Csrf-Token'] = tokenData.token;
                    console.log('使用新的 CSRF token 重試請求');
                    return await fetch(url, config);
                }
            }
        } catch (e) {
            console.error('重新獲取 CSRF token 失敗:', e);
        }
    }

    return response;
}

// ========= 原有邏輯（修改 API 調用） =========

// 讀取查詢參數：status = pending / approved（預設 pending）
const url = new URL(location.href);
const status = url.searchParams.get('status') || 'pending';

// Stepper：只亮當前步驟（pending→亮2；approved→亮3）
const on = id => document.getElementById(id)?.classList.add('step-active');
const off = id => document.getElementById(id)?.classList.remove('step-active');
['step1', 'step2', 'step3'].forEach(off);
if (status === 'approved') on('step3');   // 只亮 3
else on('step2');   // 只亮 2

// 取得登入狀態，未登入就導去 login
const auth = await apiRequest('/api/auth/status').then(r => r.json());
if (!auth.loggedIn) {
    sessionStorage.setItem('redirect', location.pathname + location.search);
    location.replace('/loginpage.html');
    throw new Error('redirecting to login'); // ★ 加這行，中斷後續程式
}

// 管理員不該在此頁，直接導去審核中心
if (auth.role === 'ADMIN') {
    location.replace('/post-review.html');
    throw new Error('redirecting: admin'); // ★ 加這行，中斷後續程式
}

// 依 status 打 API：/api/posts/my?status=...
let data = [];
try {
    if (status === 'approved') {
        const [r1, r2] = await Promise.all([
            apiRequest('/api/posts/my?status=approved'),
            apiRequest('/api/posts/my?status=on_hold')
        ]);
        const a = r1.ok ? await r1.json() : [];
        const b = r2.ok ? await r2.json() : [];
        data = [...a, ...b].sort((x, y) => new Date(y.createdAt) - new Date(x.createdAt));
    } else {
        const res = await apiRequest(`/api/posts/my?status=${encodeURIComponent(status)}`);
        data = res.ok ? await res.json() : [];
    }
} catch (err) {
    console.error(err);
}

// 產卡
const list = document.getElementById('list');
const empty = document.getElementById('emptyHint');

const badge = s =>
    s === 'approved' ? '<span class="badge text-bg-success">刊登成功</span>' :
        s === 'on_hold' ? '<span class="badge text-bg-secondary">暫停中</span>' :
            s === 'rejected' ? '<span class="badge text-bg-danger">退件</span>' :
                '<span class="badge text-bg-warning text-dark">審核中</span>';

const fallback = '/images/no-image.jpg';
const firstImg = p => p.image1 || p.image2 || p.image3 || fallback;

list.innerHTML = data.map(p => `
        <div class="col-12">
            <div class="card shadow-sm">
            <div class="row g-0">
                <div class="col-md-4">
                <div class="thumb-box bg-contain" style="background-image:url('${firstImg(p)}');"></div>
                </div>
                <div class="col-md-8">
                <div class="card-body">
                    <h5 class="card-title mb-2">${p.title || '未命名'} │ ${p.breed || p.species || ''}</h5>
                    <p class="mb-2">審核狀態：${badge(p.status)}</p>
                    <p class="text-muted mb-3">送出日期：${(p.createdAt ?? '').toString().substring(0, 10)}</p>

                    <div class="d-flex gap-2">
                    ${p.status === 'pending' ? `
                        <button class="btn btn-outline-danger btn-sm" onclick="cancelPost(${p.id})">取消刊登</button>
                    ` : ''}

                    ${p.status === 'approved' ? `
                        <button class="btn btn-outline-warning btn-sm" onclick="holdPost(${p.id}, true)">暫停</button>
                        <button class="btn btn-outline-secondary btn-sm" onclick="closePost(${p.id})">關閉</button>
                    ` : ''}

                    ${p.status === 'on_hold' ? `
                        <button class="btn btn-outline-success btn-sm" onclick="holdPost(${p.id}, false)">恢復</button>
                        <button class="btn btn-outline-secondary btn-sm" onclick="closePost(${p.id})">關閉</button>
                    ` : ''}
                    </div>
                </div>
                </div>
            </div>
            </div>
        </div>
        `).join('');

if (!data.length) empty.classList.remove('d-none');

// tabs 切換（右上角「審核中 / 刊登完成」）
const tabs = document.getElementById('tabs');
if (tabs) {
    tabs.querySelectorAll('a').forEach(a => {
        const to = a.dataset.to; // 'pending' 或 'approved'
        if (to === status) a.classList.add('active'); // 高亮目前頁籤
        a.addEventListener('click', e => {
            e.preventDefault();
            location.href = `/my-adopt-progress.html?status=${to}`;
        });
    });
}

// ========= 操作函數（使用 apiRequest） =========
window.cancelPost = async (id) => {
    if (!confirm('確定要取消這筆刊登嗎？')) return;

    try {
        const response = await apiRequest(`/api/posts/${id}/cancel`, 'PATCH');

        if (!response.ok) {
            if (response.status === 401) {
                alert('請先登入');
                location.href = '/loginpage.html';
                return;
            } else if (response.status === 403) {
                alert('權限不足，請重新整理頁面後再試');
                return;
            } else {
                const errorText = await response.text();
                alert('取消失敗：' + errorText);
                return;
            }
        }

        alert('已取消');
        location.reload();
    } catch (error) {
        console.error('取消刊登錯誤:', error);
        alert('取消失敗，請重試');
    }
};

window.holdPost = async (id, hold = true) => {
    const msg = hold ? '暫停上架？' : '恢復上架？';
    if (!confirm(msg)) return;

    try {
        const response = await apiRequest(`/api/posts/${id}/hold?hold=${hold}`, 'PATCH');

        if (!response.ok) {
            if (response.status === 401) {
                alert('請先登入');
                location.href = '/loginpage.html';
                return;
            } else if (response.status === 403) {
                alert('權限不足，請重新整理頁面後再試');
                return;
            } else {
                const errorText = await response.text();
                alert('更新失敗：' + errorText);
                return;
            }
        }

        alert('已更新');
        location.reload();
    } catch (error) {
        console.error('更新狀態錯誤:', error);
        alert('更新失敗，請重試');
    }
};

window.closePost = async (id) => {
    if (!confirm('確定要關閉（代表已送養完成）？')) return;

    try {
        const response = await apiRequest(`/api/posts/${id}/close`, 'PATCH');

        if (!response.ok) {
            if (response.status === 401) {
                alert('請先登入');
                location.href = '/loginpage.html';
                return;
            } else if (response.status === 403) {
                alert('權限不足，請重新整理頁面後再試');
                return;
            } else {
                const errorText = await response.text();
                alert('關閉失敗：' + errorText);
                return;
            }
        }

        alert('已關閉');
        location.reload();
    } catch (error) {
        console.error('關閉刊登錯誤:', error);
        alert('關閉失敗，請重試');
    }
};