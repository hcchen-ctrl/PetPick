// ===== 基本設定 =====
const qs = new URLSearchParams(location.search);

const API = {
  listConvs: (uid) => `/api/chat/conversations?userId=${uid}`,
  createConv: (mid, appId) => `/api/chat/conversations?missionId=${mid}&applicantId=${appId}`,
  listMsgs: (cid, uid) => `/api/chat/conversations/${cid}/messages?userId=${uid}`,
  markRead: (cid, uid) => `/api/chat/conversations/${cid}/read?userId=${uid}`
};

// ===== DOM =====
const $matchListEl = $("#matchList");
const $chatNameEl  = $("#chatName");
const $chatMsgsEl  = $("#chatMessages");
const $inputEl     = $("#msgInput").length ? $("#msgInput") : $(".card-footer input");
const $sendBtn     = $("#sendBtn").length ? $("#sendBtn") : $(".card-footer button");

// ===== 狀態 =====
let CONVS = [];
let currentConvId = null;
let currUnsub = null;   // 目前會話的取消訂閱函式

// 進入聊天室頁面就把全域紅點關掉
$("#chatBubbleBadge-unread").addClass("d-none");
$("#chatBubbleBadge").removeClass("d-none");

// ===== 入口 =====
$(async function () {
  if (!Realtime?.isReady?.()) {
    console.warn('[chat] Realtime not ready yet, continue with UI…');
  }

  // 解析參數：直接指定 conversationId 或由 missionId+applicantId 建立/取得
  const missionId    = qs.get("missionId");
  const applicantId  = qs.get("applicantId");
  const conversationId = qs.get("conversationId");

  if (conversationId) {
    currentConvId = Number(conversationId);
  } else if (missionId && applicantId) {
    const id = await $.ajax({ url: API.createConv(missionId, applicantId), method: 'POST' });
    currentConvId = Number(id);
    history.replaceState(null, '', `${location.pathname}?conversationId=${currentConvId}`);
  }

  await loadConversations();

  if (currentConvId) {
    await openConversation(currentConvId);
  } else if (CONVS.length) {
    await openConversation(CONVS[0].conversationId);
  } else {
    $chatMsgsEl.html('<p class="text-muted text-center my-4">尚無對話</p>');
  }
});

// ===== 清單 =====
async function loadConversations() {
  CONVS = await $.getJSON(API.listConvs(CURRENT_USER_ID)) || [];
  $matchListEl.html('<a href="#" class="list-group-item"><strong>最新消息</strong></a>');
  $matchListEl.find('a').on('click', e => e.preventDefault());

  CONVS.forEach(c => {
    const $li = $(`
      <li class="list-group-item d-flex align-items-center conv-item" data-cid="${c.conversationId}">
        <img src="${c.otherAvatarUrl || 'https://picsum.photos/40/40'}" class="rounded-circle me-2" width="36" height="36" onerror="this.src='https://picsum.photos/40/40'">
        <div class="flex-grow-1">
          <strong>${esc(c.otherName)}　<small class="text-muted">(${esc(c.missionTitle)})</small></strong><br>
          <small class="text-muted last-preview">${esc(c.lastMessage || '')}</small>
        </div>
        ${c.unreadCount > 0 ? `<span class="badge bg-danger ms-2 unread">${c.unreadCount}</span>` : ''}
      </li>`);
    $li.on('click', () => openConversation(c.conversationId));
    $matchListEl.append($li);

    // 讓 realtime.js 幫你監聽每個會話（用於側欄預覽/紅點）
    Realtime.subscribeConversationLite?.(c.conversationId, (ev) => {
      if (!isMessage(ev) || String(ev.senderId) === String(CURRENT_USER_ID)) return;
      if (String(c.conversationId) === String(currentConvId)) return; // 目前已開啟，不顯紅點
      const $row = $(`#matchList .conv-item[data-cid='${c.conversationId}']`);
      $row.find('.last-preview').text(ev.content || '');
      if (!$row.find('.unread').length) $row.append('<span class="badge bg-danger ms-2 unread">1</span>');
      $("#chatBubbleBadge-unread").removeClass("d-none");
      $("#chatBubbleBadge").addClass("d-none");
      console.log('[lite] unread bump on', c.conversationId, ev);
    });
  });
}

