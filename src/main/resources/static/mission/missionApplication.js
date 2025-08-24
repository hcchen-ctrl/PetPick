const API = {
  ownerMissions: `/api/owners/${CURRENT_USER_ID}/missions`,
  ownerApps:     `/api/applications/me/owner?userId=${CURRENT_USER_ID}`,      // 收到的申請
  myApplied:     `/api/applications/me/applicant?userId=${CURRENT_USER_ID}`, // 我申請的
  accept:  (appId)=> `/api/applications/${appId}/status?ownerId=${CURRENT_USER_ID}&status=accepted`,
  reject:  (appId)=> `/api/applications/${appId}/status?ownerId=${CURRENT_USER_ID}&status=rejected`,
  cancel:  (appId)=> `/api/applications/${appId}?applicantId=${CURRENT_USER_ID}`,
  delMission: (mid)=> `/api/missions/${mid}?posterId=${CURRENT_USER_ID}`
};

let $container;
let MY_MISSIONS = []; // MissionOwnerItemDTO[]
let OWNER_APPS  = []; // owner申請 ApplicationItemDTO[]
let MY_APPS     = []; // my申請的 ApplicationItemDTO[]

// ===== 入口 =====
$(function () {
  $container = $('#applicationList');
  $container.empty();
  init();
});

function init() {
  // 以 jQuery 的 Deferred 併發三隻 API
  const d1 = $.getJSON(API.ownerMissions);
  const d2 = $.getJSON(API.ownerApps);
  const d3 = $.getJSON(API.myApplied);

  $.when(d1, d2, d3)
    .done((mRes, oRes, aRes) => {
      // $.when 回傳的是 [data, textStatus, jqXHR]
      MY_MISSIONS = Array.isArray(mRes[0]) ? mRes[0] : [];
      OWNER_APPS  = Array.isArray(oRes[0]) ? oRes[0] : [];
      MY_APPS     = Array.isArray(aRes[0]) ? aRes[0] : [];

      renderTabs();
      renderAll();
    })
    .fail((jqXHR) => {
      console.error(jqXHR);
      $container.html('<div class="text-danger">載入失敗</div>');
    });
}

// 進行中 = 有待審且尚未有人被接受
function getOngoing() {
  return (MY_MISSIONS || []).filter(m => (toInt(m.pendingCount) > 0) && !toBool(m.hasAccepted));
}
function getAll() { return MY_MISSIONS || []; }
function getApplied() { return MY_APPS || []; }

// ===== Tabs =====
function renderTabs() {
  $container.html(`
    <div class="d-flex gap-2 mb-3">
      <button id="tab-all"      class="btn btn-sm btn-dark">全部任務（${getAll().length}）</button>
      <button id="tab-ongoing"  class="btn btn-sm btn-outline-dark">進行中（${getOngoing().length}）</button>
      <button id="tab-applied"  class="btn btn-sm btn-outline-dark">申請的任務（${getApplied().length}）</button>
    </div>
    <div id="board"></div>
  `);

  $('#tab-all').on('click', function(){
    setActive($(this), $('#tab-ongoing'), $('#tab-applied'));
    renderAll();
  });
  $('#tab-ongoing').on('click', function(){
    setActive($(this), $('#tab-all'), $('#tab-applied'));
    renderOngoing();
  });
  $('#tab-applied').on('click', function(){
    setActive($(this), $('#tab-all'), $('#tab-ongoing'));
    renderApplied();
  });
}

function setActive($activeBtn, ...others) {
  $activeBtn.attr('class', 'btn btn-sm btn-dark');
  others.forEach($b => $b.attr('class', 'btn btn-sm btn-outline-dark'));
}

// ===== 三個面板 =====
function renderAll(){ renderMissionList(getAll()); }
function renderOngoing(){ renderMissionList(getOngoing(), true); }
function renderApplied(){
  const $box = $('#board');
  const list = getApplied();
  if(list.length === 0){ $box.html(emptyApplied()); return; }
  $box.empty();
  list.forEach(a => $box.append(createAppliedCard(a)));
}

function renderMissionList(list){
  const $box = $('#board');
  if(!list || list.length === 0){
    $box.html(emptyAll());
    return;
  }
  $box.empty();
  list.forEach(m => $box.append(createOwnerMissionCard(m)));
}

