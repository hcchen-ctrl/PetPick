window.Presence = (function () {
  let stomp, ready = null, pingTimer;

  async function ensure() {
    if (ready) return ready;
    ready = new Promise((res, rej) => {
      const sock = new SockJS('/ws');
      stomp = Stomp.over(sock);
      stomp.debug = null;
      stomp.connect({}, res, rej);
    });
    return ready;
  }

  // 啟用自己上線心跳（登入後呼叫一次即可）
  async function startPing(currentUserId) {
    await ensure();
    if (pingTimer) clearInterval(pingTimer);
    const ping = () => stomp.send('/app/presence.ping', {}, JSON.stringify({ userId: Number(currentUserId) }));
    ping();
    pingTimer = setInterval(ping, 15000);
    window.addEventListener('beforeunload', () =>
      stomp.send('/app/presence.leave', {}, JSON.stringify({ userId: Number(currentUserId) })));
  }

  // 監看某個 user 的在線狀態
  async function subscribe(userId, onChange) {
    await ensure();
    const sub = stomp.subscribe(`/topic/presence.${userId}`, f => {
      const ev = JSON.parse(f.body || '{}');
      onChange?.(ev.status === 'online');
    });
    return () => sub.unsubscribe();
  }

  return { startPing, subscribe };
})();