// ===== 打開會話 =====
async function openConversation(cid) {
  currentConvId = cid;
  highlightActive(cid);

  // 清側欄紅點
  $(`#matchList .conv-item[data-cid='${cid}'] .unread`).remove();

  const conv = CONVS.find(x => String(x.conversationId) === String(cid));
  $chatNameEl.text(conv ? conv.otherName : '聊天室');

  // 任務摘要
  await renderMissionChip(conv);

  // 歷史訊息
  $chatMsgsEl.children(':not(:first)').remove();
  const msgs = await $.getJSON(API.listMsgs(cid, CURRENT_USER_ID));
  (msgs || []).forEach(m => appendMessageRow(m.messageId, m.senderId, m.senderName, m.content, m.createdAt));
  $chatMsgsEl.scrollTop($chatMsgsEl.prop('scrollHeight'));

  // 標記已讀（REST + WS）
  try { await $.post(API.markRead(cid, CURRENT_USER_ID)); } catch {}
  Realtime.sendRead?.(cid, CURRENT_USER_ID);

  // 解除舊訂閱，訂閱目前會話
  if (typeof currUnsub === 'function') { currUnsub(); currUnsub = null; }
  currUnsub = Realtime.subscribeConversation?.(cid, async (ev) => {
    console.log('[curr]', ev);
    // 輸入中
    if (ev.type === 'typing' && String(ev.senderId) !== String(CURRENT_USER_ID)) {
      const base = conv?.otherName || '聊天室';
      $chatNameEl.text(`${base}（輸入中…）`);
      clearTimeout(openConversation._t);
      openConversation._t = setTimeout(() => $chatNameEl.text(base), 1200);
      return;
    }
    // 已讀（對方讀我最後一則）
    if (ev.type === 'read' && String(ev.userId) !== String(CURRENT_USER_ID)) {
      $('.msg-row.me:last .meta').each(function () {
        const t = $(this).text();
        if (!/已讀/.test(t)) $(this).text(`${t}  已讀`);
      });
      return;
    }
    // 新訊息
    if (isMessage(ev)) {
      appendMessageRow(ev.messageId, ev.senderId, ev.senderName, ev.content, ev.createdAt);
      // 更新側欄預覽
      $(`#matchList .conv-item[data-cid='${cid}'] .last-preview`).text(ev.content || '');
      // 我在此會話視窗 -> 立即回覆已讀
      if (document.visibilityState === 'visible' && String(ev.senderId) !== String(CURRENT_USER_ID)) {
        try { await $.post(API.markRead(cid, CURRENT_USER_ID)); } catch {}
        Realtime.sendRead?.(cid, CURRENT_USER_ID);
      }
      // 隨著訊息捲到底
      $chatMsgsEl.scrollTop($chatMsgsEl.prop('scrollHeight'));
    }
  });

  // 綁發送
  bindSend();
}

function bindSend() {
  // 解除舊 handler，避免重複送兩次
  $sendBtn.off('click').on('click', doSend);
  let composing = false;
  $inputEl.off('compositionstart compositionend keydown input')
    .on('compositionstart', () => { composing = true; })
    .on('compositionend',  () => { composing = false; })
    .on('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        if (composing) return;
        e.preventDefault();
        doSend();
      }
    })
    .on('input', () => {
      if (!currentConvId) return;
      Realtime.sendTyping?.(currentConvId, CURRENT_USER_ID);
    });
}

function doSend() {
  const text = String($inputEl.val() || '').trim();
  if (!text || !currentConvId) return;
  Realtime.sendMessage?.({ conversationId: currentConvId, senderId: CURRENT_USER_ID, content: text });
  // 樂觀插入自己訊息
  $(`#matchList .conv-item[data-cid='${currentConvId}'] .last-preview`).text(text);
  $inputEl.val('');
}

// ===== UI & 工具 =====
async function renderMissionChip(conv) {
  const mid = conv?.missionId;
  const title = conv?.missionTitle || '';
  const cover = await fetchMissionCover(mid);
  const $chip = $(`
    <div class="mb-3 d-flex justify-content-center">
      <div style="display:flex;align-items:center;gap:10px;background:#fff;border:1px solid #eee;border-radius:10px;padding:8px 12px;max-width:600px;width:100%;">
        <img src="${cover}" style="width:48px;height:48px;object-fit:cover;border-radius:6px" onerror="this.src='https://picsum.photos/64/64'">
        <div class="flex-grow-1">
          <div class="small text-muted">任務</div>
          <div class="fw-semibold" style="line-height:1.3">${esc(title)}</div>
        </div>
        ${mid ? `<a class="btn btn-sm btn-outline-secondary" href="/finalProject/mission/missionDetail.html?id=${mid}">查看任務</a>` : ''}
      </div>
    </div>`);
  $chatMsgsEl.empty().append($chip);
}
async function fetchMissionCover(mid) {
  try {
    if (!mid) return 'https://picsum.photos/64/64';
    const m = await $.getJSON(`/api/missions/${mid}`);
    return m.imageUrl || (Array.isArray(m.imageUrls) && m.imageUrls[0]) || 'https://picsum.photos/64/64';
  } catch { return 'https://picsum.photos/64/64'; }
}
function appendMessageRow(messageId, senderId, senderName, content, createdAt) {
  if (!content?.trim()) return;
  const isMe = String(senderId) === String(CURRENT_USER_ID);
  const $row = $('<div/>').addClass(`msg-row ${isMe ? 'me' : 'other'}`).css({ marginBottom:'10px', textAlign:isMe?'right':'left' }).attr('data-id', messageId || '');
  const $bubble = $('<div/>').css({
    display:'inline-block',maxWidth:'min(72%,560px)',lineHeight:'1.5',
    padding:'10px 12px',borderRadius:'12px',wordBreak:'break-word',whiteSpace:'pre-wrap',overflowWrap:'anywhere',
    backgroundColor: isMe ? 'burlywood' : 'white'
  }).text(content);
  const $meta = $('<small/>').addClass('meta text-muted d-block').css({marginTop:'4px',textAlign:isMe?'right':'left'})
    .text(`${isMe?'你':(senderName||'')}　${fmtTime(createdAt)}`);
  $row.append($bubble,$meta); $('#chatMessages').append($row);
}
function highlightActive(cid) {
  $("#matchList .conv-item").each(function(){ $(this).toggleClass('active', String($(this).data('cid'))===String(cid)); });
}
function esc(s){return String(s||'').replace(/[&<>\"']/g,m=>({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[m]));}
function pad(n){return String(n).padStart(2,'0');}
function fmtTime(iso){const d=new Date(iso);return `${pad(d.getHours())}:${pad(d.getMinutes())}`;}
function isMessage(ev){ return (!!ev?.content && String(ev.content).trim().length>0) || ev?.messageId!=null; }