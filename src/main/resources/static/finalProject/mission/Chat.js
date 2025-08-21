// ===== 基本設定 =====
const urlq = new URLSearchParams(location.search);
let CURRENT_USER_ID = Number(urlq.get('userId')) || 101;
const API = {
  listConvs: (uid) => `/api/chat/conversations?userId=${uid}`,
  createConv: (mid, appId) => `/api/chat/conversations?missionId=${mid}&applicantId=${appId}`,
  listMsgs: (cid, uid) => `/api/chat/conversations/${cid}/messages?userId=${uid}`,
  markRead: (cid, uid) => `/api/chat/conversations/${cid}/read?userId=${uid}`
};

let STOMP = null;          // STOMP client via SockJS
let STOMP_SUB = null;      // {dot, slash} subscriptions for current conversation
let RECONNECT_TIMER = null;
// —— Realtime monitor / WS status —— //
let WS_STATE = 'idle';
function baseChatTitle() {
  const cid = currentConvId;
  const conv = (CONVS || []).find(x => String(x.conversationId) === String(cid));
  return conv?.otherName || '聊天室';
}
function setWsState(state) {
  WS_STATE = state;
  try {
    const base = baseChatTitle();
    let suffix = '';
    if (state === 'connecting') suffix = '（連線中…）';
    else if (state === 'reconnecting') suffix = '（重連中…）';
    else if (state === 'offline') suffix = '（離線）';
    $chatNameEl.text(base + suffix);
  } catch (_) { }
}
// 全局訂閱：維護所有會話的訂閱（用於即時更新側欄與紅點）
const GLOBAL_SUBS = new Map();
function topicToCid(destination) {
  // 支援 /topic/conversations.8 與 /topic/conversations/8
  const m = String(destination || '').match(/conversations[./](\d+)/);
  return m ? Number(m[1]) : null;
}
function ensureGlobalSubs() {
  if (!STOMP || !STOMP.connected) return;
  try { console.log('[WS] ensureGlobalSubs (connected)', CONVS?.length || 0); } catch (e) { }
  setWsState('online');
  (CONVS || []).forEach(c => {
    const cid = c.conversationId;
    if (!cid || GLOBAL_SUBS.has(cid)) return;
    const dot = STOMP.subscribe(`/topic/conversations.${cid}`, onGlobalMessage);
    const slash = STOMP.subscribe(`/topic/conversations/${cid}`, onGlobalMessage);
    GLOBAL_SUBS.set(cid, { dot, slash });
    try { console.log('[SUB global]', cid); } catch (e) { }
  });
}
function onGlobalMessage(frame) {
  // 任何會話的推播（包含目前沒打開的）都會到這裡
  try { console.log('[WS][GLOBAL]', frame?.headers?.destination, frame?.body); } catch (e) { }
  let m = {};
  try { m = JSON.parse(frame.body || '{}'); } catch { return; }
  const cid = topicToCid(frame.headers?.destination) ?? m.conversationId;
  if (!cid) return;

  const isMessage = (m.content != null && String(m.content).length > 0) || (m.messageId != null);
  if (!isMessage) return;

  // 來自我的訊息不觸發紅點
  const fromMe = String(m.senderId) === String(CURRENT_USER_ID);

  // 更新側欄最後訊息
  const conv = CONVS.find(x => String(x.conversationId) === String(cid));
  if (conv && m.content) conv.lastMessage = m.content;
  const $li = $(`#matchList .conv-item[data-cid='${cid}']`);
  $li.find('.last-preview').text(m.content || $li.find('.last-preview').text());

  // 若不是目前打開的會話，且不是我發的，顯示紅點 +1
  if (String(cid) !== String(currentConvId) && !fromMe) {
    const $badge = $li.find('.unread');
    if ($badge.length) {
      const n = parseInt($badge.text(), 10) || 0; $badge.text(n + 1);
    } else {
      $li.append(`<span class="badge bg-danger ms-2 unread">1</span>`);
    }
    // 顯示全域紅點
    $("#chatBubbleBadge-unread").removeClass("d-none");
    $("#chatBubbleBadge").addClass("d-none");
  }
}


