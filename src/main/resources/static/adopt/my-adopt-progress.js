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
const auth = await fetch('/api/auth/status', { credentials: 'include' }).then(r => r.json());
if (!auth.loggedIn) {
    sessionStorage.setItem('redirect', location.pathname + location.search);
    location.replace('/login.html');
    throw new Error('redirecting to login'); // ★ 加這行，中斷後續程式
}

// 管理員不該在此頁，直接導去審核中心
if (auth.role === 'ADMIN') {
    location.replace('/post-review.html');
    throw new Error('redirecting: admin'); // ★ 加這行，中斷後續程式
}

// 依 status 打 API：/api/posts/my?status=...
const api = `/api/posts/my?status=${encodeURIComponent(status)}`;

let data = [];
try {
    if (status === 'approved') {
        const [r1, r2] = await Promise.all([
            fetch('/api/posts/my?status=approved', { credentials: 'include' }),
            fetch('/api/posts/my?status=on_hold', { credentials: 'include' })
        ]);
        const a = r1.ok ? await r1.json() : [];
        const b = r2.ok ? await r2.json() : [];
        data = [...a, ...b].sort((x, y) => new Date(y.createdAt) - new Date(x.createdAt));
    } else {
        const res = await fetch(`/api/posts/my?status=${encodeURIComponent(status)}`, { credentials: 'include' });
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

window.cancelPost = async (id) => {
    if (!confirm('確定要取消這筆刊登嗎？')) return;
    const ok = await fetch(`/api/posts/${id}/cancel`, { method: 'PATCH', credentials: 'include' }).then(r => r.ok);
    alert(ok ? '已取消' : '取消失敗');
    if (ok) location.reload();
};

window.holdPost = async (id, hold = true) => {
    const msg = hold ? '暫停上架？' : '恢復上架？';
    if (!confirm(msg)) return;
    const ok = await fetch(`/api/posts/${id}/hold?hold=${hold}`, { method: 'PATCH', credentials: 'include' }).then(r => r.ok);
    alert(ok ? '已更新' : '更新失敗');
    if (ok) location.reload();
};

window.closePost = async (id) => {
    if (!confirm('確定要關閉（代表已送養完成）？')) return;
    const ok = await fetch(`/api/posts/${id}/close`, { method: 'PATCH', credentials: 'include' }).then(r => r.ok);
    alert(ok ? '已關閉' : '關閉失敗');
    if (ok) location.reload();
};