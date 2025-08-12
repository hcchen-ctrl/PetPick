


// 表單驗證
document.getElementById('registerForm').querySelector('form').addEventListener('submit', function (e) {
    e.preventDefault();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        alert('密碼與確認密碼不相符，請重新輸入');
        return;
    }

    if (password.length < 8) {
        alert('密碼長度至少需要8個字符');
        return;
    }

    if (!document.getElementById('agreeTerms').checked) {
        alert('請同意服務條款與隱私政策');
        return;
    }

    alert('註冊成功！歡迎加入 Wepet 大家庭');
    showLogin();
});

// 登入表單提交
document.getElementById('loginForm').querySelector('form').addEventListener('submit', function (e) {
    e.preventDefault();
    alert('登入成功！歡迎回到 Wepet');
});

// 忘記密碼表單提交
document.getElementById('forgotPasswordForm').querySelector('form').addEventListener('submit', function (e) {
    e.preventDefault();
    alert('重設密碼連結已發送至您的信箱，請查收');
    showLogin();
});

// 預設顯示登入表單
showLogin();



function showForm(formId) {
    // 所有表單 ID
    const formIds = ['loginForm', 'registerForm', 'forgotPasswordForm'];

    formIds.forEach(id => {
        document.getElementById(id).style.display = (id === formId) ? 'block' : 'none';
    });
}

// 預設顯示 loginForm（如果沒有用 style 設定，也可以在這裡控制）
window.onload = () => showForm('loginForm');




// 頁面切換功能
function showSection(sectionId) {
    // 隱藏所有內容區塊
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });

    // 移除所有導航項目的 active 狀態
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });

    // 顯示選中的內容區塊
    document.getElementById(sectionId).classList.add('active');

    // 設定對應導航項目為 active
    event.target.classList.add('active');
}

// 驗證方式切換
function setVerificationMethod(method) {
    // 移除所有按鈕的 active 狀態
    document.querySelectorAll('.verification-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // 設定點擊的按鈕為 active
    event.target.classList.add('active');

    // 隱藏所有驗證區塊
    document.querySelectorAll('.verification-section').forEach(section => {
        section.style.display = 'none';
    });

    // 顯示對應的驗證區塊
    switch (method) {
        case 'phone':
            document.getElementById('phoneVerification').style.display = 'block';
            break;
        case 'password':
            document.getElementById('passwordChange').style.display = 'block';
            break;
        case 'update':
            document.getElementById('profileUpdate').style.display = 'block';
            break;
        case 'deleteAccount':
            document.getElementById('deleteAccountPage').style.display = 'block';
            break;
    }
}

// 發送驗證碼
function sendVerificationCode() {
    const phoneInput = document.getElementById('phoneInput');
    if (!phoneInput.value) {
        showMessage('請先輸入手機號碼', 'error');
        return;
    }

    // 模擬發送驗證碼
    showMessage('驗證碼已發送至您的手機', 'success');

    // 按鈕倒數計時
    const btn = event.target;
    let countdown = 60;
    btn.disabled = true;
    btn.textContent = `${countdown}秒後重新發送`;

    const timer = setInterval(() => {
        countdown--;
        if (countdown > 0) {
            btn.textContent = `${countdown}秒後重新發送`;
        } else {
            btn.disabled = false;
            btn.textContent = '發送驗證碼';
            clearInterval(timer);
        }
    }, 1000);
}

// 重設表單
function resetForm() {
    document.getElementById('profileForm').reset();
    document.getElementById('accountInput').value = '0920****04';
}

// 顯示訊息
function showMessage(text, type) {
    const messageElement = document.getElementById(type === 'success' ? 'successMessage' : 'errorMessage');
    messageElement.textContent = text;
    messageElement.style.display = 'block';

    // 3秒後自動隱藏
    setTimeout(() => {
        messageElement.style.display = 'none';
    }, 3000);
}

// 表單提交處理
document.getElementById('profileForm').addEventListener('submit', function (e) {
    e.preventDefault();

    // 模擬提交處理
    showMessage('資料更新成功！', 'success');
});