function unsubscribeCurrent() {
  try { if (STOMP_SUB) { STOMP_SUB.dot?.unsubscribe(); STOMP_SUB.slash?.unsubscribe(); } } catch (_) { }
  STOMP_SUB = null;
}

// ===== DOM =====
const $matchListEl = $("#matchList");
const $chatNameEl = $("#chatName");
const $chatMsgsEl = $("#chatMessages");
const $inputEl = $("#msgInput").length ? $("#msgInput") : $(".card-footer input");
const $sendBtn = $("#sendBtn").length ? $("#sendBtn") : $(".card-footer button");
function setSendEnabled(on) { if ($sendBtn.length) $sendBtn.prop('disabled', !on); }
setSendEnabled(false);

let CONVS = [];
let currentConvId = null;

// ===== 清除聊天室紅點 =====
$(function () {
  $("#chatBubbleBadge-unread").addClass("d-none");
  $("#chatBubbleBadge").removeClass("d-none");
});

// ===== 入口 =====
$(async function init() {
  // 先建立 WS 連線（僅連線，不訂閱特定會話）
  await setupStomp(null);
  try {
    const u = new URL(location.href);
    const conversationId = u.searchParams.get("conversationId");
    const missionId = u.searchParams.get("missionId");
    const applicantId = u.searchParams.get("applicantId");

    if (conversationId) {
      currentConvId = Number(conversationId);
    } else if (missionId && applicantId) {
      const data = await $.ajax({ url: API.createConv(missionId, applicantId), method: 'POST' });
      currentConvId = Number(data);
    }

    await loadConversations();
    // 建立全域訂閱（側欄與紅點即時）
    ensureGlobalSubs();

    if (currentConvId) {
      await openConversation(currentConvId);
      highlightActive(currentConvId);
    } else if (CONVS.length) {
      await openConversation(CONVS[0].conversationId);
      highlightActive(CONVS[0].conversationId);
    } else {
      $chatMsgsEl.html('<p class="text-muted text-center my-4">尚無對話</p>');
    }
  } catch (err) {
    console.error(err);
    $chatMsgsEl.html('<p class="text-danger">載入對話失敗</p>');
  }
});

// ===== 載入對話清單 =====
async function loadConversations() {
  CONVS = await $.getJSON(API.listConvs(CURRENT_USER_ID)) || [];
  $matchListEl.html('<a href="#" class="list-group-item" aria-current="true"><strong>最新消息</strong></a>');
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
    $li.on('click', async () => { await openConversation(c.conversationId); highlightActive(c.conversationId); });
    $matchListEl.append($li);
  });

  // 確保全局訂閱已建立（可即時更新側欄與紅點）
  ensureGlobalSubs();
}

// ===== 開啟對話（任務摘要 + 訊息 + 訂閱 + 已讀） =====
async function openConversation(cid) {
  currentConvId = cid;
  const conv = CONVS.find(x => String(x.conversationId) === String(cid));

  // 側欄清除未讀
  $(`#matchList .conv-item[data-cid='${cid}'] .unread`).remove();

  $chatMsgsEl.empty();
  $chatNameEl.text(conv ? conv.otherName : '聊天室');

  // 更新網址
  try {
    const u = new URL(location.href);
    u.searchParams.set('conversationId', String(cid));
    u.searchParams.set('userId', String(CURRENT_USER_ID));
    history.replaceState(null, '', u.toString());
  } catch (_) { }

  // 任務摘要
  await renderMissionChip(conv);

  // 歷史訊息
  await loadMessages(cid);

  // 標記已讀（REST）
  try { await $.post(API.markRead(cid, CURRENT_USER_ID)); } catch (_) { }

  // 前端即時標示最後一則我方訊息為已讀
  (function markLastMineRead() {
    const $lastMeta = $('.msg-row.me:last .meta');
    if ($lastMeta.length) {
      const t = $lastMeta.text();
      if (!/已讀/.test(t)) $lastMeta.text(`${t}  ✓已讀`);
    }
  })();

  // 切換訂閱
  unsubscribeCurrent();
  if (RECONNECT_TIMER) { clearTimeout(RECONNECT_TIMER); RECONNECT_TIMER = null; }
  setSendEnabled(false);

  await setupStomp(cid);
  const base = CONVS.find(x => x.conversationId === cid)?.otherName || '聊天室';
  $chatNameEl.text(base);
}

