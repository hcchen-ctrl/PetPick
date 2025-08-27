import { requireAdmin } from '/adopt/auth.js';
await requireAdmin();

// ---- DOM refs ----
const list = document.getElementById('list');
const empty = document.getElementById('empty');
const filters = document.getElementById('filters');
const fStatus = document.getElementById('fStatus');
const fSpecies = document.getElementById('fSpecies');
const fQ = document.getElementById('fQ');
const resultCount = document.getElementById('resultCount');
const pager = document.getElementById('pager');

const modal = new bootstrap.Modal('#detailModal');
const mTitle = document.getElementById('mTitle');
const mBody = document.getElementById('mBody');
const mApprove = document.getElementById('mApprove');
const mReject = document.getElementById('mReject');

// ---- state & URL sync ----
const qs = new URLSearchParams(location.search);
const state = {
    page: +(qs.get('page') || 0),
    size: 24,
    status: qs.get('status') || 'pending',
    species: qs.get('species') || '',
    q: qs.get('q') || ''
};

// initialize filter UI
fStatus.value = ['pending', 'approved', 'rejected'].includes(state.status) ? state.status : 'all';
fSpecies.value = state.species;
fQ.value = state.q;

function buildParams() {
    const p = new URLSearchParams();
    if (state.status && state.status !== 'all') p.set('status', state.status);
    if (state.species) p.set('species', state.species);
    if (state.q) p.set('q', state.q.trim());
    p.set('page', state.page);
    p.set('size', state.size);
    return p.toString();
}

function syncUrl(push = false) {
    const url = `/post-review.html?${buildParams()}`;
    if (push) history.pushState(null, '', url); else history.replaceState(null, '', url);
}

window.addEventListener('popstate', () => {
    const q = new URLSearchParams(location.search);
    state.page = +(q.get('page') || 0);
    state.status = q.get('status') || 'pending';
    state.species = q.get('species') || '';
    state.q = q.get('q') || '';
    fStatus.value = ['pending', 'approved', 'rejected'].includes(state.status) ? state.status : 'all';
    fSpecies.value = state.species;
    fQ.value = state.q;
    load();
});

// ---- format helpers ----
const fallback = '/images/no-image.jpg';
const fmt = {
    badge(s) {
        if (s === 'approved') return '<span class="badge bg-success badge-status">已通過</span>';
        if (s === 'rejected') return '<span class="badge bg-danger badge-status">已退回</span>';
        if (s === 'on_hold') return '<span class="badge bg-secondary badge-status">暫停</span>';
        if (s === 'closed') return '<span class="badge bg-dark badge-status">已關閉</span>';
        if (s === 'cancelled') return '<span class="badge bg-outline-secondary text-dark border badge-status">已取消</span>';
        return '<span class="badge bg-warning text-dark badge-status">審核中</span>';
    },
    place(p = {}) { return [p.city, p.district].filter(Boolean).join(' '); },
    sexText(s) { return s === 'male' ? '公' : s === 'female' ? '母' : '—'; },
    sexIcon(s) { return s === 'male' ? '♂' : s === 'female' ? '♀' : ''; },
    neuterText(n) { return n === 'yes' ? '是' : n === 'no' ? '否' : '不確定'; },
    ageLimitText(a) { return a === 'age20plus' ? '20歲以上' : a === 'age25plus' ? '25歲以上' : '不限'; },
    contactMethodText(m) { return m === 'line_only' ? '僅 LINE' : '電話＋簡訊'; },
    boolText(b) { return b ? '需要' : '不需要'; },
    animalLine(p = {}) {
        return [p.species, p.breed, [this.sexText(p.sex), this.sexIcon(p.sex)].filter(Boolean).join(' '), p.age, p.bodyType]
            .filter(Boolean).join('｜');
    },
    contactLine(p = {}) {
        const a = [`聯絡人：${p.contactName || '—'}`, `電話：${p.contactPhone || '—'}`];
        if (p.contactLine) a.push(`LINE：${p.contactLine}`);
        return a.join('　');
    }
};

