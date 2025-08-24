$(function(){
  // ===== DOM =====
  const $missionList     = $('#mission-list');
  const $recommendList   = $('#recommend-list');
  const $showAllBtn      = $('#showAllBtn');
  const $showRecommendBtn= $('#showRecommendBtn');

  const $typeSelect   = $('select.form-select').eq(0);
  const $regionSelect = $('select.form-select').eq(1);
  const $searchForm   = $('#search-input').closest('form');
  const $searchInput  = $('#search-input');

  // ===== 常數 =====
  const API_LIST = (uid) => `/api/missions?userId=${encodeURIComponent(uid)}`;
  const FALLBACK_IMG = '/images/dog1.jpg';

  // ===== 狀態 =====
  let ALL = [];
  let VIEW = [];

  // ===== 區塊切換 =====
  $showAllBtn.on('click', () => {
    $('#all-missions').show();
    $('#recommend-list').hide();
    $showAllBtn.addClass('active');
    $showRecommendBtn.removeClass('active');
  });
  $showRecommendBtn.on('click', () => {
    $('#all-missions').hide();
    $('#recommend-list').css('display','flex');
    $showRecommendBtn.addClass('active');
    $showAllBtn.removeClass('active');
  });

  // ===== 初始化 =====
  init();
  function init(){
    $missionList.html('<p class="text-muted">載入中...</p>');
    $recommendList.empty();
    const uid = CURRENT_USER_ID;
    $.getJSON(API_LIST(uid))
      .done(data => {
        ALL = data || [];
        VIEW = ALL.slice();
        hydrateFilters(ALL);
        render();
      })
      .fail((xhr)=>{
        console.error('載入任務失敗', xhr);
        $missionList.html('<p class="text-danger">載入任務失敗</p>');
      });
  }

  // ===== 篩選 =====
  function filterMissions() {
    const kw = ($searchInput.val() || '').trim().toLowerCase();
    const type = $typeSelect.val();     // e.g. "遛狗" 或 "任務類型"
    const region = $regionSelect.val(); // e.g. "台中市" 或 "地區"

    VIEW = (ALL || []).filter(m => {
      const inKw = !kw || [m.title, m.city, m.district, ...((m.tags)||[])]
        .filter(v=>v!=null)
        .some(v => String(v).toLowerCase().includes(kw));

      const inType = (type === '任務類型') || (((m.tags)||[]).includes(type));
      const inRegion = (region === '地區') || (m.city && m.city.indexOf(region) !== -1);

      return inKw && inType && inRegion;
    });

    render();
  }

  // 動態填入篩選選項
  function hydrateFilters(list) {
    // 類型
    const tagSet = new Set();
    (list||[]).forEach(m => ((m.tags)||[]).forEach(t => tagSet.add(t)));
    resetSelect($typeSelect, '任務類型', Array.from(tagSet));

    // 地區
    const citySet = new Set((list||[]).map(m => m.city).filter(Boolean));
    resetSelect($regionSelect, '地區', Array.from(citySet));
  }

  function resetSelect($select, placeholder, items) {
    $select.empty();
    $('<option/>', { text: placeholder, value: placeholder, selected: true }).appendTo($select);
    (items||[]).forEach(v => $('<option/>', { text: v, value: v }).appendTo($select));
  }

  // 監聽
  $typeSelect.on('change', filterMissions);
  $regionSelect.on('change', filterMissions);
  $searchForm.on('submit', e => { e.preventDefault(); filterMissions(); });
  $searchInput.on('input', debounce(filterMissions, 250));

  // ===== 渲染 =====
  function render() {
    $missionList.empty();
    $recommendList.empty();

    (VIEW||[]).forEach(m => {
      $missionList.append(createMissionCard(m));
      const s = Number.isFinite(+m.score) ? +m.score : 0;
      if (s >= 70) $recommendList.append(createMissionCard(m));
    });
  }

  function createMissionCard(mission) {
    const $card = $('<div/>', { class: 'col-12 mb-4' });

    const score = Number.isFinite(+mission.score) ? Math.round(+mission.score) : 0;
    const scoreStyle =
      score >= 90 ? 'background-color:rgb(112,190,88);' :
      score >= 70 ? 'background-color:rgb(218,203,107);' :
      score >= 50 ? 'background-color:rgb(219,120,120);' : 'background-color:#cfcfcf;';

    const img = mission.imageUrl || FALLBACK_IMG;
    const tagText = Array.isArray(mission.tags) && mission.tags.length ? mission.tags.join('、') : '未標註';

    $card.html(`
      <div class="d-flex border shadow-sm p-3 align-items-start" style="border-radius: 15px;">
        <img src="${img}" alt="任務圖片"
             onerror="this.src='${FALLBACK_IMG}'"
             style="width:250px;height:200px;object-fit:cover" class="me-3">
        <div class="flex-grow-1">
          <div class="d-flex justify-content-between align-items-center">
            <h3 class="fw-bold mb-2">${mission.title}</h3>
            <span class="score-circle" style="${scoreStyle}">${score}</span>
          </div>
          <p class="mb-1 text-muted">地點：${mission.city ?? ''} ${mission.district ?? ''}</p>
          <p class="mb-1 text-muted">時間：${fmt(mission.startTime)} ~ ${fmt(mission.endTime)}</p>
          <p class="mb-1 text-muted">酬勞：$${mission.price ?? 0}</p>
          <div class="d-flex justify-content-between align-items-center mt-5">
            <span class="mission-tag">#${tagText}</span>
            <a class="btn btn-md" style="background-color:burlywood" href="/mission/missionDetail.html?id=${mission.missionId}">查看任務</a>
          </div>
        </div>
      </div>
    `);
    return $card;
  }

  function fmt(iso){
    if(!iso) return '';
    const d = new Date(iso);
    const mm = String(d.getMonth()+1).padStart(2,'0');
    const dd = String(d.getDate()).padStart(2,'0');
    const hh = String(d.getHours()).padStart(2,'0');
    const mi = String(d.getMinutes()).padStart(2,'0');
    return `${mm}/${dd} ${hh}:${mi}`;
  }

  // debounce
  function debounce(fn, delay=300){
    let id; return function(){
      const ctx = this, args = arguments;
      clearTimeout(id);
      id = setTimeout(function(){ fn.apply(ctx, args); }, delay);
    };
  }
});