async function renderMissionChip(conv) {
  const missionId = conv?.missionId;
  const missionTitle = conv?.missionTitle || '';
  const cover = await fetchMissionCover(missionId);

  const $chip = $(`
    <div class="mb-3 d-flex justify-content-center">
      <div style="display:flex;align-items:center;gap:10px;background:#fff;border:1px solid #eee;border-radius:10px;padding:8px 12px;max-width:600px;width:100%;">
        <img src="${cover}" alt="封面" style="width:48px;height:48px;object-fit:cover;border-radius:6px" onerror="this.src='https://picsum.photos/64/64'">
        <div class="flex-grow-1">
          <div class="small text-muted">任務</div>
          <div class="fw-semibold" style="line-height:1.3">${esc(missionTitle)}</div>
        </div>
        ${missionId ? `<a class="btn btn-sm btn-outline-secondary" href="/finalProject/mission/missionDetail.html?id=${missionId}">查看任務</a>` : ''}
      </div>
    </div>`);
  $chatMsgsEl.append($chip);
}

async function fetchMissionCover(mid) {
  try {
    if (!mid) return 'https://picsum.photos/64/64';
    const m = await $.getJSON(`/api/missions/${mid}`);
    return m.imageUrl || (Array.isArray(m.imageUrls) && m.imageUrls.length ? m.imageUrls[0] : 'https://picsum.photos/64/64');
  } catch (e) {
    return 'https://picsum.photos/64/64';
  }
}

// ===== 讀取訊息 =====
async function loadMessages(cid) {
  $chatMsgsEl.children(':not(:first)').remove();
  try {
    const msgs = await $.getJSON(API.listMsgs(cid, CURRENT_USER_ID));
    (msgs || []).forEach(m => appendMessageRow(m.messageId, m.senderId, m.senderName, m.content, m.createdAt));
    try { void $chatMsgsEl[0].offsetHeight; } catch (_) { }
    $chatMsgsEl.scrollTop($chatMsgsEl.prop('scrollHeight'));
  } catch {
    $chatMsgsEl.append('<p class="text-danger">載入訊息失敗</p>');
  }
}