// ---- 改進的 CSRF Token function ----
function getCsrfToken() {
    console.log('正在獲取 CSRF Token...');

    // 方法1: 從 cookie 中獲取 XSRF-TOKEN
    const cookies = document.cookie.split(';');
    console.log('所有 cookies:', document.cookie);

    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'XSRF-TOKEN') {
            const token = decodeURIComponent(value);
            console.log('從 cookie 獲取到 CSRF Token:', token);
            return token;
        }
    }

    // 方法2: 從 meta tag 獲取
    const metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken) {
        const token = metaToken.getAttribute('content');
        console.log('從 meta tag 獲取到 CSRF Token:', token);
        return token;
    }

    // 方法3: 嘗試手動獲取 CSRF token（備用方案）
    try {
        const tokenElement = document.querySelector('input[name="_csrf"]');
        if (tokenElement) {
            const token = tokenElement.value;
            console.log('從 input 獲取到 CSRF Token:', token);
            return token;
        }
    } catch (e) {
        console.warn('無法從 input 獲取 CSRF token:', e);
    }

    console.warn('無法獲取 CSRF Token');
    return null;
}

// 強制獲取新的 CSRF token 的函數
async function refreshCsrfToken() {
    try {
        const response = await fetch('/loginpage', { method: 'GET' });
        if (response.ok) {
            console.log('已刷新頁面以獲取新的 CSRF token');
            // 重新檢查 token
            return getCsrfToken();
        }
    } catch (e) {
        console.error('刷新 CSRF token 失敗:', e);
    }
    return null;
}

// ---- core loaders ----
async function load() {
    window.showLoading?.();
    try {
        const r = await fetch(`/api/posts?${buildParams()}`);
        if (!r.ok) throw new Error('讀取失敗');

        const data = await r.json();
        const items = data.content ?? data ?? [];

        empty.classList.toggle('d-none', items.length > 0);
        resultCount.textContent = (data.totalElements != null)
            ? `共 ${data.totalElements} 筆，第 ${data.number + 1}/${data.totalPages} 頁`
            : `共 ${items.length} 筆`;

        list.innerHTML = items.map(p => {
            const img = p.image1 || p.image2 || p.image3 || fallback;
            return `
        <div class="col-12 col-md-6 col-lg-4">
          <div class="card h-100">
            <div class="card-thumb"><img src="${img}" onerror="this.src='${fallback}'"></div>
            <div class="card-body">
              <h5 class="mb-1 d-flex align-items-center justify-content-between">
                <span>${p.title || '未命名'}</span>
                ${fmt.badge(p.status)}
              </h5>
              <div class="text-muted small mb-1">${fmt.place(p)}</div>
              <div class="small mb-1">動物：${fmt.animalLine(p)}</div>
              <div class="small mb-2">聯絡：${fmt.contactLine(p)}</div>
              <div class="d-flex flex-wrap gap-2">
                <button class="btn btn-outline-primary btn-sm" data-view="${p.id}">詳情</button>
                ${p.status === 'pending' ? `
                  <button class="btn btn-success btn-sm" data-pass="${p.id}">通過</button>
                  <button class="btn btn-danger btn-sm"  data-reject="${p.id}">退回</button>` : ``}
                ${p.status === 'approved' ? `
                  <button class="btn btn-outline-warning btn-sm" data-hold="${p.id}" data-do="hold">暫停</button>
                  <button class="btn btn-outline-secondary btn-sm" data-close="${p.id}">關閉</button>` : ``}
                ${p.status === 'on_hold' ? `
                  <button class="btn btn-outline-success btn-sm" data-hold="${p.id}" data-do="resume">恢復</button>
                  <button class="btn btn-outline-secondary btn-sm" data-close="${p.id}">關閉</button>` : ``}
              </div>
            </div>
          </div>
        </div>`;
        }).join('');

        // 綁定事件
        list.querySelectorAll('[data-view]').forEach(b => b.onclick = () => openDetail(b.dataset.view));
        list.querySelectorAll('[data-pass]').forEach(b => b.onclick = () => updateStatus(b.dataset.pass, 'approved'));
        list.querySelectorAll('[data-reject]').forEach(b => b.onclick = () => updateStatus(b.dataset.reject, 'rejected'));
        list.querySelectorAll('[data-hold]').forEach(b => {
            const id = b.dataset.hold; const resume = b.dataset.do === 'resume';
            b.onclick = () => adminHold(id, !resume);
        });
        list.querySelectorAll('[data-close]').forEach(b => b.onclick = () => adminClose(b.dataset.close));

        // 分頁
        if (data.totalPages != null) {
            pager.innerHTML = `
        <button class="btn btn-outline-secondary" ${data.number <= 0 ? 'disabled' : ''} id="pgPrev">上一頁</button>
        <button class="btn btn-outline-secondary" ${data.number >= data.totalPages - 1 ? 'disabled' : ''} id="pgNext">下一頁</button>`;
            pager.querySelector('#pgPrev')?.addEventListener('click', () => { state.page = Math.max(0, data.number - 1); syncUrl(true); load(); });
            pager.querySelector('#pgNext')?.addEventListener('click', () => { state.page = Math.min(data.totalPages - 1, data.number + 1); syncUrl(true); load(); });
        } else {
            pager.innerHTML = '';
        }
    } catch (e) {
        console.error(e);
        list.innerHTML = '';
        empty.classList.remove('d-none');
        resultCount.textContent = '讀取失敗';
        pager.innerHTML = '';
    } finally {
        window.hideLoading?.();
    }
}

