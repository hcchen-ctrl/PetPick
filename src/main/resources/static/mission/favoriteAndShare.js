const favoriteBtn = document.getElementById("btn-favorite");
let isFavorited = false;

// 1. 初始化時檢查是否已收藏
async function initFavoriteCheck() {
    try {
        const res = await fetch(`/favorites/check?userId=${CURRENT_USER_ID}&missionId=${missionId}`);
        if (!res.ok) throw new Error("檢查收藏失敗");
        const data = await res.json();
        isFavorited = data.favorited;
        updateFavoriteUI();
    } catch (err) {
        console.error(err);
    }
}

// 2. 更新 UI (保持原樣)
function updateFavoriteUI() {
    if (isFavorited) {
        favoriteBtn.innerHTML = `<span class="material-icons">favorite</span>`;
    } else {
        favoriteBtn.innerHTML = `<span class="material-icons">favorite_border</span>`;
    }
}

// 3. 點擊切換收藏
favoriteBtn.addEventListener("click", async () => {
    const url = `/favorites?userId=${CURRENT_USER_ID}&missionId=${missionId}`;
    try {
        if (isFavorited) {
            // 取消收藏
            const res = await fetch(url, { method: "DELETE" });
            if (!res.ok) throw new Error("取消收藏失敗");
            isFavorited = false;
            updateFavoriteUI();
        } else {
            // 新增收藏
            const res = await fetch(url, { method: "POST" });
            if (!res.ok) throw new Error("收藏失敗");
            isFavorited = true;
            updateFavoriteUI();
        }
    } catch (err) {
        console.error(err);
    }
});

// 初始化
initFavoriteCheck();



//分享
const shareBtn = document.getElementById('btn-share');
shareBtn?.addEventListener('click', async () => {
    const title = document.getElementById('title')?.textContent || 'PetPick 任務';
    const url = window.location.href;

    if (navigator.share) {
        try {
            await navigator.share({
                title,
                text: '看看這個任務～',
                url
            });
        } catch (err) { }
        return;
    }
});