// ===== 連線/訂閱 =====
function setupStomp(cid) {
  return new Promise((resolve) => {
    if (STOMP && STOMP.connected) {
      if (cid != null) {
        STOMP_SUB = {
          dot: STOMP.subscribe(`/topic/conversations.${cid}`, onStompMessage),
          slash: STOMP.subscribe(`/topic/conversations/${cid}`, onStompMessage)
        };
        try { console.log('[SUB current]', cid); } catch (e) { }
      }
      setSendEnabled(true);
      ensureGlobalSubs();
      return resolve();
    }
    const sock = new SockJS('/ws');
    STOMP = Stomp.over(sock);
    STOMP.debug = null;
    // heartbeats (ms)
    if (STOMP.heartbeat) {
      STOMP.heartbeat.outgoing = 10000; // ping server every 10s
      STOMP.heartbeat.incoming = 10000; // expect server ping every 10s
    }
    setWsState('connecting');
    // monitor low-level close/error
    try {
      sock.onclose = () => { setWsState('offline'); };
      sock.onerror = () => { setWsState('offline'); };
    } catch (_) { }
    STOMP.connect({}, () => {
      setWsState('online');
      try { console.log('[WS] CONNECTED'); } catch (e) { }
      if (cid != null) {
        STOMP_SUB = {
          dot: STOMP.subscribe(`/topic/conversations.${cid}`, onStompMessage),
          slash: STOMP.subscribe(`/topic/conversations/${cid}`, onStompMessage)
        };
        try { console.log('[SUB current]', cid); } catch (e) { }
      }
      setSendEnabled(true);
      ensureGlobalSubs();
      resolve();
    }, () => {
      try { console.warn('[WS] connection lost, scheduling reconnect…'); } catch (e) { }
      setWsState('reconnecting');
      setSendEnabled(false);
      if (String(cid) !== String(currentConvId)) return resolve();
      if (RECONNECT_TIMER) clearTimeout(RECONNECT_TIMER);
      RECONNECT_TIMER = setTimeout(() => {
        setWsState('reconnecting');
        setupStomp(cid).then(resolve);
      }, 2000);
    });
  });
}
//===== 持續訂閱 ==== //
function startRealtimeMonitor() {

  setInterval(() => {
    if (!STOMP || !STOMP.connected) {
      setWsState('offline');
      return;
    }
    setWsState('online');

    ensureGlobalSubs();

    if (currentConvId && !STOMP_SUB) {
      try {
        STOMP_SUB = {
          dot: STOMP.subscribe(`/topic/conversations.${currentConvId}`, onStompMessage),
          slash: STOMP.subscribe(`/topic/conversations/${currentConvId}`, onStompMessage)
        };
      } catch (_) { }
    }
  }, 5000);

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      if (STOMP && STOMP.connected) {
        setWsState('online');
        ensureGlobalSubs();
        if (currentConvId && !STOMP_SUB) {
          try {
            STOMP_SUB = {
              dot: STOMP.subscribe(`/topic/conversations.${currentConvId}`, onStompMessage),
              slash: STOMP.subscribe(`/topic/conversations/${currentConvId}`, onStompMessage)
            };
          } catch (_) { }
        }
      } else {
        setWsState('offline');
      }
    }
  }, { passive: true });
}
$(startRealtimeMonitor);

function onStompMessage(frame) {
  try { console.log('[WS][CURR]', frame?.headers?.destination, frame?.body); } catch (e) { }
  let m = {};
  try { m = JSON.parse(frame.body || '{}'); } catch { m = {}; }

  // 1) 解析這則訊息屬於哪個會話
  const dest = frame?.headers?.destination || '';
  const cidFromHeader = topicToCid(dest);
  const cid = cidFromHeader ?? m.conversationId;
  if (!cid) return;

  setWsState('online');

  // 2) 若不是目前打開的會話，改交給全域處理（更新側欄預覽、紅點）
  if (String(cid) !== String(currentConvId)) {
    onGlobalMessage(frame);
    return;
  }

  // 3) 目前會話才處理 typing / read / 訊息
  if (m.type === 'typing' && String(m.senderId) !== String(CURRENT_USER_ID)) {
    const base = CONVS.find(x => x.conversationId === currentConvId)?.otherName || '聊天室';
    $chatNameEl.text(`${base}（輸入中…）`);
    clearTimeout(onStompMessage._typingClear);
    onStompMessage._typingClear = setTimeout(() => $chatNameEl.text(base), 1500);
    return;
  }

  if (m.type === 'read' && String(m.userId) !== String(CURRENT_USER_ID)) {
    // 把我方最後一則訊息加上已讀 ✓
    $('.msg-row.me:last .meta').each(function () {
      const t = $(this).text();
      if (!/已讀/.test(t)) $(this).text(`${t}  已讀`);
    });
    return;
  }

  // 4) 一般訊息（無論是否帶 m.type，只要有內容或 messageId 就視為訊息）
  const isMessage = (m.content != null && String(m.content).trim().length > 0) || (m.messageId != null);
  if (isMessage) {
    const insert = () => {
      appendMessageRow(m.messageId, m.senderId, m.senderName, m.content, m.createdAt || new Date().toISOString());

      // 同步刷新側欄預覽
      const conv = CONVS.find(x => String(x.conversationId) === String(cid));
      if (conv) conv.lastMessage = m.content;
      const $prev = $(`#matchList .conv-item[data-cid='${cid}'] .last-preview`);
      $prev.text(m.content || '');
    };
    if (window.requestAnimationFrame) requestAnimationFrame(insert); else insert();
  } else {
    try { console.warn('[WS] 未識別或無內容的事件，忽略', m); } catch (e) { }
  }
}

