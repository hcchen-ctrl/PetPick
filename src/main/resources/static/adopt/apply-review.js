import { requireAdmin } from '/js/auth.js';
await requireAdmin();

const list = document.getElementById('list');
const empty = document.getElementById('empty');
const filters = document.getElementById('filters');
const fStatus = document.getElementById('fStatus');
const fSpecies = document.getElementById('fSpecies');
const fQ = document.getElementById('fQ');
const resultCount = document.getElementById('resultCount');
const pager = document.getElementById('pager');

const m = new bootstrap.Modal('#detailModal');
const mTitle = document.getElementById('mTitle');
const mBody = document.getElementById('mBody');
const mApprove = document.getElementById('mApprove');
const mReject = document.getElementById('mReject');

const qs = new URLSearchParams(location.search);
const state = {
    page: +(qs.get('page') || 0),
    size: 24,
    status: qs.get('status') || 'pending',
    species: qs.get('species') || '',
    q: qs.get('q') || ''
};

fStatus.value = ['pending', 'approved', 'rejected', 'cancelled'].includes(state.status) ? state.status : 'all';
fSpecies.value = state.species;
fQ.value = state.q;

const fallback = '/images/no-image.jpg';
const fmt = {
    badge(s) {
        if (s === 'approved') return '<span class="badge bg-success badge-status">已通過</span>';
        if (s === 'rejected') return '<span class="badge bg-danger badge-status">已退回</span>';
        if (s === 'cancelled') return '<span class="badge bg-secondary badge-status">已取消</span>';
        return '<span class="badge bg-warning text-dark badge-status">審核中</span>';
    },
    place(p) { return [p.city, p.district].filter(Boolean).join(' '); },
    sexText(s) { return s === 'male' ? '公' : s === 'female' ? '母' : '—'; },
    animalLine(p) {
        return [p.species, p.breed, [this.sexText(p.sex)].filter(Boolean).join(' '), p.age, p.bodyType].filter(Boolean).join('｜');
    }
};

function buildParams() {
    const p = new URLSearchParams();
    const status = fStatus.value;    // all / pending / approved / rejected...
    const species = fSpecies.value;  // all / dog / cat / ...
    const q = (fQ.value || '').trim();

    if (status && status !== 'all' && status !== '全部') p.set('status', status);
    if (species && species !== 'all' && species !== '全部') p.set('species', species);
    if (q) p.set('q', q);

    p.set('page', state.page);
    p.set('size', state.size);
    return p.toString();
}

function syncUrl(push = false) {
    const url = `/apply-review.html?${buildParams()}`;
    if (push) history.pushState(null, '', url); else history.replaceState(null, '', url);
}

window.addEventListener('popstate', () => {
    const q = new URLSearchParams(location.search);
    state.page = +(q.get('page') || 0);
    state.status = q.get('status') || 'pending';
    state.species = q.get('species') || '';
    state.q = q.get('q') || '';
    fStatus.value = ['pending', 'approved', 'rejected', 'cancelled'].includes(state.status) ? state.status : 'all';
    fSpecies.value = state.species; fQ.value = state.q;
    load();
});