async function openDetail(id) {
    mTitle.textContent = '詳細資料';
    mBody.innerHTML = `<div class="text-center text-muted">載入中…</div>`;
    modal.show();

    // 修正 API 路徑：從 /api/adopts 改為 /api/posts
    const r = await fetch(`/api/posts/${id}`);
    if (!r.ok) { mBody.innerHTML = `<div class="text-danger">讀取失敗</div>`; return; }
    const p = await r.json();

    const imgs = [p.image1, p.image2, p.image3].filter(u => !!u && u.trim());
    const useImgs = imgs.length ? imgs : [fallback];
    const slides = useImgs.map((u, i) =>
        `<div class="carousel-item ${i === 0 ? 'active' : ''}">
       <img src="${u}" class="d-block w-100 carousel-img" onerror="this.src='${fallback}'">
     </div>`).join('');
    const indicators = useImgs.map((_, i) =>
        `<button type="button" data-bs-target="#mCarousel" data-bs-slide-to="${i}"
      class="${i === 0 ? 'active' : ''}" ${i === 0 ? 'aria-current="true"' : ''}
      aria-label="第 ${i + 1} 張"></button>`).join('');

    mTitle.innerHTML = `${p.title || '未命名'}　${fmt.badge(p.status)}`;
    mBody.innerHTML = `
    <div class="row g-3">
      <div class="col-md-6">
        <div id="mCarousel" class="carousel slide" data-bs-ride="true">
          <div class="carousel-indicators">${indicators}</div>
          <div class="carousel-inner">${slides}</div>
          <button class="carousel-control-prev" type="button" data-bs-target="#mCarousel" data-bs-slide="prev">
            <span class="carousel-control-prev-icon" aria-hidden="true"></span><span class="visually-hidden">上一張</span>
          </button>
          <button class="carousel-control-next" type="button" data-bs-target="#mCarousel" data-bs-slide="next">
            <span class="carousel-control-next-icon" aria-hidden="true"></span><span class="visually-hidden">下一張</span>
          </button>
        </div>
      </div>
      <div class="col-md-6">
        <div class="mb-2 text-muted">${fmt.place(p)}</div>
        <div class="mb-2"><strong>動物：</strong>${fmt.animalLine(p)}</div>
        <div class="mb-2"><strong>毛色：</strong>${p.color || '—'}</div>
        <div class="mb-2"><strong>是否結紮：</strong>${fmt.neuterText(p.neutered)}</div>
        <div class="mb-2"><strong>聯絡方式：</strong>${fmt.contactMethodText(p.contactMethod)}</div>
        <div class="mb-2"><strong>聯絡資訊：</strong>${fmt.contactLine(p)}</div>
        <hr class="my-2">
        <div class="mb-2"><strong>領養人年齡限制：</strong>${fmt.ageLimitText(p.adopterAgeLimit)}</div>
        <div class="mb-2"><strong>是否接受家訪：</strong>${fmt.boolText(p.requireHomeVisit)}</div>
        <div class="mb-2"><strong>是否簽切結書：</strong>${fmt.boolText(p.requireContract)}</div>
        <div class="mb-2"><strong>是否回報領養情況：</strong>${fmt.boolText(p.requireFollowup)}</div>
        <hr class="my-2">
        <div class="mb-1"><strong>其他說明：</strong></div>
        <div class="border rounded p-2 bg-light" style="min-height:80px">${p.description || '—'}</div>
      </div>
    </div>`;

    const pending = p.status === 'pending';
    mApprove.disabled = !pending;
    mReject.disabled = !pending;

    mApprove.onclick = () => updateStatus(id, 'approved', true);
    mReject.onclick = () => updateStatus(id, 'rejected', true);
}

