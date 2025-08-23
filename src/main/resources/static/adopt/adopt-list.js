let currentPage = 0;
const pageSize = 12;

// ====== 性別顯示 ======
function normalizeSex(s) {
  const v = (s ?? '').toString().trim().toLowerCase();
  if (v === 'male' || v === 'm' || v.includes('公')) return 'male';
  if (v === 'female' || v === 'f' || v.includes('母')) return 'female';
  return 'unknown';
}
function sexTextForCard(s) {
  const t = normalizeSex(s);
  return t === 'male' ? '公' : t === 'female' ? '母' : '';
}
function sexIcon(s) {
  const t = normalizeSex(s);
  if (t === 'male') return '<span class="sex-icon"><img src="/images/male.png"></span>';
  if (t === 'female') return '<span class="sex-icon"><img src="/images/female.png"></span>';
  return '';
}
const badge = (srcType) =>
  srcType === 'platform'
    ? '<span class="badge text-bg-primary ms-2">我方救助</span>'
    : '<span class="badge text-bg-warning ms-2">民眾送養</span>';

// ====== 城市/地區資料 ======
const AREA_SOURCES = [
  '/adopt/tw-areas.json',
  'https://cdn.jsdelivr.net/gh/donma/TaiwanAddressCityAreaRoadChinese@master/CityCountyData.json'
];
const normalizeCity = s => (s || '').replace('臺', '台');

async function fetchAreas() {
  for (const url of AREA_SOURCES) {
    try {
      const r = await fetch(url, { cache: 'force-cache' });
      if (!r.ok) continue;
      const data = await r.json();
      if (Array.isArray(data) && data.length && data[0].districts) {
        return data.map(c => ({ name: normalizeCity(c.name), districts: c.districts }));
      } else if (Array.isArray(data) && data.length && data[0].CityName) {
        return data.map(c => ({
          name: normalizeCity(c.CityName),
          districts: (c.AreaList || []).map(a => a.AreaName)
        }));
      }
    } catch { }
  }
  return [];
}

