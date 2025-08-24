// /finalProject/common/realtime.js
;(function (w) {
    const S = {
        userId: null,
        stomp: null,
        connecting: false,
        subs: new Map(),          // cid -> {dot, slash}
        handlers: new Map(),      // cid -> Set<fn(ev)>
        convIds: new Set(),
        selUnread: '#chatBubbleBadge-unread',
        selNormal: '#chatBubbleBadge',
        selBtn: '#chatBubbleBtn',
        currentCid: null,
        refreshTimer: null
    };

    // ---------- Badge helpers ----------
    function showUnread() {
        $(S.selUnread).removeClass('d-none');
        $(S.selNormal).addClass('d-none');
    }
    function hideUnread() {
        $(S.selUnread).addClass('d-none');
        $(S.selNormal).removeClass('d-none');
    }

    // ---------- Connection management ----------
    function ensureConn(cb) {
        if (S.stomp && S.stomp.connected) return cb(S.stomp);
        if (S.connecting) return setTimeout(() => ensureConn(cb), 200);
        S.connecting = true;

        const sock = new SockJS('/ws');
        const stomp = Stomp.over(sock);
        stomp.debug = null;

        // auto-reconnect on close
        sock.onclose = () => {
            console.warn('[Realtime] socket closed, retrying...');
            S.stomp = null;
            S.connecting = false;
            setTimeout(() => ensureConn((_st) => {
                subscribeAll();
                refreshBadgeFromServer();
            }), 800);
        };

        stomp.connect({}, () => {
            S.stomp = stomp;
            S.connecting = false;
            subscribeAll();
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

    // ---------- Inbound events ----------
    function onGlobal(frame) {
        let ev = {};
        try { ev = JSON.parse(frame.body || '{}'); } catch {}
        const cid = topicToCid(frame.headers?.destination) ?? ev.conversationId;

        // Forward to page-level handlers (if any)
        if (cid && S.handlers.has(cid)) {
            for (const fn of S.handlers.get(cid)) {
                try { fn(ev); } catch (e) { console.warn('[Realtime] handler error', e); }
            }
        }

        // Any message (not from me) -> show red dot when not currently viewing that convo or tab unfocused
        const isMsg = (ev.messageId != null) || (ev.content && String(ev.content).trim().length>0);
        const fromMe = String(ev.senderId) === String(S.userId);
        if (cid && isMsg && !fromMe) {
            if (cid !== S.currentCid || !document.hasFocus()) showUnread();
            w.dispatchEvent(new CustomEvent('chat:new-message', { detail: ev }));
        }
    }

    // ---------- Server sync for unread ----------
    async function refreshBadgeFromServer() {
        try {
            const res = await fetch(`/api/chat/conversations?userId=${S.userId}`);
            if (!res.ok) throw new Error(await res.text());
            const list = await res.json();
            const anyUnread = Array.isArray(list) && list.some(c => Number(c?.unreadCount) > 0);
            if (anyUnread) showUnread(); else hideUnread();
            // update known conversations
            (list || []).forEach(c => { if (c?.conversationId != null) S.convIds.add(Number(c.conversationId)); });
            subscribeAll();
        } catch (e) {
            console.warn('[Realtime] refresh badge fail', e);
        }
    }

    // Periodic refresh as safety net
    function startRefreshTimer() {
        if (S.refreshTimer) clearInterval(S.refreshTimer);
        S.refreshTimer = setInterval(refreshBadgeFromServer, 60000); // 60s
    }

    // ---------- Public API ----------
    const Realtime = {
        init(userId, opts = {}) {
            S.userId = userId;
            if (opts.unreadSelector) S.selUnread = opts.unreadSelector;
            if (opts.normalSelector) S.selNormal = opts.normalSelector;
            if (opts.chatButtonSelector) S.selBtn = opts.chatButtonSelector;

            // Clicking the chat button clears badge
            $(document).on('click', S.selBtn, hideUnread);

            ensureConn(async () => {
                await refreshBadgeFromServer();
                subscribeAll();
                startRefreshTimer();
            });

            // If tab becomes hidden then visible, recheck unread
            document.addEventListener('visibilitychange', () => {
                if (document.visibilityState === 'visible') refreshBadgeFromServer();
            });
        },

        // The chat page should call this when entering/leaving a conversation
        setCurrentConversationId(cid) {
            S.currentCid = (cid != null) ? Number(cid) : null;
        },

        // Ensure we subscribe to a specific conversation and attach an optional handler
        subscribeConversation(cid, handler) {
            cid = Number(cid);
            S.convIds.add(cid);
            if (handler) {
                if (!S.handlers.has(cid)) S.handlers.set(cid, new Set());
                S.handlers.get(cid).add(handler);
            }
            ensureConn(() => subscribeAll());
            return () => { // unsubscribe function for the handler only
                if (handler && S.handlers.has(cid)) {
                    S.handlers.get(cid).delete(handler);
                }
            };
        },

        // STOMP sends
        sendMessage({ conversationId, senderId, content }) {
            ensureConn(st => {
                const payload = { conversationId: Number(conversationId), senderId: Number(senderId), content: String(content||'') };
                st.send('/app/chat.send', {}, JSON.stringify(payload));
                console.log('[Realtime][SEND message]', payload);
            });
        },
        sendTyping(conversationId, senderId) {
            ensureConn(st => {
                st.send('/app/chat.typing', {}, JSON.stringify({ conversationId: Number(conversationId), senderId: Number(senderId) }));
            });
        },
        sendRead(conversationId, userId) {
            ensureConn(st => {
                st.send('/app/chat.read', {}, JSON.stringify({ conversationId: Number(conversationId), userId: Number(userId) }));
                // optimistic: clear badge if reading current conv
                if (Number(conversationId) === Number(S.currentCid)) hideUnread();
            });
        },

        chatBadge: {
            show: showUnread,
            clear: hideUnread
        }
    };

    w.Realtime = Realtime;
})(window);