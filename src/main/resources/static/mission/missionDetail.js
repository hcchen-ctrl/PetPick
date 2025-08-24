// /finalProject/mission/missionDetail.js
const fallbackImg = '/images/dog1.jpg';

$(async function () {
  try { Realtime.Presence.startPing(CURRENT_USER_ID); } catch (e) { console.warn('[realtime] Presence.startPing fail', e); }

  // 2) 載入任務
  const params = new URLSearchParams(location.search);
  const missionId = Number(params.get("id"));
  if (!missionId) { alert("缺少任務 ID"); return; }

  try {
    const m = await $.getJSON(`/api/missions/${missionId}`);
    renderMissionDetail(m);
    setupPosterInfo(m.poster);

    // 3) 訂閱發文者在線狀態（用 presence.js）
    if (m?.poster?.posterId != null) {
      try {
        const stop = await Realtime.Presence.subscribe(m.poster.posterId, (isOnline) => {
          $('#userStatus').text(isOnline ? '🟢 在線' : '⚪ 離線');
        });
        // 離頁取消訂閱（可選）
        $(window).on('unload', stop);
      } catch (e) {
        console.warn('[realtime] Presence.subscribe fail', e);
        $('#userStatus').text('⚪ 離線');
      }
    }

    // 自己的任務就隱藏申請鈕；否則綁申請事件
    if (Number(m.poster?.posterId) === CURRENT_USER_ID) {
      hideApply();
    } else {
      wireApply(m);
    }
  } catch (err) {
    console.error(err);
    alert("載入任務資料失敗，請稍後再試。");
  }
});

// ====== 申請 → 建立/取得聊天室 → 導到聊天室 ======
function wireApply(mission) {
  const $btn = $('#btn-apply');
  if ($btn.length === 0) return;

  $btn.on('click', async function () {
    if (!confirm('確認送出申請？')) return;
    setBusy(true);

    try {
      const url = `/api/applications?missionId=${mission.missionId}&applicantId=${CURRENT_USER_ID}`;
      const res = await fetch(url, { method: 'POST' });
      const body = await safeText(res);

      if (res.ok || /already|duplicate/i.test(body)) {
        await goChat(mission.missionId, CURRENT_USER_ID);
        return;
      }

      if (res.status === 409 && /matched|accepted/i.test(body)) {
        alert('任務已配對完成'); hideApply(); return;
      }

      alert('申請失敗：' + body);
      setBusy(false);
    } catch (e) {
      console.error(e);
      alert('申請失敗，請稍後再試');
      setBusy(false);
    }
  });

  function setBusy(b) {
    $btn.prop('disabled', b).html(
      b ? `<span class="spinner-border spinner-border-sm me-1"></span>送出中`
        : `請求接取任務`
    );
  }
}

async function goChat(missionId, applicantId) {
  const r = await fetch(`/api/chat/conversations?missionId=${missionId}&applicantId=${applicantId}`, { method: 'POST' });
  if (!r.ok) { alert('建立對話失敗'); return; }
  const cid = await r.json();
  location.href = `/mission/chat.html?conversationId=${cid}`;
}

// ===== 渲染 / 發文者資訊 / 小工具 =====
function renderMissionDetail(m) {
  $('#PetPick-title, #title').text(m.title ?? '');
  $('#petName').text(m.petName ?? '');
  $('#petAge').text(m.petAge ?? '');
  $('#petGender').text(m.petGender ?? '');
  $('#phone').text(m.contactPhone ?? '');
  $('#location').text(`${m.city ?? ''} ${m.district ?? ''}`.trim());
  $('#time').text(`${formatTime(m.startTime)} ~ ${formatTime(m.endTime)}`);
  $('#price').text(m.price ?? '');
  $('#description').text(m.description ?? '');
  $('#tag').text(Array.isArray(m.tags) && m.tags.length ? m.tags.map(t => `#${t}`).join(' ') : '無標籤');

  const imgs = Array.isArray(m.imageUrls) && m.imageUrls.length ? m.imageUrls : [fallbackImg];

  $('#carouselImages').html(
    imgs.map((url, i) => `
      <div class="carousel-item ${i === 0 ? 'active' : ''}" data-index="${i}">
        <img src="${url}" class="d-block w-100" style="height:700px;object-fit:cover;" onerror="this.src='${fallbackImg}'">
      </div>
    `).join('')
  );

  $('#imageThumbnails').html(
    imgs.map((url, i) => `
      <img src="${url}" class="img-thumbnail mx-1 thumb" data-idx="${i}"
           style="height:80px;width:80px;object-fit:cover;cursor:pointer"
           onerror="this.src='${fallbackImg}'">
    `).join('')
  );

  // 縮圖點擊切換
  $('#imageThumbnails').off('click', '.thumb').on('click', '.thumb', function () {
    const idx = Number($(this).data('idx'));
    changeMainImage(idx);
  });
}

function setupPosterInfo(poster) {
  if (!poster) return;
  $('#posterName').text(poster.name ?? 'User');
  const $avatar = $('#posterAvatar');
  $avatar.attr('src', poster.avatarUrl || fallbackImg)
         .on('error', () => $avatar.attr('src', fallbackImg));
  $('#posterEmail').text(poster.email ?? '');
  $('#posterLocation').text(poster.location ?? '');
  $('#missionCount').text(poster.missionCount ?? 0);
  $('#replyRate').text(poster.replyRate ?? '尚未回覆');

  // 初始顯示（真正狀態會由 Presence.subscribe 推進來）
  $('#userStatus').text('⚪ 離線');
}

function changeMainImage(index) {
  $('#carouselImages .carousel-item').removeClass('active').eq(index).addClass('active');
}

function formatTime(str) {
  if (!str) return '';
  const d = new Date(str);
  return d.toLocaleString('zh-TW', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false
  });
}

function hideApply() { $('#btn-apply').hide(); }
async function safeText(res) { try { return await res.text(); } catch { return ''; } }