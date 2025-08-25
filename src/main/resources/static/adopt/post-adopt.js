import { requireLogin, getAuth } from '/adopt/auth.js';

// ---------- 登入檢查 ----------
await requireLogin();                 // 若未登入會自動導去 /login.html
const auth = await getAuth();         // 拿登入狀態（送出成功後要用來判斷導頁）

// ---------- DOM 快取 ----------
const form = document.getElementById('postForm');
const agree = document.getElementById('agreeTerms');
const picker = document.getElementById('imgPicker');
const citySel = document.getElementById('citySelect');
const distSel = document.getElementById('districtSelect');

// ---------- Stepper：亮第 1 步 ----------
['step1', 'step2', 'step3']
    .forEach(id => document.getElementById(id)?.classList.remove('step-active'));
document.getElementById('step1')?.classList.add('step-active');

// ---------- 圖片上傳 ----------
window.pickAndUpload = async function (slot) {
    picker.onchange = async () => {
        if (!picker.files || !picker.files.length) return;

        const fd = new FormData();
        for (const f of picker.files) fd.append('files', f);

        try {
            window.showLoading?.();
            const res = await fetch('/api/upload', { method: 'POST', body: fd });
            if (!res.ok) throw new Error(await res.text());
            const { urls } = await res.json();   // { urls: ["/uploads/xxx.jpg", ...] }

            const input = document.querySelector(`input[name="image${slot}"]`);
            if (input && urls && urls.length) input.value = urls[0];
        } catch (err) {
            alert('上傳失敗：' + (err.message || err));
        } finally {
            picker.value = '';
            picker.onchange = null;
            window.hideLoading?.();
        }
    };

    picker.click();
};

// ---------- 行政區載入 ----------
const AREA_SOURCES = [
    '/adopt/tw-areas.json',
    // 'https://cdn.jsdelivr.net/gh/donma/TaiwanAddressCityAreaRoadChinese@master/CityCountyData.json'
];
const normalizeCity = (s) => (s || '').replace('臺', '台');

async function loadAreas() {
    for (const url of AREA_SOURCES) {
        try {
            const r = await fetch(url, { cache: 'force-cache' });
            if (!r.ok) continue;
            const data = await r.json();
            if (Array.isArray(data) && data[0]?.districts) {
                return data.map(c => ({ name: normalizeCity(c.name), districts: c.districts }));
            } else if (Array.isArray(data) && data[0]?.CityName) {
                return data.map(c => ({
                    name: normalizeCity(c.CityName),
                    districts: (c.AreaList || []).map(a => a.AreaName)
                }));
            }
        } catch { }
    }
    throw new Error('無法載入行政區資料');
}

(async () => {
    try {
        const areas = await loadAreas();
        // 填縣市
        areas.forEach(c => {
            const opt = document.createElement('option');
            opt.value = normalizeCity(c.name);
            opt.textContent = normalizeCity(c.name);
            citySel.appendChild(opt);
        });
        // 縣市改變 → 重填地區
        citySel.addEventListener('change', () => {
            const selCity = citySel.value;
            distSel.innerHTML = '<option value="" selected disabled>請選擇地區</option>';
            distSel.disabled = !selCity;
            if (!selCity) return;
            const city = areas.find(c => normalizeCity(c.name) === selCity);
            (city?.districts || []).forEach(d => {
                const opt = document.createElement('option');
                opt.value = d;
                opt.textContent = d;
                distSel.appendChild(opt);
            });
        });
    } catch (e) {
        console.error(e);
        alert('行政區資料載入失敗');
    }
})();

// 取得 cookie 的函式
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return '';
}

// ---------- 表單驗證 & 送出 ----------
agree.addEventListener('change', () => {
    agree.classList.toggle('is-invalid', !agree.checked);
});

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // 先跑 Bootstrap 的驗證
    const formOk = form.checkValidity();
    form.classList.add('was-validated');

    // 條款 checkbox 額外驗證
    if (!agree.checked) {
        agree.classList.add('is-invalid');
        agree.focus();
        return;
    } else {
        agree.classList.remove('is-invalid');
    }

    if (!formOk) return;

    // 取得 cookie 的函式（統一版本）
    function getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

// 獲取 CSRF token 的函數
    async function getValidCsrfToken() {
        // 先嘗試從 cookie 取得
        let token = getCookie('XSRF-TOKEN');

        // 如果沒有，呼叫 API 取得
        if (!token) {
            try {
                const response = await fetch('/api/csrf-token', {
                    method: 'GET',
                    credentials: 'include'
                });
                const data = await response.json();
                token = data.token;
            } catch (error) {
                console.error('Failed to get CSRF token:', error);
            }
        }

        return token;
    }

// ---------- 表單驗證 & 送出 ----------
    agree.addEventListener('change', () => {
        agree.classList.toggle('is-invalid', !agree.checked);
    });

// 只保留一個 submit 事件監聽器
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // 先跑 Bootstrap 的驗證
        const formOk = form.checkValidity();
        form.classList.add('was-validated');

        // 條款 checkbox 額外驗證
        if (!agree.checked) {
            agree.classList.add('is-invalid');
            agree.focus();
            return;
        } else {
            agree.classList.remove('is-invalid');
        }

        if (!formOk) return;

        // 收集表單資料
        const formData = new FormData(form);
        const postData = {};

        // 將 FormData 轉換為普通物件
        for (const [key, value] of formData.entries()) {
            postData[key] = value;
        }

        try {
            window.showLoading?.();

            // 獲取有效的 CSRF token
            const csrfToken = await getValidCsrfToken();

            if (!csrfToken) {
                alert('無法獲取安全令牌，請重新整理頁面');
                return;
            }

            const res = await fetch('/api/posts', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Csrf-Token': csrfToken   // 使用正確的 header 名稱
                },
                credentials: 'include',
                body: JSON.stringify(postData)  // 發送完整的表單資料
            });

            if (res.ok) {
                alert('已送出！');
                const dest = (auth && auth.role === 'ADMIN')
                    ? '/adopt/post-review.html?status=pending'
                    : '/adopt/my-adopt-progress.html?status=pending';
                location.href = dest;
                return;
            }

            if (res.status === 401) {
                sessionStorage.setItem('redirect', '/post-adopt.html');
                location.href = '/loginpage';
                return;
            }

            const msg = await res.text();
            alert('送出失敗：' + msg);
        } catch (err) {
            console.error('送出錯誤:', err);
            alert('送出失敗，請稍後再試: ' + err.message);
        } finally {
            window.hideLoading?.();
        }
    });


});

