export async function getAuth() {
    try {
        const r = await fetch('/api/auth/status', { credentials: 'include' });
        if (!r.ok) return { loggedIn: false };
        return await r.json(); // { loggedIn, uid?, role?, name? }
    } catch {
        return { loggedIn: false };
    }
}

export async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    } finally {
        // no-op
    }
}

export async function requireLogin() {
    const a = await getAuth();
    if (!a.loggedIn) {
        // 登入後導回原頁
        sessionStorage.setItem('redirect', location.pathname + location.search);
        location.href = '/login.html';
        throw new Error('redirecting to login');
    }
    return a;
}

export async function requireAdmin() {
    const a = await requireLogin();
    if (a.role !== 'ADMIN') {
        alert('此頁面僅限管理員');
        location.href = '/index.html';
        throw new Error('redirecting: not admin');
    }
    return a;
}
