import { getAuth, logout } from '/adopt/auth.js';

function attachMenuToButton(btn, auth) {
    if (!btn) return;

    const wrap = document.createElement('div');
    wrap.className = 'dropdown d-inline-block';
    btn.parentNode.insertBefore(wrap, btn);
    wrap.appendChild(btn);

    btn.classList.add('dropdown-toggle');
    btn.setAttribute('data-bs-toggle', 'dropdown');
    btn.setAttribute('data-bs-display', 'static');
    btn.setAttribute('aria-expanded', 'false');

    const menu = document.createElement('ul');
    menu.className = 'dropdown-menu dropdown-menu-end';

    if (auth.loggedIn) {
        // ★ Admin 也保留「刊登送養」（他送出會直接上架）
        const items = (auth.role === 'ADMIN')
            ? [
                { text: '我的帳戶', href: '/profile.html' },
                { text: '貼文審核', href: '/post-review.html' },   // post-review 有 Tab 也能到 apply-review
                { text: '申請審核', href: '/apply-review.html' }, // 想直接進申請審核就留這個
                { divider: true },
                { text: '刊登送養', href: '/post-adopt.html' },
                { divider: true },
                { text: '登出', href: '#', id: 'logoutLink', danger: true },
            ]
            : [
                { text: '我的帳戶', href: '/profile.html' },
                { text: '我的刊登', href: '/my-adopt-progress.html?status=pending' },
                { text: '我的申請', href: '/my-apply.html?status=pending' }, // ★ 新增
                { divider: true },
                { text: '刊登送養', href: '/post-adopt.html' },
                { divider: true },
                { text: '登出', href: '#', id: 'logoutLink', danger: true },
            ];

        menu.innerHTML = items.map(it => {
            if (it.divider) return '<li><hr class="dropdown-divider"></li>';
            const cls = it.danger ? 'dropdown-item text-danger' : 'dropdown-item';
            const idAttr = it.id ? ` id="${it.id}"` : '';
            return `<li><a class="${cls}" href="${it.href}"${idAttr}>${it.text}</a></li>`;
        }).join('');
    } else {
        menu.innerHTML = `
      <li><a class="dropdown-item" href="/login.html" id="loginLink">登入</a></li>
      <li><a class="dropdown-item" href="/register.html">註冊</a></li>
    `;
    }

    wrap.appendChild(menu);

    if (!auth.loggedIn) {
        menu.querySelector('#loginLink')?.addEventListener('click', () => {
            sessionStorage.setItem('redirect', location.pathname + location.search);
        });
    } else {
        menu.querySelector('#logoutLink')?.addEventListener('click', async (e) => {
            e.preventDefault();
            await logout();
            location.reload();
        });
    }
}

(async () => {
    const auth = await getAuth();
    document.getElementById('authArea')?.replaceChildren(); // 清空
    attachMenuToButton(document.getElementById('desktopAccountBtn'), auth);
    attachMenuToButton(document.getElementById('mobileAccountBtn'), auth);
})();