// ===== 我發佈的 =====
function createOwnerMissionCard(m){
  const $card = $('<div class="card mb-3"></div>');
  const cover = m.imageUrl || '/images/default-avatar.png';
  const tags  = (m.tags && m.tags.length) ? m.tags.map(t => `#${t}`).join(' ') : '無標籤';
  const pending  = toInt(m.pendingCount);
  const accepted = toBool(m.hasAccepted);
  const badgeClass = accepted ? 'bg-success' : (pending > 0 ? 'bg-warning' : 'bg-secondary');
  const badgeText  = accepted ? '已配對' : (pending > 0 ? '待審中' : '未有申請');

  const missionApplicants = (OWNER_APPS || []).filter(a => String(a.missionId) === String(m.missionId));

  $card.html(`
    <div class="card-body">
      <div class="d-flex">
        <img src="${cover}" alt="封面" class="me-3"
             style="width:160px;height:120px;object-fit:cover;border-radius:8px"
             onerror="this.src='/images/default-avatar.png'">
        <div class="flex-grow-1">
          <div class="d-flex justify-content-between align-items-start">
            <h5 class="mb-1">${esc(m.title)}</h5>
            <span class="badge ${badgeClass}">${badgeText}</span>
          </div>
          <div class="text-muted small mb-1">
            地點：${esc(m.city)} ${esc(m.district)}　時間：${fmt(m.startTime)} ~ ${fmt(m.endTime)}
          </div>
          <div class="text-muted small mb-2">
            報酬：$${m.price}　申請數：<span class="badge bg-dark">${toInt(m.applyCount)}</span>
          </div>
          <div class="mb-2"><span class="mission-tag">${tags}</span></div>

          <div class="d-flex gap-2 mb-2 justify-content-end">
            <button class="btn btn-sm btn-outline btn-toggle-apps" style="background-color: burlywood;">查看申請者</button>
            <a class="btn btn-sm btn-outline-secondary"
              href="/finalProject/mission/missionDetail.html?id=${m.missionId}">查看</a>
            <button class="btn btn-sm btn-outline-danger btn-del" data-mid="${m.missionId}">刪除</button>
          </div>

          <div class="apps-panel border rounded p-2" style="display:none">
            ${
              missionApplicants.length === 0
                ? `<div class="text-muted small">尚無申請者</div>`
                : missionApplicants.map(renderApplicantRow).join('')
            }
          </div>
        </div>
      </div>
    </div>
  `);

  // 展開/收合
  $card.find('.btn-toggle-apps').on('click', function () {
    const $p = $card.find('.apps-panel');
    $p.toggle();
  });

  // 刪除任務
  $card.find('.btn-del').on('click', async function(){
    const mid = $(this).data('mid');
    if(!confirm('確定刪除此任務？此動作無法復原')) return;
    $.ajax({ url: API.delMission(mid), method: 'DELETE' })
      .done(() => {
        MY_MISSIONS = MY_MISSIONS.filter(x => String(x.missionId) !== String(mid));
        OWNER_APPS  = OWNER_APPS.filter(x => String(x.missionId) !== String(mid));
        renderTabs(); renderAll();
      })
      .fail(jq => alert('刪除失敗：' + (jq.responseText || '')));
  });

  // 申請者列中的 accept/reject
  $card.find('.btn-accept').on('click', function(){
    ownerDecision($(this).data('appId'), 'accepted', $card, m.missionId);
  });
  $card.find('.btn-reject').on('click', function(){
    ownerDecision($(this).data('appId'), 'rejected', $card, m.missionId);
  });

  return $card;
}

function renderApplicantRow(a){
  const disable = (a.status !== 'pending') ? 'disabled' : '';
  const badge = `<span class="badge ${statusClass(a.status)} me-2">${statusText(a.status)}</span>`;
  return `
    <div class="d-flex align-items-center justify-content-between py-1 border-bottom small" data-app-id="${a.applicationId}">
      <div>
        ${badge}
        <strong>${esc(a.applicantName)}</strong>　
        電話：${esc(a.contactPhone || '')}　
        申請時間：${fmt(a.applyTime)}
      </div>
      <div class="d-flex gap-2">
        <button class="btn btn-sm btn-success btn-accept" data-app-id="${a.applicationId}" ${disable}>同意</button>
        <button class="btn btn-sm btn-outline-danger btn-reject" data-app-id="${a.applicationId}" ${disable}>拒絕</button>
      </div>
    </div>
  `;
}

