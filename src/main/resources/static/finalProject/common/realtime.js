// /finalProject/common/realtime.js
;(function (w) {
  const S = {
    userId: null,
    stomp: null,
    connecting: false,
    subs: new Map(),          // cid -> {dot, slash}
    convIds: new Set(),
    selUnread: '#chatBubbleBadge-unread',
    selNormal: '#chatBubbleBadge',
    selBtn: '#chatBubbleBtn'
  };

  function showUnread() {
    $(S.selUnread).removeClass('d-none');
    $(S.selNormal).addClass('d-none');
  }
  function hideUnread() {
    $(S.selUnread).addClass('d-none');
    $(S.selNormal).removeClass('d-none');
  }

  function ensureConn(cb) {
    if (S.stomp && S.stomp.connected) return cb(S.stomp);
    if (S.connecting) return setTimeout(() => ensureConn(cb), 200);
    S.connecting = true;

    const sock = new SockJS('/ws');
    const stomp = Stomp.over(sock);
    stomp.debug = null;

    stomp.connect({}, () => {
      S.stomp = stomp; S.connecting = false;
      cb(stomp);
    }, (e) => { console.warn('[Realtime] connect fail', e); S.connecting = false; });
  }

  function topicToCid(dest) {
    const m = String(dest||'').match(/conversations[./](\d+)/);
    return m ? Number(m[1]) : null;
  }

  function subscribeAll() {
    if (!S.stomp || !S.stomp.connected) return;
    S.convIds.forEach(cid => {
      if (S.subs.has(cid)) return;
      const dot = S.stomp.subscribe(`/topic/conversations.${cid}`, onGlobal);
      const slash = S.stomp.subscribe(`/topic/conversations/${cid}`, onGlobal);
      S.subs.set(cid, { dot, slash });
      console.log('[Realtime][SUB]', cid);
    });
  }

  function onGlobal(frame) {
    // 任何會話有新訊息（且不是自己發的） => 顯示紅點
    let ev = {};
    try { ev = JSON.parse(frame.body || '{}'); } catch {}
    const cid = topicToCid(frame.headers?.destination) ?? ev.conversationId;
    const isMsg = (ev.messageId != null) || (ev.content && String(ev.content).trim().length>0);
    const fromMe = String(ev.senderId) === String(S.userId);
    if (cid && isMsg && !fromMe) {
      showUnread();
      w.dispatchEvent(new CustomEvent('chat:new-message', { detail: ev }));
    }
  }

  async function refreshBadgeFromServer() {
    try {
      const res = await fetch(`/api/chat/conversations?userId=${S.userId}`);
      if (!res.ok) throw new Error(await res.text());
      const list = await res.json();
      const anyUnread = Array.isArray(list) && list.some(c => Number(c?.unreadCount) > 0);
      if (anyUnread) showUnread(); else hideUnread();
      // 更新已知會話清單
      (list || []).forEach(c => { if (c?.conversationId != null) S.convIds.add(Number(c.conversationId)); });
    } catch (e) {
      console.warn('[Realtime] refresh badge fail', e);
    }
  }

  async function loadMyConvs() {
    return refreshBadgeFromServer();
  }

  const Realtime = {
    init(userId, opts = {}) {
      S.userId = userId;
      if (opts.unreadSelector) S.selUnread = opts.unreadSelector;
      if (opts.normalSelector) S.selNormal = opts.normalSelector;
      if (opts.chatButtonSelector) S.selBtn = opts.chatButtonSelector;

      // hideUnread();                       // 進站先清（移除，進站不自動清紅點）
      $(document).on('click', S.selBtn, hideUnread);  // 點按鈕清紅點

      ensureConn(async () => {
        await refreshBadgeFromServer();  // 依後端未讀數決定是否顯示紅點
        subscribeAll();
      });
    },
    chatBadge: {
      show: showUnread,
      clear: hideUnread
    }
  };

  w.Realtime = Realtime;
})(window);