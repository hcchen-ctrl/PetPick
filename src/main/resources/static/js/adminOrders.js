// js/adminOrders.js
(function () {
  // ====== Config / State ======
  const API_BASE = "/api/admin/orders";
  // ★ 管理端請求一律帶上 ADMIN 標頭（對應後端 DemoAuthFilter）
  const ADMIN_HEADERS = { "X-Demo-Role": "ADMIN" };

  const state = {
    page: 1,                   // 1-based
    size: 10,
    totalPages: 1,
    totalElements: 0,
    filters: {
      q: "",
      status: "",
      delivery: "",
      dateFrom: "",
      dateTo: "",
    },
    selected: new Set(),       // 勾選的 orderId 集合
  };

  // ====== DOM ======
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => Array.from(document.querySelectorAll(s));

  const tbody = $("#order-table-body");
  const pagination = $("#pagination");
  const orderCountEl = $("#orderCount");
  const lastRefreshedEl = $("#lastRefreshed");
  const loading = $("#loading");
  const toastContainer = $("#toastContainer");
  const logisticsMeta = $("#logisticsMeta");
  const btnHomeCreate = $("#btnHomeCreate");
  const btnQueryTracking = $("#btnQueryTracking"); // === NEW ===

  // 篩選元件
  const qFilter = $("#qFilter");
  const statusFilter = $("#statusFilter");
  const deliveryFilter = $("#deliveryFilter");
  const fromDate = $("#fromDate");
  const toDate = $("#toDate");
  const btnSearch = $("#btnSearch");
  const btnReset = $("#btnReset");
  const pageSize = $("#pageSize");

  // 批次
  const chkHeader = $("#chkHeader");
  const chkAll = $("#chkAll");
  const btnBulkMarkPaid = $("#btnBulkMarkPaid");
  const btnBulkShip = $("#btnBulkShip");
  const btnBulkCancel = $("#btnBulkCancel");
  const btnExport = $("#btnExport");

  // Modals：狀態
  const statusModal = new bootstrap.Modal($("#statusModal"));
  const statusOrderId = $("#statusOrderId");
  const statusSelect = $("#statusSelect");
  const statusNote = $("#statusNote");
  const btnStatusSave = $("#btnStatusSave");

  // Modals：已付款
  const markPaidModal = new bootstrap.Modal($("#markPaidModal"));
  const markPaidOrderId = $("#markPaidOrderId");
  const gatewayInput = $("#gatewayInput");
  const tradeNoInput = $("#tradeNoInput");
  const paidAmountInput = $("#paidAmountInput");
  const btnMarkPaid = $("#btnMarkPaid");

  // 單筆快取
  const orderCache = new Map();

  // Modals：物流
  const logisticsModal = new bootstrap.Modal($("#logisticsModal"));
  const logisticsOrderId = $("#logisticsOrderId");
  const logisticsIdInput = $("#logisticsIdInput");
  const trackingNoInput = $("#trackingNoInput");
  const btnLogisticsSave = $("#btnLogisticsSave");

  // Modals：取消
  const cancelModal = new bootstrap.Modal($("#cancelModal"));
  const cancelOrderId = $("#cancelOrderId");
  const cancelReasonInput = $("#cancelReasonInput");
  const btnCancelSave = $("#btnCancelSave");

  // ====== Init ======
  document.addEventListener("DOMContentLoaded", () => {
    wireUI();
    loadPage(1);
  });

  function wireUI() {
    // 查詢
    btnSearch.addEventListener("click", () => {
      state.filters.q = (qFilter.value || "").trim();
      state.filters.status = statusFilter.value || "";
      state.filters.delivery = deliveryFilter.value || "";
      state.filters.dateFrom = fromDate.value || "";
      state.filters.dateTo = toDate.value || "";
      loadPage(1);
    });

    // 重設
    btnReset.addEventListener("click", () => {
      qFilter.value = "";
      statusFilter.value = "";
      deliveryFilter.value = "";
      fromDate.value = "";
      toDate.value = "";
      state.filters = { q: "", status: "", delivery: "", dateFrom: "", dateTo: "" };
      loadPage(1);
    });

    // 每頁筆數
    pageSize.addEventListener("change", () => {
      const n = Number(pageSize.value) || 10;
      state.size = n;
      loadPage(1);
    });

    // 全選（表頭 & 批次區域）
    const syncHeaderAll = (checked) => {
      if (chkHeader) chkHeader.checked = checked;
      if (chkAll) chkAll.checked = checked;
    };
    if (chkHeader) chkHeader.addEventListener("change", () => toggleAll(chkHeader.checked, syncHeaderAll));
    if (chkAll) chkAll.addEventListener("change", () => toggleAll(chkAll.checked, syncHeaderAll));

    // 批次功能
    btnBulkMarkPaid.addEventListener("click", () => openBulkMarkPaid());
    btnBulkShip.addEventListener("click", () => bulkUpdateStatus("Shipped"));
    btnBulkCancel.addEventListener("click", () => openBulkCancel());
    btnExport.addEventListener("click", () => exportCSV());
    updateBulkButtons();

    // Modals 行為
    btnStatusSave.addEventListener("click", onSaveStatus);
    btnMarkPaid.addEventListener("click", onMarkPaid);
    btnLogisticsSave.addEventListener("click", onSaveLogistics);
    btnCancelSave.addEventListener("click", onSaveCancel);

    // === NEW === 查詢追蹤碼按鈕
    if (btnQueryTracking) {
      btnQueryTracking.addEventListener("click", async () => {
        const id = Number(logisticsModal._currentId);
        if (!id) return;
        try {
          showLoading(true);
          const j = await queryTrackingFromEcpay(id);
          // 優先用後端回傳補欄位（非空才寫）
          if (j.logisticsId) logisticsIdInput.value = j.logisticsId;
          if (j.trackingNo)  trackingNoInput.value  = j.trackingNo;

          // 再讀一次訂單確認 DB 已寫
          const o = await fetchOrderOne(id);
          if (o.logisticsId && !logisticsIdInput.value) logisticsIdInput.value = o.logisticsId;
          if (o.trackingNo  && !trackingNoInput.value)  trackingNoInput.value  = o.trackingNo;

          if (trackingNoInput.value) {
            toast(`已取得追蹤碼：${escapeHtml(trackingNoInput.value)}`);
          } else {
            toast("目前仍未提供追蹤碼，稍後可再試一次。", "secondary");
          }
        } catch (err) {
          toast(`查詢失敗：${escapeHtml(err.message || "")}`, "danger");
        } finally {
          showLoading(false);
        }
      });
    }
  }

  // ====== API ======
  async function fetchOrders() {
    const p = new URLSearchParams();
    if (state.filters.q) p.set("q", state.filters.q);
    if (state.filters.status) p.set("status", state.filters.status);
    if (state.filters.delivery) p.set("delivery", state.filters.delivery);
    if (state.filters.dateFrom) p.set("dateFrom", state.filters.dateFrom);
    if (state.filters.dateTo) p.set("dateTo", state.filters.dateTo);
    p.set("page", String(state.page));
    p.set("size", String(state.size));

    const url = `${API_BASE}?${p.toString()}`;
    const res = await fetch(url, { headers: { ...ADMIN_HEADERS } });
    if (!res.ok) throw new Error(`讀取失敗 (${res.status})`);

    const data = await res.json();
    if (Array.isArray(data)) {
      state.totalElements = data.length;
      state.totalPages = 1;
      return data;
    } else {
      state.totalElements = Number(data.totalElements ?? 0);
      state.totalPages = Number(data.totalPages ?? 1);
      return Array.isArray(data.content) ? data.content : [];
    }
  }

  async function apiPatch(url, body) {
    const r = await fetch(url, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...ADMIN_HEADERS },
      body: JSON.stringify(body || {}),
    });
    if (!r.ok) throw new Error(await safeText(r));
    return true;
  }

  async function apiPost(url, body) {
    const r = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...ADMIN_HEADERS },
      body: JSON.stringify(body || {}),
    });
    if (!r.ok) throw new Error(await safeText(r));
    return true;
  }

  // === NEW === 從綠界查詢（後端封裝）：GET /api/logistics/home/query/{orderId}
  async function apiGet(url) {
    const r = await fetch(url, { headers: { ...ADMIN_HEADERS } });
    const j = await r.json().catch(() => ({}));
    if (!r.ok || j.ok === false) {
      throw new Error(j.error || `HTTP ${r.status}`);
    }
    return j;
  }

  // ====== Load / Render ======
  async function loadPage(pageNum = 1) {
    state.page = Math.max(1, pageNum);
    showLoading(true);
    try {
      const list = await fetchOrders();
      renderTable(list);
      renderPagination();
      orderCountEl.textContent = String(state.totalElements);
      lastRefreshedEl.textContent = fmtDateTime(new Date().toISOString());
      state.selected.clear();
      syncHeaderChecks();
      updateBulkButtons();
    } catch (err) {
      console.error(err);
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">載入失敗：${escapeHtml(err.message || "")}</td></tr>`;
      pagination.innerHTML = "";
    } finally {
      showLoading(false);
    }
  }

  function renderTable(list) {
    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">暫無訂單</td></tr>`;
      return;
    }
    list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));

    tbody.innerHTML = list.map(o => rowHtml(o)).join("");

    // 綁定 row checkbox 與 row actions
    $$(".row-check").forEach(cb => {
      cb.addEventListener("change", () => {
        const id = Number(cb.dataset.id);
        if (cb.checked) state.selected.add(id); else state.selected.delete(id);
        syncHeaderChecks();
        updateBulkButtons();
      });
    });

    // 單筆動作
    $$(".btn-view").forEach(btn => btn.addEventListener("click", onView));
    $$(".btn-status").forEach(btn => btn.addEventListener("click", onOpenStatus));
    $$(".btn-paid").forEach(btn => btn.addEventListener("click", onOpenMarkPaid));
    $$(".btn-logistics").forEach(btn => btn.addEventListener("click", onOpenLogistics));
    $$(".btn-cancel").forEach(btn => btn.addEventListener("click", onOpenCancel));

    hydrateDelivery(); // 補齊配送欄顯示（必要時）
  }

  function rowHtml(o) {
    const id = o.orderId ?? o.id;
    const mtn = (o.merchantTradeNo ?? "").trim();
    const member = o.receiverName || o.userName || (o.userId != null ? `#${o.userId}` : "—");
    const total = Number(o.totalPrice ?? o.total ?? 0);
    const status = o.status ?? "";
    const time = fmtDateTime(o.createdAt || o.date);
    const delivery = displayDelivery(o);

    const checked = state.selected.has(Number(id)) ? "checked" : "";

    return `
    <tr data-row-id="${escapeAttr(id)}">
      <td><input class="form-check-input row-check" type="checkbox" data-id="${escapeAttr(id)}" ${checked}></td>
      <td class="font-mono lh-sm">
        <div>#${escapeHtml(id)}</div>
        <div class="text-muted">${mtn ? escapeHtml(mtn) : ""}</div>
      </td>
      <td>${escapeHtml(member)}</td>
      <td>NT$${total.toLocaleString("zh-Hant-TW")}</td>
      <td><span class="badge ${statusBadgeClass(status)}">${escapeHtml(status)}</span></td>
      <td class="td-delivery">${escapeHtml(delivery)}</td>
      <td>${time}</td>
      <td class="d-flex flex-wrap gap-1 align-items-center">
        <button class="btn btn-sm btn-outline-primary btn-view" data-id="${escapeAttr(id)}">查看</button>
        <button class="btn btn-sm btn-outline-secondary btn-status" data-id="${escapeAttr(id)}" data-status="${escapeAttr(status)}">狀態</button>
        <button class="btn btn-sm btn-success btn-paid" data-id="${escapeAttr(id)}" data-amount="${escapeAttr(total)}">付款</button>
        <button class="btn btn-sm btn-info btn-logistics" data-id="${escapeAttr(id)}">物流</button>
        <button class="btn btn-sm btn-outline-danger btn-cancel" data-id="${escapeAttr(id)}">取消</button>
      </td>
    </tr>
  `;
  }

  function renderPagination() {
    const { page, totalPages } = state;
    if (totalPages <= 1) { pagination.innerHTML = ""; return; }

    const item = (label, target, disabled = false, active = false) =>
      `<li class="page-item ${disabled ? "disabled" : ""} ${active ? "active" : ""}">
        <a class="page-link" href="#" data-page="${target}">${label}</a>
       </li>`;

    let html = "";
    html += item("«", 1, page === 1);
    html += item("‹", Math.max(1, page - 1), page === 1);

    const pageWindow = 2;
    const start = Math.max(1, page - pageWindow);
    const end = Math.min(totalPages, page + pageWindow);
    for (let i = start; i <= end; i++) {
      html += item(String(i), i, false, i === page);
    }

    html += item("›", Math.min(totalPages, page + 1), page === totalPages);
    html += item("»", totalPages, page === totalPages);

    pagination.innerHTML = html;
    $$("#pagination a.page-link").forEach(a => {
      a.addEventListener("click", (e) => {
        e.preventDefault();
        const p = Number(a.dataset.page);
        if (Number.isFinite(p) && p >= 1 && p <= state.totalPages) loadPage(p);
      });
    });
  }

  // ====== Row Actions ======
  function onView(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    location.href = `orderDetail.html?orderId=${encodeURIComponent(id)}`;
  }

  function onOpenStatus(e) {
    const id = e.currentTarget.dataset.id;
    const s = e.currentTarget.dataset.status || "Pending";
    statusOrderId.textContent = `#${id}`;
    statusSelect.value = s;
    statusNote.value = "";
    statusModal.show();
    statusModal._currentId = Number(id);
  }

  async function onSaveStatus() {
    const id = statusModal._currentId;
    if (!id) return;
    try {
      showLoading(true);
      await apiPatch(`${API_BASE}/${id}/status`, {
        status: statusSelect.value,
        note: statusNote.value || ""
      });
      statusModal.hide();
      toast("狀態已更新");
      loadPage(state.page);
    } catch (err) {
      toast(`更新失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  function onOpenMarkPaid(e) {
    const id = e.currentTarget.dataset.id;
    const amt = Number(e.currentTarget.dataset.amount || 0);
    markPaidOrderId.textContent = `#${id}`;
    gatewayInput.value = "Manual";
    tradeNoInput.value = "";
    paidAmountInput.value = String(amt || "");
    markPaidModal.show();
    markPaidModal._currentId = Number(id);
  }

  async function onMarkPaid() {
    const id = markPaidModal._currentId;
    if (!id) return;
    try {
      showLoading(true);
      await apiPost(`${API_BASE}/${id}/mark-paid`, {
        gateway: (gatewayInput.value || "Manual").trim(),
        tradeNo: (tradeNoInput.value || "").trim(),
        paidAmount: Number(paidAmountInput.value || 0)
      });
      markPaidModal.hide();
      toast("已標記為已付款");
      loadPage(state.page);
    } catch (err) {
      toast(`操作失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  async function onOpenLogistics(e) {
    const id = Number(e.currentTarget.dataset.id);
    logisticsOrderId.textContent = `#${id}`;
    logisticsIdInput.value = "";
    trackingNoInput.value = "";
    logisticsMeta.textContent = "讀取中…";
    btnHomeCreate.classList.add("d-none");           // 預設先隱藏
    btnQueryTracking.classList.add("d-none");        // === NEW === 預設隱藏
    logisticsModal._currentId = id;

    try {
      showLoading(true);
      const o = await fetchOrderOne(id);

      // 預填既有物流欄位（若有）
      if (o.logisticsId) logisticsIdInput.value = o.logisticsId;
      if (o.trackingNo)  trackingNoInput.value  = o.trackingNo;

      // 顯示配送資訊
      const t = String(o.shippingType || "").toLowerCase();
      if (t === "address") {
        logisticsMeta.innerHTML = `
          <div><span class="badge bg-secondary">宅配</span> ${escapeHtml(o.addr || "")}</div>
          <div class="text-muted">收件人：${escapeHtml(o.receiverName || "—")}（${escapeHtml(o.receiverPhone || "—")}）</div>
        `;
        // 宅配：顯示兩顆按鈕（建立、查詢）
        btnHomeCreate.classList.remove("d-none");
        btnHomeCreate.onclick = () => createHomeFor(id);

        btnQueryTracking.classList.remove("d-none"); // === NEW ===
        btnQueryTracking.onclick = async () => {
          try {
            showLoading(true);
            const j = await queryTrackingFromEcpay(id);
            if (j.logisticsId) logisticsIdInput.value = j.logisticsId;
            if (j.trackingNo)  trackingNoInput.value  = j.trackingNo;

            const o2 = await fetchOrderOne(id);
            if (o2.trackingNo) {
              trackingNoInput.value = o2.trackingNo;
              toast(`已取得追蹤碼：${escapeHtml(o2.trackingNo)}`);
            } else {
              toast("目前仍未提供追蹤碼，稍後可再試一次。", "secondary");
            }
          } catch (err) {
            toast(`查詢失敗：${escapeHtml(err.message || "")}`, "danger");
          } finally {
            showLoading(false);
          }
        };
      } else if (t === "cvs_cod") {
        const brand = o.storeBrand || "";
        const brandText = brand ? `（${escapeHtml(brand)}）` : "";
        logisticsMeta.innerHTML = `
          <div><span class="badge bg-info text-dark">超商取貨付款</span> ${brandText}</div>
          <div class="text-muted">${escapeHtml(o.storeName || "—")}　${o.storeAddress ? "｜" + escapeHtml(o.storeAddress) : ""}</div>
        `;
        btnHomeCreate.classList.add("d-none");
        btnHomeCreate.onclick = null;
        btnQueryTracking.classList.add("d-none"); // === NEW ===
        btnQueryTracking.onclick = null;
      } else {
        logisticsMeta.innerHTML = `<div class="text-muted">配送：${escapeHtml(o.shippingType || "—")}</div>`;
        btnHomeCreate.classList.add("d-none");
        btnHomeCreate.onclick = null;
        btnQueryTracking.classList.add("d-none"); // === NEW ===
        btnQueryTracking.onclick = null;
      }

      logisticsModal.show();
    } catch (err) {
      toast(`讀取訂單失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  async function onSaveLogistics() {
    const id = logisticsModal._currentId;
    if (!id) return;
    try {
      showLoading(true);

      // 只有在有值時才更新，避免把 DB 的 NULL 蓋成空字串
      const payload = {};
      const lid = (logisticsIdInput.value || "").trim();
      const tno = (trackingNoInput.value || "").trim();
      if (lid) payload.logisticsId = lid;
      if (tno) payload.trackingNo = tno;

      if (Object.keys(payload).length === 0) {
        toast("沒有可更新的欄位", "warning");
        return;
      }

      await apiPost(`${API_BASE}/${id}/logistics`, payload);

      // 若有追蹤碼或物流編號 → 可順便標記為已出貨（依需求）
      if (tno || lid) {
        await apiPatch(`${API_BASE}/${id}/status`, { status: "Shipped", note: "" });
      }

      logisticsModal.hide();
      toast("物流資訊已儲存");
      loadPage(state.page);
    } catch (err) {
      toast(`操作失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  function onOpenCancel(e) {
    const id = e.currentTarget.dataset.id;
    cancelOrderId.textContent = `#${id}`;
    cancelReasonInput.value = "";
    cancelModal.show();
    cancelModal._currentId = Number(id);
  }

  async function onSaveCancel() {
    const id = cancelModal._currentId;
    if (!id) return;
    try {
      showLoading(true);
      await apiPost(`${API_BASE}/${id}/cancel`, {
        reason: cancelReasonInput.value || ""
      });
      cancelModal.hide();
      toast("訂單已取消");
      loadPage(state.page);
    } catch (err) {
      toast(`取消失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  // ====== Bulk ======
  function selectedIds() {
    return Array.from(state.selected.values());
  }

  function toggleAll(checked, syncHeaderAll) {
    $$(".row-check").forEach(cb => {
      cb.checked = checked;
      const id = Number(cb.dataset.id);
      if (checked) state.selected.add(id); else state.selected.delete(id);
    });
    if (typeof syncHeaderAll === "function") syncHeaderAll(checked);
    updateBulkButtons();
  }

  function syncHeaderChecks() {
    const all = $$(".row-check");
    const checked = all.filter(cb => cb.checked).length;
    const allChecked = all.length > 0 && checked === all.length;
    if (chkHeader) chkHeader.checked = allChecked;
    if (chkAll) chkAll.checked = allChecked;
  }

  function updateBulkButtons() {
    const hasSel = state.selected.size > 0;
    btnBulkMarkPaid.disabled = !hasSel;
    btnBulkShip.disabled = !hasSel;
    btnBulkCancel.disabled = !hasSel;
  }

  function openBulkMarkPaid() {
    if (state.selected.size === 0) return;
    markPaidOrderId.textContent = `${state.selected.size} 筆`;
    gatewayInput.value = "Manual";
    tradeNoInput.value = "";
    paidAmountInput.value = "";
    markPaidModal.show();
    markPaidModal._bulk = true;
  }

  async function bulkUpdateStatus(targetStatus) {
    const ids = selectedIds();
    if (ids.length === 0) return;
    try {
      showLoading(true);
      await apiPost(`${API_BASE}/bulk-status`, {
        orderIds: ids,
        status: targetStatus,
        note: ""
      });
      toast(`已批次標記為 ${targetStatus}`);
      loadPage(state.page);
    } catch (err) {
      toast(`批次操作失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  function openBulkCancel() {
    if (state.selected.size === 0) return;
    cancelOrderId.textContent = `${state.selected.size} 筆`;
    cancelReasonInput.value = "";
    cancelModal.show();
    cancelModal._bulk = true;
  }

  const _origOnSaveCancel = onSaveCancel;
  btnCancelSave.removeEventListener?.("click", onSaveCancel);
  btnCancelSave.addEventListener("click", async () => {
    if (cancelModal._bulk) {
      try {
        showLoading(true);
        await apiPost(`${API_BASE}/bulk-status`, {
          orderIds: selectedIds(),
          status: "Cancelled",
          note: cancelReasonInput.value || ""
        });
        cancelModal.hide();
        toast("已批次取消");
        loadPage(state.page);
      } catch (err) {
        toast(`批次取消失敗：${escapeHtml(err.message || "")}`, "danger");
      } finally {
        showLoading(false);
      }
    } else {
      await _origOnSaveCancel();
    }
    cancelModal._bulk = false;
  });

  const _origOnMarkPaid = onMarkPaid;
  btnMarkPaid.removeEventListener?.("click", onMarkPaid);
  btnMarkPaid.addEventListener("click", async () => {
    if (markPaidModal._bulk) {
      try {
        showLoading(true);
        const ids = selectedIds();
        await apiPost(`${API_BASE}/bulk-status`, {
          orderIds: ids,
          status: "Paid",
          note: `手動：${(gatewayInput.value || "Manual")} / ${tradeNoInput.value || ""} / ${paidAmountInput.value || ""}`
        });
        markPaidModal.hide();
        toast("已批次標記為已付款");
        loadPage(state.page);
      } catch (err) {
        toast(`批次標記失敗：${escapeHtml(err.message || "")}`, "danger");
      } finally {
        showLoading(false);
      }
    } else {
      await _origOnMarkPaid();
    }
    markPaidModal._bulk = false;
  });

  // ====== Export CSV ======
  async function exportCSV() {
    try {
      showLoading(true);
      const rows = [];
      const sizeBackup = state.size;
      const pageBackup = state.page;
      const tmpSize = 100;

      state.size = tmpSize;
      await loadPage(1);
      rows.push(...collectRowsFromTbody());

      for (let p = 2; p <= state.totalPages; p++) {
        state.page = p;
        const list = await fetchOrders();
        renderTable(list);
        rows.push(...collectRowsFromTbody());
      }

      state.size = sizeBackup;
      state.page = pageBackup;
      await loadPage(state.page);

      const head = ["訂單編號", "綠界訂單編號", "會員", "金額", "狀態", "配送", "下單時間"];
      const csv = [head, ...rows].map(r => r.map(csvCell).join(",")).join("\n");
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `orders_${Date.now()}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast("CSV 匯出完成");
    } catch (err) {
      toast(`匯出失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  function collectRowsFromTbody() {
    return Array.from(tbody.querySelectorAll("tr")).map(tr => {
      const tds = tr.querySelectorAll("td");
      if (tds.length < 8) return [];
      const idBox = tds[1];
      const id = idBox.querySelector("div:nth-child(1)")?.textContent.trim() || "";
      const mtn = idBox.querySelector("div:nth-child(2)")?.textContent.trim() || "";
      const member = tds[2]?.textContent.trim() || "";
      const amount = tds[3]?.textContent.replace(/\s+/g, " ").trim() || "";
      const status = tds[4]?.innerText.trim() || "";
      const delivery = tds[5]?.textContent.trim() || "";
      const time = tds[6]?.textContent.trim() || "";
      return [id.replace(/^#/, ""), mtn, member, amount.replace(/^NT\$/, ""), status, delivery, time];
    }).filter(r => r.length > 0);
  }

  // ====== Utils ======
  function showLoading(v) {
    if (!loading) return;
    loading.style.display = v ? "flex" : "none";
  }

  function toast(message, type = "success") {
    const id = "t" + Math.random().toString(36).slice(2);
    const html = `
      <div id="${id}" class="toast align-items-center text-bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">${message}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>`;
    toastContainer.insertAdjacentHTML("beforeend", html);
    const el = document.getElementById(id);
    const t = new bootstrap.Toast(el, { delay: 2500 });
    t.show();
    el.addEventListener("hidden.bs.toast", () => el.remove());
  }

  function statusBadgeClass(s) {
    const k = String(s || "").toLowerCase();
    if (["paid", "已付款"].includes(k)) return "bg-success";
    if (["pending", "待付款"].includes(k)) return "bg-warning text-dark";
    if (["shipped", "已出貨"].includes(k)) return "bg-info text-dark";
    if (["cancelled", "取消"].includes(k)) return "bg-secondary";
    if (["failed", "付款失敗"].includes(k)) return "bg-danger";
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

  function displayDelivery(o) {
    const type = String(o.shippingType || "").toLowerCase();
    if (type === "cvs_cod" || o.storeId || o.storeName || o.storeAddress) {
      const brand = o.storeBrand || "";
      const name = o.storeName || "";
      const addr = o.storeAddress || "";
      const parts = [brand, name].filter(Boolean).join(" ");
      return parts || addr ? `${parts ? parts : ""}${addr ? (parts ? " " : "") + `（${addr}）` : ""}` : "超商取貨付款";
    }
    if (type === "address" || o.addr) {
      return o.addr ? `宅配（${o.addr}）` : "宅配";
    }
    return "—";
  }

  function escapeHtml(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;").replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;").replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }
  function escapeAttr(s) { return String(s ?? "").replaceAll('"', '&quot;'); }
  async function safeText(r) { try { return await r.text(); } catch { return `HTTP ${r.status}`; } }

  // ====== 這三個函式移進 IIFE 內 ======
  async function hydrateDelivery() {
    const rows = Array.from(tbody.querySelectorAll("tr[data-row-id]"));
    for (const tr of rows) {
      const td = tr.querySelector(".td-delivery");
      if (!td) continue;
      const current = (td.textContent || "").trim();
      if (current && current !== "—") continue;

      const id = tr.getAttribute("data-row-id");
      if (!id) continue;

      try {
        const r = await fetch(`${API_BASE}/${encodeURIComponent(id)}`, { headers: { ...ADMIN_HEADERS } });
        if (!r.ok) continue;
        const o = await r.json();
        td.textContent = displayDelivery(o);
      } catch { }
    }
  }

  async function fetchOrderOne(id) {
    id = Number(id);
    if (orderCache.has(id)) return orderCache.get(id);
    const r = await fetch(`${API_BASE}/${id}`, { headers: { ...ADMIN_HEADERS } });
    if (!r.ok) throw new Error(`讀取訂單失敗 (${r.status})`);
    const data = await r.json();
    orderCache.set(id, data);
    return data;
  }

  // === NEW === 後端封裝的「查詢宅配單」：呼叫 GET /api/logistics/home/query/{orderId}
  async function queryTrackingFromEcpay(orderId) {
    const j = await apiGet(`/api/logistics/home/query/${encodeURIComponent(orderId)}`);
    // 這支 API 已在後端內部把 trackingNo / logisticsStatus 回寫 DB（若有變動）
    return j;
  }

  // ★ 建立宅配託運單：若暫無追蹤碼，啟動輪詢並搭配「查詢宅配單」API
  async function createHomeFor(orderId) {
    if (!orderId) return;
    try {
      showLoading(true);
      const r = await fetch("/api/logistics/home/ecpay/create", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...ADMIN_HEADERS },
        body: JSON.stringify({ orderId, isCollection: false })
      });
      const j = await r.json().catch(() => ({}));
      if (!r.ok || j.ok === false) throw new Error(j.error || "建立失敗");

      // 寫回輸入框（只在有值時寫入）
      if (j.logisticsId) logisticsIdInput.value = j.logisticsId;
      if (j.trackingNo)  trackingNoInput.value  = j.trackingNo;

      // 立刻存到訂單（只帶有值欄位，避免覆蓋成空字串）
      const payload = {};
      if (j.logisticsId) payload.logisticsId = j.logisticsId;
      if (j.trackingNo)  payload.trackingNo  = j.trackingNo;
      if (Object.keys(payload).length > 0) {
        await apiPost(`${API_BASE}/${orderId}/logistics`, payload);
      }

      if (j.trackingNo) {
        toast(`宅配託運單建立完成，追蹤碼：${escapeHtml(j.trackingNo)}`);
      } else {
        toast("宅配託運單建立完成，追蹤碼尚未提供（將於回拋或查詢後自動更新）", "warning");
        // 啟動輪詢：每 5 秒查一次，最多 6 次；每次先打 Query API，再讀訂單
        await pollTracking(orderId, 6, 5000);
      }

      // 詢問是否標記出貨
      if (confirm("是否將訂單標記為 Shipped？")) {
        await apiPatch(`${API_BASE}/${orderId}/status`, { status: "Shipped", note: "Admin 建立綠界宅配" });
        orderCache.delete(Number(orderId));
        await loadPage(state.page);
      }
    } catch (err) {
      toast(`宅配建立失敗：${escapeHtml(err.message || "")}`, "danger");
    } finally {
      showLoading(false);
    }
  }

  // ★ 輪詢追蹤碼（搭配 Query API）
  async function pollTracking(orderId, times = 6, intervalMs = 5000) {
    for (let i = 0; i < times; i++) {
      try {
        await wait(intervalMs);
        // 先對綠界查詢（由後端封裝）
        await queryTrackingFromEcpay(orderId);
        // 再讀一次訂單
        const o = await fetchOrderOne(orderId);
        if (o && o.trackingNo) {
          trackingNoInput.value = o.trackingNo;
          toast(`已取得追蹤碼：${escapeHtml(o.trackingNo)}`);
          return;
        }
      } catch {
        // 忽略單次失敗，繼續下一輪
      }
    }
    toast("仍未收到追蹤碼（可能綠界稍後回拋）", "secondary");
  }

  function wait(ms) {
    return new Promise(res => setTimeout(res, ms));
  }

  // ====== /moved inside IIFE ======
})();