// ====== 主查詢（公開列表固定只看 approved） ======
async function loadPosts() {
  const url = new URL('/adopts', location.origin);
  url.searchParams.set('page', currentPage);
  url.searchParams.set('size', pageSize);
  url.searchParams.set('status', 'approved'); // ★ 只取審核通過的

  const citySel = document.getElementById('cityFilter');
  const distSel = document.getElementById('districtFilter');
  const city = citySel ? citySel.value.trim() : '';
  const district = distSel ? distSel.value.trim() : '';
  const species = (document.getElementById('speciesFilter')?.value || '').trim();
  const sex = (document.getElementById('sexFilter')?.value || '').trim();
  const age = (document.getElementById('ageFilter')?.value || '').trim();
  const keyword = (document.getElementById('keyword')?.value || '').trim();
  const source = (document.getElementById('sourceFilter')?.value || '').trim();

  if (city) url.searchParams.set('city', city);
  if (district) url.searchParams.set('district', district);
  if (species) url.searchParams.set('species', species);
  if (sex) url.searchParams.set('sex', sex);
  if (age) url.searchParams.set('age', age);
  if (keyword) url.searchParams.set('q', keyword);
  if (source) url.searchParams.set('sourceType', source);

  window.showLoading?.();
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();

    document.getElementById('page-info').textContent =
      `第 ${data.number + 1} / ${data.totalPages} 頁`;

    const box = document.getElementById('post-list');
    box.innerHTML = '';

    if (!data.content.length) {
      box.innerHTML = `<div class="col-12 text-center text-muted mt-4">沒有符合條件的貼文</div>`;
      document.getElementById('pagination').innerHTML = '';
      return;
    }

    data.content.forEach(p => {
      box.innerHTML += `
        <div class="col-12 col-sm-6 col-md-4 mb-4">
          <div class="card pet-card h-100">
            <div class="position-relative">
                <img src="${p.image1 || '/images/no-image.jpg'}" class="card-img-top"
                    onerror="this.onerror=null; this.src='/images/no-image.jpg';">
                ${(p.pendingApplications && p.pendingApplications > 0)
          ? `<span class="badge text-bg-info position-absolute top-0 start-0 m-2">
                        申請中 ${p.pendingApplications}
                      </span>`
          : ''}
              </div>
            <div class="card-body">
              <h5 class="card-title">${p.title || ''} ${badge(p.sourceType)}</h5>
              <div class="small text-muted">${p.city || ''} ${p.district || ''}</div>

              <div class="mt-1 small">
                種類：${p.species || ''}　品種：${p.breed || ''}
                ${sexIcon(p.sex)}${sexTextForCard(p.sex)}　年齡：${p.age || ''}
              </div>

              <p class="mt-2 small text-truncate" title="${p.description || ''}">
                ${p.description || ''}
              </p>

              <div class="d-flex align-items-center mt-2">
                <a class="btn btn-outline-secondary ms-2" href="/adopt/adopt-view.html?id=${p.id}">查看</a>
                ${normalizeSex(p.sex) === 'unknown'
          ? `<span class="ms-auto badge rounded-pill bg-light text-secondary border"
                       data-bs-toggle="tooltip" title="刊登者未填寫性別">未提供性別</span>`
          : ''
        }
              </div>
            </div>
          </div>
        </div>`;
    });

    if (window.bootstrap) {
      document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
    }

    const totalPages = data.totalPages, current = data.number;
    const pagination = document.getElementById('pagination');
    pagination.innerHTML = `
      <li class="page-item ${current === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" data-page="0">&laquo;</a></li>
      <li class="page-item ${current === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" data-page="${current - 1}">&lsaquo;</a></li>`;
    const start = Math.max(0, current - 5), end = Math.min(totalPages, current + 6);
    for (let i = start; i < end; i++) {
      pagination.innerHTML += `
        <li class="page-item ${i === current ? 'active' : ''}">
          <a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
    }
    pagination.innerHTML += `
      <li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" data-page="${current + 1}">&rsaquo;</a></li>
      <li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" data-page="${totalPages - 1}">&raquo;</a></li>`;

    document.querySelectorAll("#pagination .page-link").forEach(a => {
      a.addEventListener('click', e => {
        e.preventDefault();
        const p = parseInt(a.getAttribute('data-page'));
        if (!isNaN(p)) { currentPage = p; loadPosts(); window.scrollTo({ top: 0, behavior: 'smooth' }); }
      });
    });

    const gotoBtn = document.getElementById("gotoPageBtn");
    const gotoInput = document.getElementById("gotoPageInput");
    if (gotoBtn && gotoInput) {
      gotoBtn.onclick = () => {
        const val = parseInt(gotoInput.value);
        if (!isNaN(val) && val >= 1 && val <= totalPages) { currentPage = val - 1; loadPosts(); }
        else gotoInput.classList.add("is-invalid");
      };
      gotoInput.oninput = () => gotoInput.classList.remove("is-invalid");
    }
  } catch (err) {
    console.error(err);
    alert('載入失敗');
  } finally {
    window.hideLoading?.();
  }
}

// ====== 初始化 ======
; (async function init() {
  const citySel = document.getElementById('cityFilter');
  const distSel = document.getElementById('districtFilter');
  if (citySel && distSel) {
    const areas = await fetchAreas();
    areas.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.name; opt.textContent = c.name;
      citySel.appendChild(opt);
    });
    citySel.addEventListener('change', () => {
      const city = citySel.value;
      distSel.innerHTML = '<option value="">全部地區</option>';
      distSel.disabled = !city;
      if (!city) return;
      const found = areas.find(a => a.name === city);
      (found?.districts || []).forEach(d => {
        const opt = document.createElement('option');
        opt.value = d; opt.textContent = d;
        distSel.appendChild(opt);
      });
    });
  }

  ['cityFilter', 'districtFilter', 'speciesFilter', 'sexFilter', 'ageFilter', 'sourceFilter']
    .forEach(id => document.getElementById(id)?.addEventListener('change', () => { currentPage = 0; loadPosts(); }));
  document.getElementById('searchBtn')?.addEventListener('click', () => { currentPage = 0; loadPosts(); });
  document.getElementById('keyword')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); currentPage = 0; loadPosts(); }
  });

  loadPosts();
})();
