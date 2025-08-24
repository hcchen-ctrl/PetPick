// favorites.js
const KEY = 'petpick:favs';

function _load() {
    try { return new Set(JSON.parse(localStorage.getItem(KEY) || '[]')); }
    catch { return new Set(); }
}
function _save(set) {
    localStorage.setItem(KEY, JSON.stringify(Array.from(set)));
}

/** 取得所有收藏的 missionId（字串陣列） */
export function getFavs() {
    return Array.from(_load());
}

/** 是否已收藏 */
export function isFav(id) {
    if (id == null) return false;
    return _load().has(String(id));
}

/** 加入收藏（回傳 true 表成功加入 / false 已存在） */
export function addFav(id) {
    if (id == null) return false;
    const set = _load();
    const k = String(id);
    if (set.has(k)) return false;
    set.add(k); _save(set);
    return true;
}

/** 取消收藏（回傳 true 表成功刪除 / false 原本不存在） */
export function removeFav(id) {
    if (id == null) return false;
    const set = _load();
    const k = String(id);
    const ok = set.delete(k);
    _save(set);
    return ok;
}

/** 切換收藏（回傳 {liked:boolean}） */
export function toggleFav(id) {
    const liked = isFav(id) ? !removeFav(id) && false : addFav(id) && true;
    return { liked };
}

/** 綁定一顆收藏按鈕，幫你處理 UI 切換（icon、aria、title） */
export function bindFavButton(buttonEl, iconEl, missionId, opts = {}) {
    const color = opts.activeColor || '#ff0000';
    const toast = opts.toast || (()=>{});
    const refresh = () => {
        const liked = isFav(missionId);
        if (iconEl) iconEl.textContent = liked ? 'favorite' : 'favorite_border';
        if (iconEl) iconEl.style.color = liked ? color : '';
        if (buttonEl) {
            buttonEl.setAttribute('aria-pressed', liked ? 'true' : 'false');
            buttonEl.title = liked ? '取消收藏' : '加入收藏';
        }
    };
    refresh();
    buttonEl?.addEventListener('click', () => {
        const { liked } = toggleFav(missionId);
        refresh();
        toast(liked ? '已加入收藏' : '已取消收藏');
    });
    return { refresh };
}