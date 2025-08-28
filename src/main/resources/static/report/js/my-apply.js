// 需登入
const auth = await fetch('/api/auth/status', { credentials: 'include' }).then(r => r.json());
if (!auth?.loggedIn) { sessionStorage.setItem('redirect', location.pathname + location.search); location.href = '/login.html'; }

const list = document.getElementById('list');
const empty = document.getElementById('empty');
const pager = document.getElementById('pager');
const result = document.getElementById('resultCount');

// URL 狀態參數
const qs = new URLSearchParams(location.search);
const state = {
    page: +(qs.get('page') || 0),
    size: 12,
    status: qs.get('status') || 'pending',
};

// 狀態 tabs 高亮
document.querySelectorAll('#tabs-status a').forEach(a => {
    if (a.dataset.status === (state.status || 'all')) a.classList.add('active');
    a.onclick = (e) => { e.preventDefault(); state.status = a.dataset.status; state.page = 0; syncUrl(true); load(); };
});

function buildParams() {
    const p = new URLSearchParams();
    p.set('page', state.page); p.set('size', state.size);
    if (state.status && state.status !== 'all') p.set('status', state.status);
    return p.toString();
}
function syncUrl(push = false) {
    const url = `/my-apply.html?${buildParams()}`;
    if (push) history.pushState(null, '', url); else history.replaceState(null, '', url);
    // 切換高亮
    document.querySelectorAll('#tabs-status a').forEach(a => {
        a.classList.toggle('active', a.dataset.status === (state.status || 'all'));
    });
}

const fallback = '/images/no-image.jpg';
const badge = (s) => ({
    approved: '<span class="badge bg-success badge-status">已通過</span>',
    rejected: '<span class="badge bg-danger  badge-status">已退回</span>',
    cancelled: '<span class="badge bg-secondary badge-status">已取消</span>',
    pending: '<span class="badge bg-warning text-dark badge-status">審核中</span>'
}[s] || `<span class="badge bg-dark">${s}</span>`);

const place = p => [p?.city, p?.district].filter(Boolean).join(' ');
const animalLine = p => [p?.species, p?.breed, p?.sex, p?.age, p?.bodyType].filter(Boolean).join('｜');
const firstImg = p => p?.image1 || p?.image2 || p?.image3 || fallback;
const dt = s => s ? new Date(s).toLocaleString('zh-TW', { hour12: false }) : '';

async function load() {
    const r = await fetch(`/api/my/applications?${buildParams()}`);
    if (!r.ok) { list.innerHTML = ''; empty.classList.remove('d-none'); result.textContent = '讀取失敗'; pager.innerHTML = ''; return; }
    const data = await r.json();
    const items = data.content ?? data ?? [];
    empty.classList.toggle('d-none', items.length > 0);

    result.textContent = (data.totalElements != null)
        ? `共 ${data.totalElements} 筆，第 ${data.number + 1}/${data.totalPages} 頁`
        : `共 ${items.length} 筆`;

    list.innerHTML = items.map(a => {
        const p = a.post || {};
        return `
          <div class="col-12">
            <div class="card shadow-sm">
              <div class="row g-0">
                <div class="col-md-4">
                  <div class="thumb-box bg-contain" style="background-image:url('${firstImg(p)}');"></div>
                </div>
                <div class="col-md-8">
                  <div class="card-body">
                    <h5 class="card-title mb-2 d-flex justify-content-between align-items-center position-relative">
                    <a class="stretched-link text-decoration-none" href="/adopt-view.html?id=${p.id}" target="_blank">
                        ${p.title || '未命名'} │ ${p.breed || p.species || ''}
                    </a>
                    ${badge(a.status)}
                    </h5>

                    <!-- 按鈕區塊加上 action-area -->
                    <div class="d-flex gap-2 action-area">
                    ${a.status === 'pending' ? `<button class="btn btn-outline-danger btn-sm" data-cancel="${a.id}">取消申請</button>` : ``}
                    </div>
                    <div class="small text-muted mb-1">${place(p)}</div>
                    <div class="small mb-2">動物：${animalLine(p) || '—'}</div>
                    <div class="text-muted mb-3">申請時間：${dt(a.createdAt)}</div>
                    <div class="d-flex gap-2">
                      ${a.status === 'pending' ? `<button class="btn btn-outline-danger btn-sm" data-cancel="${a.id}">取消申請</button>` : ``}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>`;
    }).join('');

    // 綁定取消
    list.querySelectorAll('[data-cancel]').forEach(b => {
        b.onclick = async (e) => {
            e.preventDefault();
            e.stopPropagation();                 // 關鍵：不要讓點擊冒泡到 stretched-link
            if (!confirm('確定要取消這筆申請？')) return;
            const ok = await fetch(`/api/applications/${b.dataset.cancel}/cancel`, { method: 'PATCH' }).then(r => r.ok);
            alert(ok ? '已取消' : '取消失敗');
            if (ok) load();
        };
    });

    if (data.totalPages != null) {
        pager.innerHTML = `
          <button class="btn btn-outline-secondary" ${data.number <= 0 ? 'disabled' : ''} id="pgPrev">上一頁</button>
          <button class="btn btn-outline-secondary" ${data.number >= data.totalPages - 1 ? 'disabled' : ''} id="pgNext">下一頁</button>`;
        pager.querySelector('#pgPrev')?.addEventListener('click', () => { state.page = Math.max(0, data.number - 1); syncUrl(true); load(); });
        pager.querySelector('#pgNext')?.addEventListener('click', () => { state.page = Math.min(data.totalPages - 1, data.number + 1); syncUrl(true); load(); });
    } else {
        pager.innerHTML = '';
    }
}

syncUrl();
load();

// === 動態插入「我要回報」按鈕 ===
(function addReportButton() {
  const group = document.getElementById('tabs-cross');
  if (!group) return;

  // 避免重複新增
  if (group.querySelector('[data-role="report-link"]')) return;

  const a = document.createElement('a');
  a.className = 'btn btn-outline-secondary';
  a.href = '/adopt-report.html';  // 第二步可再調整帶 adoptionId
  a.textContent = '我要回報';
  a.setAttribute('data-role', 'report-link');

  group.appendChild(a);
})();






