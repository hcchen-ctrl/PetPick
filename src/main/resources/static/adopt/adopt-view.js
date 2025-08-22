import { getAuth } from '/adopt/auth.js';

// ===== 顯示 helper =====
const normalizeSex = s => {
    const v = (s ?? '').toString().trim().toLowerCase();
    if (v === 'male' || v === 'm' || v.includes('公')) return 'male';
    if (v === 'female' || v === 'f' || v.includes('母')) return 'female';
    return 'unknown';
};
const sexText = s => (normalizeSex(s) === 'male' ? '公' : normalizeSex(s) === 'female' ? '母' : '不確定');
const sexIcon = s => normalizeSex(s) === 'male'
    ? '<span class="sex-icon"><img src="/images/male.png"></span>'
    : normalizeSex(s) === 'female' ? '<span class="sex-icon"><img src="/images/female.png"></span>' : '';
const neuterText = n => n === 'yes' ? '是' : n === 'no' ? '否' : '不確定';
const ageLimitText = a => a === 'age20plus' ? '20歲以上' : a === 'age25plus' ? '25歲以上' : '不限';
const contactMethodText = m => m === 'line_only' ? '僅 LINE' : '電話＋簡訊';
const boolText = b => (b ? '需要' : '不需要');
const place = p => [p.city, p.district].filter(Boolean).join(' ');
const animalLine = p => [p.species, p.breed, `${sexText(p.sex)} ${sexIcon(p.sex)}`, p.age, p.bodyType].filter(Boolean).join('｜');
const contactLine = p => {
    const a = [`聯絡人：${p.contactName || '—'}`, `電話：${p.contactPhone || '—'}`];
    if (p.contactLine) a.push(`LINE：${p.contactLine}`); return a.join('　');
};

const id = new URLSearchParams(location.search).get('id');
const [auth, post] = await Promise.all([
    getAuth(),
    fetch('/adopts/' + id).then(r => r.ok ? r.json() : Promise.reject('not found'))
]);

const loggedIn = !!auth?.loggedIn;
const isAdmin = auth?.role === 'ADMIN';
const isOwner = loggedIn && auth.uid && post.postedByUserId === auth.uid;
const canControl = isOwner || isAdmin;

// 若未 approved 且非擁有者/管理員 → 不公開
if (post.status !== 'approved' && !canControl) {
    document.getElementById('box').innerHTML = `
        <div class="alert alert-secondary mt-4">
          這則貼文尚未公開或已被取消/關閉。
          <a href="/adopt-list.html" class="alert-link">回到列表</a>
        </div>`;
    // 不再渲染細節，避免洩漏資料
    throw new Error('not public');
}

const pendingBadge = (post.pendingApplications && post.pendingApplications > 0)
    ? `<span class="badge text-bg-info ms-2">申請中 ${post.pendingApplications}</span>` : '';

const badge = post.sourceType === 'platform'
    ? '<span class="badge text-bg-primary ms-2">我方救助</span>'
    : '<span class="badge text-bg-warning ms-2">民眾送養</span>';

let bottom = '';
if (!loggedIn) {
    bottom = (post.sourceType === 'user')
        ? `<div class="alert alert-warning">聯絡資訊僅登入會員可見</div>`
        : `<div class="alert alert-info">官方刊登，<a href="/login.html">請登入</a> 以進一步申請</div>`;
} else if (post.sourceType === 'user') {
    bottom = `<div class="alert alert-success">聯絡人：${post.contactName || '—'}　電話：${post.contactPhone || '—'}　LINE：${post.contactLine || '—'}</div>`;
} else {
    if (post.appliedByMe) {
        if (post.myPendingApplicationId) {
            bottom = `
            <div class="alert alert-secondary d-flex justify-content-between align-items-center">
            <span>你已送出申請，請等待審核。</span>
            <button class="btn btn-outline-danger btn-sm" id="cancelAppBtn">取消申請</button>
            </div>`;
        } else {
            bottom = `<div class="alert alert-secondary">你已送出申請，請等待審核。</div>`;
        }
    } else {
        bottom = `
        <div class="d-flex align-items-start gap-2 flex-md-nowrap">
            <textarea id="applyMsg" class="form-control flex-grow-1" rows="2" placeholder="想說的話（選填）" style="min-width:0"></textarea>
            <button class="btn btn-outline-secondary flex-shrink-0" id="applyBtn" style="white-space:nowrap">我要領養</button>
        </div>`;
    }
}


// 1. 設定圖片前綴（你後端放在 /adopt/uploads/）
const uploadPrefix = '/adopt/uploads/';

// 2. 拼接完整的圖片 URL（排除 null、空字串）
const imgs = [post.image1, post.image2, post.image3]
  .filter(u => !!u && u.trim())
  .map(u => uploadPrefix + u); // <<< 這行是關鍵！

// 3. 沒圖就顯示預設圖片
if (!imgs.length) imgs.push('/images/no-image.jpg');

// 4. 產生輪播 indicator
const indicators = imgs.map((_, i) =>
  `<button type="button" data-bs-target="#petCarousel" data-bs-slide-to="${i}"
    class="${i === 0 ? 'active' : ''}" ${i === 0 ? 'aria-current="true"' : ''} aria-label="Slide ${i + 1}"></button>`
).join('');

// 5. 產生圖片輪播內容
const slides = imgs.map((u, i) =>
  `<div class="carousel-item ${i === 0 ? 'active' : ''}">
     <div class="carousel-fitbox">
       <img src="${u}" onerror="this.src='/images/no-image.jpg'">
     </div>
   </div>`
).join('');