function ownerDecision(appId, action, $cardEl, missionId){
  if(!confirm(action === 'accepted' ? '確定同意此申請？' : '確定拒絕此申請？')) return;

  const url = (action === 'accepted') ? API.accept(appId) : API.reject(appId);
  $.ajax({ url, method: 'PATCH' })
    .done(() => {
      // 更新 OWNER_APPS 快取
      OWNER_APPS = OWNER_APPS.map(a =>
        a.applicationId === +appId ? { ...a, status: action } : a
      );

      // 列上標籤 & 按鈕
      const $row = $cardEl.find(`[data-app-id="${appId}"]`);
      if($row.length){
        const $badge = $row.find('.badge');
        $badge.attr('class', `badge ${statusClass(action)}`).text(statusText(action));
        $row.find('button').prop('disabled', true);
      }

      // 若同意，將此任務標為「已配對」，並把其他 pending 的行為鎖住
      if(action === 'accepted'){
        const $badgeTop = $cardEl.find('.badge.bg-warning, .badge.bg-secondary, .badge.bg-success').first();
        if($badgeTop.length){ $badgeTop.attr('class','badge bg-success').text('已配對'); }
        $cardEl.find('.btn-accept, .btn-reject').prop('disabled', true);

        // 同步到 MY_MISSIONS
        MY_MISSIONS = MY_MISSIONS.map(m =>
          String(m.missionId) === String(missionId) ? { ...m, hasAccepted: true, pendingCount: 0 } : m
        );
        // 重新計數「進行中」tab
        $('#tab-ongoing').text(`進行中（${getOngoing().length}）`);
      } else {
        // 拒絕：該任務 pendingCount -1
        MY_MISSIONS = MY_MISSIONS.map(m =>
          String(m.missionId) === String(missionId)
            ? { ...m, pendingCount: Math.max(0, toInt(m.pendingCount) - 1) }
            : m
        );
        $('#tab-ongoing').text(`進行中（${getOngoing().length}）`);

        // 若已無待審，更新頂部徽章
        const m = MY_MISSIONS.find(x => String(x.missionId) === String(missionId));
        if(m){
          const $badgeTop = $cardEl.find('.badge.bg-warning').first();
          if($badgeTop.length && toInt(m.pendingCount) === 0){
            $badgeTop.attr('class','badge bg-secondary').text('未有申請');
          }
        }
      }
    })
    .fail(jq => {
      console.error(jq);
      alert('操作失敗：' + (jq.responseText || ''));
    });
}

// ===== 我申請的 =====
function createAppliedCard(app){
  const $card = $('<div class="card mb-3"></div>');
  const sTxt = statusText(app.status), sCls = statusClass(app.status);
  const cancelBtn = (app.status === 'pending') ? `<button class="btn btn-sm text-white btn-cancel" style="background-color:rgb(219,120,120)">取消申請</button>` : '';

  $card.html(`
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-start">
        <h5 class="mb-1">🐾 ${esc(app.missionTitle)}</h5>
        <span class="badge ${sCls}">${sTxt}</span>
      </div>
      <div class="text-muted small mb-2">
        申請時間：${fmt(app.applyTime)}　對方：${esc(app.ownerName)}　電話：${esc(app.contactPhone || '')}
      </div>
      <div class="d-flex gap-2 justify-content-end">
        <a class="btn btn-sm btn-outline-secondary" href="/finalProject/mission/missionDetail.html?id=${app.missionId}">查看任務</a>
        ${cancelBtn}
      </div>
    </div>
  `);

  $card.find('.btn-cancel').on('click', function(){
    if(!confirm('確定取消這筆申請？')) return;
    $.ajax({ url: API.cancel(app.applicationId), method: 'DELETE' })
      .done(() => {
        MY_APPS = MY_APPS.filter(x => x.applicationId !== app.applicationId);
        $('#tab-applied').text(`申請的任務（${getApplied().length}）`);
        renderApplied();
      })
      .fail(jq => {
        console.error(jq);
        alert('取消失敗：' + (jq.responseText || ''));
      });
  });

  return $card;
}

// ===== 顯示文案 / 小工具 =====
function emptyAll(){
  return `<div class="text-center text-muted py-5">目前沒有任務<br><a href="/finalProject/mission/missionUpload.html" class="btn btn-sm btn-primary mt-2">去發布任務</a></div>`;
}
function emptyApplied(){
  return `<div class="text-center text-muted py-5">你尚未申請任何任務</div>`;
}

function statusText(s){ if(s==='accepted')return'同意'; if(s==='pending')return'等待對方回覆'; return'取消'; }
function statusClass(s){ if(s==='accepted')return'bg-success'; if(s==='pending')return'bg-warning'; return'bg-danger'; }

function fmt(s){
  if(!s) return '';
  const d = new Date(String(s).replace(' ','T'));
  const pad = n => String(n).padStart(2,'0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
function esc(str){ if(str==null)return''; return String(str).replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m])); }
function toInt(n){ return Number.isFinite(+n)?+n:0; }
function toBool(v){ return String(v)==='true'||v===true||v===1; }