// ===== 插入訊息 =====
function appendMessageRow(messageId, senderId, senderName, content, createdAt) {
  if (!content || String(content).trim().length === 0) return;
  const isMe = String(senderId) === String(CURRENT_USER_ID);

  const $row = $('<div/>')
    .addClass(`msg-row ${isMe ? 'me' : 'other'}`)
    .css({ marginBottom: '10px', textAlign: isMe ? 'right' : 'left' })
    .attr('data-id', messageId || '');

  const $bubble = $('<div/>')
    .css({
      display: 'inline-block', verticalAlign: 'top',
      maxWidth: 'min(72%, 560px)', lineHeight: '1.5',
      padding: '10px 12px', borderRadius: '12px',
      wordBreak: 'break-word', whiteSpace: 'pre-wrap', overflowWrap: 'anywhere',
      backgroundColor: isMe ? 'burlywood' : 'white'
    }).text(content || '');

  const $time = $('<small/>')
    .addClass('meta text-muted d-block')
    .css({ marginTop: '4px', textAlign: isMe ? 'right' : 'left' })
    .text(`${isMe ? '你' : (senderName || '')}　${fmtTime(createdAt)}`);

  $row.append($bubble, $time);
  const $wrap = $('#chatMessages');
  $wrap.append($row);

  // —— 自動捲底（僅在靠近底部時） —— //
  const nearBottom = ($wrap.prop('scrollHeight') - $wrap.scrollTop() - $wrap.innerHeight()) < 80;
  if (nearBottom) $wrap.scrollTop($wrap.prop('scrollHeight'));
}

// ===== 發送訊息 / Typing =====
$sendBtn.on('click', doSend);
let composing = false;
$inputEl.on('compositionstart', () => { composing = true; });
$inputEl.on('compositionend', () => { composing = false; });
$inputEl.on('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    if (composing) return;        // 中文避免多送
    e.preventDefault();
    doSend();
  }
});

let sending = false;
async function doSend() {
  const text = ($inputEl.val() || '').trim();
  if (text.length > 2000) { alert('訊息過長'); return; }
  if (!text || !currentConvId || sending) return;
  if (!STOMP || !STOMP.connected) { alert('連線未建立，請稍後再試'); return; }

  try {
    sending = true;
    $inputEl.val('');
    const payload = { conversationId: currentConvId, senderId: CURRENT_USER_ID, content: text };
    try { console.log('[SEND]', payload); } catch (e) { }
    STOMP.send('/app/chat.send', {}, JSON.stringify(payload));

    const conv = CONVS.find(x => String(x.conversationId) === String(currentConvId));
    if (conv) conv.lastMessage = text;
    $(`#matchList .conv-item[data-cid='${currentConvId}'] .last-preview`).text(text);
  } catch (err) {
    console.error(err);
    alert('傳送失敗');
  } finally {
    sending = false;
  }
}

// Typing
let typingTimer = null;
$inputEl.on('input', () => {
  if (!STOMP || !STOMP.connected || !currentConvId) return;
  if (typingTimer) return;
  STOMP.send('/app/chat.typing', {}, JSON.stringify({ conversationId: currentConvId, senderId: CURRENT_USER_ID }));
  typingTimer = setTimeout(() => { typingTimer = null; }, 1000);
});

// ===== 小工具 =====
function highlightActive(cid) {
  $("#matchList .conv-item").each(function () {
    const on = String($(this).data('cid')) === String(cid);
    $(this).toggleClass('active', on);
  });
}
function esc(str) { return String(str || '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[m])); }
function pad(n) { return String(n).padStart(2, '0'); }
function fmtTime(iso) { const d = new Date(iso); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; }