async function load() {
    const r = await fetch(`/api/applications?${buildParams()}`);
    if (!r.ok) { list.innerHTML = ''; empty.classList.remove('d-none'); resultCount.textContent = '讀取失敗'; pager.innerHTML = ''; return; }
    const data = await r.json();
    const items = data.content ?? data ?? [];

    empty.classList.toggle('d-none', items.length > 0);
    resultCount.textContent = (data.totalElements != null)
        ? `共 ${data.totalElements} 筆，第 ${data.number + 1}/${data.totalPages} 頁`
        : `共 ${items.length} 筆`;

    list.innerHTML = items.map(a => {
        const p = a.post || {};
        const img = p.image1 || p.image2 || p.image3 || fallback;
        return `
          <div class="col-12 col-md-6 col-lg-4">
            <div class="card h-100">
              <div class="card-thumb"><img src="${img}" onerror="this.src='${fallback}'"></div>
              <div class="card-body">
                <h5 class="mb-1 d-flex align-items-center justify-content-between">
                  <span>${p.title || '未命名'}</span>
                  ${fmt.badge(a.status)}
                </h5>
                <div class="text-muted small mb-1">${fmt.place(p)}</div>
                <div class="small mb-1">動物：${fmt.animalLine(p)}</div>
                <div class="small mb-1">申請者：${a.applicantName || a.applicantId}</div>
                <div class="small mb-2">留言：${a.message || '—'}</div>
                <div class="d-flex flex-wrap gap-2">
                  <button class="btn btn-outline-primary btn-sm" data-view="${a.id}">詳情</button>
                  ${a.status === 'pending' ? `
                    <button class="btn btn-success btn-sm" data-pass="${a.id}">通過</button>
                    <button class="btn btn-danger btn-sm"  data-reject="${a.id}">退回</button>` : ``}
                </div>
              </div>
            </div>
          </div>`;
    }).join('');

    list.querySelectorAll('[data-view]').forEach(b => b.onclick = () => openDetail(b.dataset.view));
    list.querySelectorAll('[data-pass]').forEach(b => b.onclick = () => approveApp(b.dataset.pass));
    list.querySelectorAll('[data-reject]').forEach(b => b.onclick = () => rejectApp(b.dataset.reject));

    if (data.totalPages != null) {
        pager.innerHTML = `
          <button class="btn btn-outline-secondary" ${data.number <= 0 ? 'disabled' : ''} id="pgPrev">上一頁</button>
          <button class="btn btn-outline-secondary" ${data.number >= data.totalPages - 1 ? 'disabled' : ''} id="pgNext">下一頁</button>`;
        pager.querySelector('#pgPrev')?.addEventListener('click', () => { state.page = Math.max(0, data.number - 1); syncUrl(true); load(); });
        pager.querySelector('#pgNext')?.addEventListener('click', () => { state.page = Math.min(data.totalPages - 1, data.number + 1); syncUrl(true); load(); });
    } else { pager.innerHTML = ''; }
}

async function openDetail(id) {
    mTitle.textContent = '申請詳情';
    mBody.innerHTML = `<div class="text-center text-muted">載入中…</div>`;
    m.show();

    const r = await fetch(`/api/applications/${id}`);
    if (!r.ok) { mBody.innerHTML = `<div class="text-danger">讀取失敗</div>`; return; }
    const a = await r.json();
    const p = a.post || {};
    const imgs = [p.image1, p.image2, p.image3].filter(u => !!u && u.trim());
    const useImgs = imgs.length ? imgs : [fallback];
    const slides = useImgs.map((u, i) => `<div class="carousel-item ${i === 0 ? 'active' : ''}"><img src="${u}" class="d-block w-100 carousel-img" onerror="this.src='${fallback}'"></div>`).join('');
    const indicators = useImgs.map((_, i) => `<button type="button" data-bs-target="#mCarousel" data-bs-slide-to="${i}" class="${i === 0 ? 'active' : ''}" ${i === 0 ? 'aria-current="true"' : ''} aria-label="第 ${i + 1} 張"></button>`).join('');

    mTitle.innerHTML = `申請詳情　${fmt.badge(a.status)}`;
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
            <hr class="my-2">
            <div class="mb-2"><strong>申請者：</strong>${a.applicantName || a.applicantId}</div>
            <div class="mb-2"><strong>申請時間：</strong>${(a.createdAt || '').replace('T', ' ')}</div>
            <div class="mb-2"><strong>留言：</strong>${a.message || '—'}</div>
          </div>
        </div>`;

    const pending = a.status === 'pending';
    mApprove.disabled = !pending;
    mReject.disabled = !pending;
    mApprove.onclick = () => approveApp(id, true);
    mReject.onclick = () => rejectApp(id, true);
}

// --- Admin actions for Applications ---
async function approveApp(id, close = false) {
    const ok = await fetch(`/api/applications/${id}/approve`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已通過' : '操作失敗'); if (ok) { if (close) m.hide(); load(); }
}
async function rejectApp(id, close = false) {
    const reason = prompt('退件原因（可留空）') || '';
    const ok = await fetch(`/api/applications/${id}/reject?reason=${encodeURIComponent(reason)}`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已退回' : '操作失敗'); if (ok) { if (close) m.hide(); load(); }
}

filters.addEventListener('submit', e => {
    e.preventDefault();
    state.status = fStatus.value; state.species = fSpecies.value; state.q = fQ.value.trim(); state.page = 0;
    syncUrl(true); load();
});

syncUrl();
load();