// ---- 修改後的 Admin actions ----
async function updateStatus(id, act, closeModal = false) {
    let reason = '';
    if (act === 'rejected') {
        reason = prompt('退件原因（可留空）') || '';
    }

    let csrfToken = getCsrfToken();

    // 如果沒有 token，嘗試刷新
    if (!csrfToken) {
        console.log('CSRF Token 為空，嘗試刷新...');
        csrfToken = await refreshCsrfToken();
    }

    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken) {
        headers['X-Csrf-Token'] = csrfToken;
        console.log('設定 CSRF Token header:', csrfToken);
    } else {
        console.error('無法獲取 CSRF Token！');
        alert('安全驗證失敗，請重新登入');
        return;
    }

    try {
        console.log('發送請求到:', `/api/posts/${id}/status`);
        console.log('請求 headers:', headers);

        const response = await fetch(`/api/posts/${id}/status?status=${act}&reason=${encodeURIComponent(reason)}`, {
            method: 'PATCH',
            headers: headers
        });

        console.log('Response status:', response.status);
        console.log('Response headers:', [...response.headers.entries()]);

        if (response.status === 403) {
            console.error('403 Forbidden 錯誤');

            // 嘗試讀取錯誤詳情
            try {
                const errorText = await response.text();
                console.error('錯誤詳情:', errorText);
            } catch (e) {
                console.error('無法讀取錯誤詳情');
            }

            alert('操作失敗：權限不足或安全驗證失敗。請確認您有管理員權限並重新登入');
            return;
        }

        const ok = response.ok;
        alert(ok ? '已更新' : `更新失敗 (HTTP ${response.status})`);

        if (ok) {
            if (closeModal && typeof modal !== 'undefined') {
                modal.hide();
            }
            if (typeof load === 'function') {
                load();
            }
        }
    } catch (error) {
        console.error('請求異常:', error);
        alert('請求失敗，請稍後再試');
    }
}

async function adminHold(id, hold) {
    let csrfToken = getCsrfToken();

    if (!csrfToken) {
        csrfToken = await refreshCsrfToken();
    }

    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken) {
        headers['X-Csrf-Token'] = csrfToken;
    } else {
        alert('安全驗證失敗，請重新登入');
        return;
    }

    try {
        const response = await fetch(`/api/posts/${id}/hold?hold=${hold}`, {
            method: 'PATCH',
            headers: headers
        });

        console.log('adminHold response status:', response.status);

        if (response.status === 403) {
            console.error('403 Forbidden - 權限不足');
            alert('操作失敗：權限不足，請確認您有管理員權限');
            return;
        }

        const ok = response.ok;
        alert(ok ? '已更新' : `更新失敗 (HTTP ${response.status})`);

        if (ok && typeof load === 'function') {
            load();
        }
    } catch (error) {
        console.error('adminHold 請求失敗:', error);
        alert('請求失敗，請稍後再試');
    }
}

async function adminClose(id) {
    let csrfToken = getCsrfToken();

    if (!csrfToken) {
        csrfToken = await refreshCsrfToken();
    }

    const headers = {
        'Content-Type': 'application/json'
    };

    if (csrfToken) {
        headers['X-Csrf-Token'] = csrfToken;
    } else {
        alert('安全驗證失敗，請重新登入');
        return;
    }

    try {
        const response = await fetch(`/api/posts/${id}/close`, {
            method: 'PATCH',
            headers: headers
        });

        console.log('adminClose response status:', response.status);

        if (response.status === 403) {
            console.error('403 Forbidden - 權限不足');
            alert('操作失敗：權限不足，請確認您有管理員權限');
            return;
        }

        const ok = response.ok;
        alert(ok ? '已關閉' : `關閉失敗 (HTTP ${response.status})`);

        if (ok && typeof load === 'function') {
            load();
        }
    } catch (error) {
        console.error('adminClose 請求失敗:', error);
        alert('請求失敗，請稍後再試');
    }
}

// ---- events ----
filters.addEventListener('submit', e => {
    e.preventDefault();
    state.status = fStatus.value;
    state.species = fSpecies.value;
    state.q = fQ.value.trim();
    state.page = 0;
    syncUrl(true);
    load();
});

// ---- boot ----
syncUrl(false);
load();