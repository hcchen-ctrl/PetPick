

(function () {
    const tbody = document.getElementById("order-table-body");

    // 後端候選端點（依序嘗試）
    const CANDIDATE_ENDPOINTS = [
        "/api/orders/admin",      // 管理端清單（建議）
        "/api/orders/all"         // 你若另有所有訂單的端點
    ];

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        try {
            const orders = await fetchAllOrders();
            renderTable(Array.isArray(orders) ? orders : []);
        } catch (err) {
            console.error(err);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">載入失敗：${escapeHtml(err.message || "找不到訂單清單 API")}</td></tr>`;
        }
    }

    async function fetchAllOrders() {
        // 逐一嘗試候選端點
        for (const url of CANDIDATE_ENDPOINTS) {
            try {
                const res = await fetch(url);
                if (!res.ok) continue;
                const data = await res.json();
                if (Array.isArray(data)) return data;             // 直接是陣列
                if (data && Array.isArray(data.content)) return data.content; // Page 物件
            } catch (_) { }
        }

        // Fallback：暫時用目前使用者的訂單清單頂上
        const uid = Number(sessionStorage.getItem("checkout_user_id")) || 1;
        const r = await fetch(`/api/orders/user/${encodeURIComponent(uid)}`);
        if (!r.ok) throw new Error("/api/orders/admin 或 /api/orders/all 不存在，且使用者訂單也讀取失敗");
        return await r.json();
    }

    function renderTable(list) {
        if (!list || list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">暫無訂單</td></tr>`;
            return;
        }

        // 新到舊
        list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));

        tbody.innerHTML = list.map(o => {
            const id = o.orderId ?? o.id;                      // 主鍵
            const member = o.receiverName || o.userName ||     // 顯示名稱：優先收件人/會員名
                (o.userId != null ? `#${o.userId}` : "");
            const total = Number(o.totalPrice ?? o.total ?? 0);
            const status = o.status ?? "";
            const time = fmtDateTime(o.createdAt || o.date);

            return `
        <tr>
          <td class="font-monospace">#${escapeHtml(id)}</td>
          <td>${escapeHtml(member)}</td>
          <td>NT$${total.toLocaleString('zh-Hant-TW')}</td>
          <td><span class="badge ${statusBadgeClass(status)}">${escapeHtml(status)}</span></td>
          <td>${time}</td>
          <td>
            <a class="btn btn-sm btn-primary" href="orderDetail.html?orderId=${encodeURIComponent(id)}">查看</a>
          </td>
        </tr>`;
        }).join("");
    }

    function statusBadgeClass(s) {
        const k = String(s || "").toLowerCase();
        if (["paid", "已付款"].includes(k)) return "bg-success";
        if (["pending", "待付款"].includes(k)) return "bg-warning text-dark";
        if (["shipped", "已出貨"].includes(k)) return "bg-info text-dark";
        if (["cancelled", "取消"].includes(k)) return "bg-secondary";
        return "bg-light text-dark";
    }

    function fmtDateTime(iso) {
        if (!iso) return "";
        const d = new Date(iso);
        if (isNaN(d)) return escapeHtml(iso);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        const hh = String(d.getHours()).padStart(2, "0");
        const mm = String(d.getMinutes()).padStart(2, "0");
        return `${y}-${m}-${day} ${hh}:${mm}`;
    }

    function escapeHtml(s) {
        return String(s ?? "")
            .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;').replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }
})();