// 擁有者/管理員控制面板（依狀態顯示）
const ownerPanel = canControl ? (() => {
    if (post.status === 'pending') {
        return `<button class="btn btn-outline-danger btn-sm" id="btnCancel">取消刊登</button>`;
    }
    if (post.status === 'approved') {
        return `
          <button class="btn btn-outline-warning btn-sm" id="btnHold">暫停</button>
          <button class="btn btn-outline-secondary btn-sm" id="btnClose">關閉</button>`;
    }
    if (post.status === 'on_hold') {
        return `
          <button class="btn btn-outline-success btn-sm" id="btnResume">恢復</button>
          <button class="btn btn-outline-secondary btn-sm" id="btnClose">關閉</button>`;
    }
    return ''; // rejected/cancelled/closed 不顯示
})() : '';

document.getElementById('box').innerHTML = `
            <div class="row g-4">
                <!-- 左欄：圖片輪播 + 其他說明 -->
                <div class="col-md-6">
                <div id="petCarousel" class="carousel slide" data-bs-ride="true">
                    <div class="carousel-indicators">${indicators}</div>
                    <div class="carousel-inner">${slides}</div>
                    <button class="carousel-control-prev" type="button" data-bs-target="#petCarousel" data-bs-slide="prev">
                    <span class="carousel-control-prev-icon" aria-hidden="true"></span><span class="visually-hidden">上一張</span>
                    </button>
                    <button class="carousel-control-next" type="button" data-bs-target="#petCarousel" data-bs-slide="next">
                    <span class="carousel-control-next-icon" aria-hidden="true"></span><span class="visually-hidden">下一張</span>
                    </button>
                </div>

                <!-- ✅ 其他說明移到這裡 -->
                <div class="mt-3">
                    <div class="mb-1"><strong>其他說明：</strong></div>
                    <div class="border rounded p-2 bg-light" style="min-height:80px">${post.description || '—'}</div>
                </div>
                </div>

                <!-- 右欄：資訊 + 申請 +（可選）擁有者控制 -->
                <div class="col-md-6">
                <h3 class="mb-2">${post.title || ''} ${badge} ${pendingBadge}</h3>
                <div class="mb-2 text-muted">${place(post)}</div>

                <div class="mb-2"><strong>動物：</strong>${animalLine(post)}</div>
                <div class="mb-2"><strong>毛色：</strong>${post.color || '—'}</div>
                <div class="mb-2"><strong>是否結紮：</strong>${neuterText(post.neutered)}</div>

                <div class="mb-2"><strong>聯絡方式：</strong>${contactMethodText(post.contactMethod)}</div>
                ${loggedIn ? `<div class="mb-2"><strong>聯絡資訊：</strong>${contactLine(post)}</div>` : ''}

                <hr class="my-2">

                <div class="mb-2"><strong>領養人年齡限制：</strong>${ageLimitText(post.adopterAgeLimit)}</div>
                <div class="mb-2"><strong>是否接受家訪：</strong>${boolText(post.requireHomeVisit)}</div>
                <div class="mb-2"><strong>是否簽切結書：</strong>${boolText(post.requireContract)}</div>
                <div class="mb-2"><strong>是否回報領養情況：</strong>${boolText(post.requireFollowup)}</div>

                <hr class="my-2">

                <!-- ✅ 申請區塊放在右欄這裡 -->
                <div class="mt-3">${bottom}</div>

                ${ownerPanel ? `<div class="mt-3"><div class="d-flex gap-2">${ownerPanel}</div></div>` : ``}
                </div>
            </div>
            `;

// 互動：我要領養（官方來源）
document.getElementById('applyBtn')?.addEventListener('click', async () => {
    const message = document.getElementById('applyMsg')?.value?.trim() || null;
    try {
        const r = await fetch(`/api/adopts/${id}/apply`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ message })
        });
        if (r.status === 409) { alert('你已申請過了，請等待審核。'); return; }
        if (!r.ok) throw new Error(await r.text() || '申請失敗');
        alert('已送出申請！');
        location.reload(); // 重新載入讓 badge/按鈕狀態更新
    } catch (e) {
        console.error(e);
        alert('申請失敗');
    }
});

document.getElementById('cancelAppBtn')?.addEventListener('click', async (e) => {
    e.preventDefault();
    if (!confirm('確定要取消這筆申請？')) return;
    const ok = await fetch(`/api/applications/${post.myPendingApplicationId}/cancel`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已取消' : '取消失敗');
    if (ok) location.reload();
});

// 互動：擁有者/管理員控制（hit /api/posts/...）
const refresh = () => location.reload();
document.getElementById('btnCancel')?.addEventListener('click', async () => {
    if (!confirm('確定取消這筆刊登？')) return;
    const ok = await fetch(`/api/posts/${post.id}/cancel`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已取消' : '取消失敗'); if (ok) refresh();
});
document.getElementById('btnHold')?.addEventListener('click', async () => {
    if (!confirm('暫停上架？')) return;
    const ok = await fetch(`/api/posts/${post.id}/hold?hold=true`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已暫停' : '操作失敗'); if (ok) refresh();
});
document.getElementById('btnResume')?.addEventListener('click', async () => {
    if (!confirm('恢復上架？')) return;
    const ok = await fetch(`/api/posts/${post.id}/hold?hold=false`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已恢復' : '操作失敗'); if (ok) refresh();
});
document.getElementById('btnClose')?.addEventListener('click', async () => {
    if (!confirm('確定關閉（已送養完成）？')) return;
    const ok = await fetch(`/api/posts/${post.id}/close`, { method: 'PATCH' }).then(r => r.ok);
    alert(ok ? '已關閉' : '關閉失敗'); if (ok) refresh();
});