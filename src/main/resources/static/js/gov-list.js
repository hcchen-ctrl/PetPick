const getSexIcon = (sex) => {
    if (!sex || typeof sex !== 'string') return '';
    const s = sex.toLowerCase();
    if (s.includes('f') || s.includes('母')) {
        return '<span class="sex-icon"><img src="/images/female.png" alt="母" /></span>';
    }
    if (s.includes('m') || s.includes('公')) {
        return '<span class="sex-icon"><img src="/images/male.png" alt="公" /></span>';
    }
    return '';
};

const getAnimalIcon = (text = "") => {
    if (text.includes('犬') || text.includes('狗')) return '🐶';
    if (text.includes('貓')) return '🐱';
    if (text.includes('兔')) return '🐰';
    if (text.includes('鼠')) return '🐭';
    if (text.includes('鳥') || text.includes('鸚鵡')) return '🐦';
    if (text.includes('龜')) return '🐢';
    if (text.includes('蛇')) return '🐍';
    return '🐾';
};

const translateAge = (age) => {
    return age === 'CHILD' ? '幼年' : age === 'ADULT' ? '成年' : '不明';
};

const translateBodytype = (type) => {
    switch (type) {
        case "SMALL": return "小型";
        case "MEDIUM": return "中型";
        case "BIG": return "大型";
        default: return type;
    }
};


let currentPage = 0;
const pageSize = 12;

const loadPets = () => {
    const shelter = document.getElementById("shelterFilter").value;
    const kind = document.getElementById("kindFilter").value;
    const sex = document.getElementById("sexFilter").value;
    const age = document.getElementById("ageFilter").value;
    const keyword = document.getElementById("keyword").value;

    const url = new URL("/api/pets", window.location.origin);
    url.searchParams.set("page", currentPage);
    url.searchParams.set("size", pageSize);
    if (shelter) url.searchParams.set("shelter", shelter);
    if (kind) url.searchParams.set("kind", kind);
    if (sex) url.searchParams.set("sex", sex);
    if (age) url.searchParams.set("age", age);
    if (keyword) url.searchParams.set("keyword", keyword);

    const fullSearch = document.getElementById("fullSearchToggle")?.checked;
    if (fullSearch) url.searchParams.set("fullSearch", "true");

    showLoading();

    fetch(url)
        .then(res => res.json())
        .then(data => {

            document.getElementById("page-info").textContent =
                `第 ${data.number + 1} / ${data.totalPages} 頁`;

            const container = document.getElementById("pet-list");
            container.innerHTML = "";

            if (data.content.length === 0) {
                container.innerHTML = `<div class="col-12 text-center text-muted mt-4">找不到符合條件的毛孩喔！</div>`;
                document.getElementById("pagination").innerHTML = "";
                return;
            }

            data.content.forEach(pet => {
                const sexIcon = getSexIcon(pet.animal_sex);
                const sexNotice = sexIcon === '' ? '<br><span class="text-danger">＊未提供性別資訊</span>' : '';

                container.innerHTML += `
                <div class="col-12 col-sm-6 col-md-4 mb-4">
                    <div class="card pet-card">
                        <img src="${pet.album_file}" class="card-img-top" alt="毛孩照片"
                        onerror="this.onerror=null; this.src='./images/no-image.jpg';">
                        <div class="card-body">
                            <h5 class="card-title">
                               ${translateBodytype(pet.animal_bodytype)}
                               ${pet.animal_colour}
                               ${sexIcon}${pet.animal_Variety}
                               ${getAnimalIcon(pet.animal_Variety)}
                            </h5>
                            <p class="card-text">
                                <strong>收容編號：</strong>${pet.animal_subid}<br>
                                <strong>收容所：</strong>${pet.animal_place}<br>
                                <strong>電話：</strong>${pet.shelter_tel}<br>
                                <strong>地址：</strong>${pet.shelter_address}<br>
                                <strong>年齡：</strong>${translateAge(pet.animal_age)}<br>
                                <strong>備註：</strong>
                                ${pet.animal_remark || '無'}<br>
                                ${sexNotice}
                            </p>
                            <a href="tel:${pet.shelter_tel}" class="btn btn-warning btn-sm">撥打電話</a>
                        </div>
                    </div>
                </div>
                `;
            });

            const pagination = document.getElementById("pagination");
            pagination.innerHTML = "";

            const totalPages = data.totalPages;
            const current = data.number;

            const startPage = Math.max(0, current - 5);
            const endPage = Math.min(totalPages, current + 6); // +6 才能顯示 11 頁

            // ✅ 第一頁 + 上一頁，含 disabled
            pagination.innerHTML += `
                <li class="page-item ${current === 0 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="0" title="第一頁"><<</a>
                </li>
                <li class="page-item ${current === 0 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${current - 1}" title="上一頁"><</a>
                </li>
            `;

            // 中央頁碼
            for (let i = startPage; i < endPage; i++) {
                pagination.innerHTML += `
                    <li class="page-item ${i === current ? 'active' : ''}">
                        <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                    </li>
                `;
            }

            // ✅ 下一頁 + 最後一頁，含 disabled
            pagination.innerHTML += `
                <li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${current + 1}" title="下一頁">></a>
                </li>
                <li class="page-item ${current === totalPages - 1 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${totalPages - 1}" title="最後一頁">>></a>
                </li>
            `;

            // 點擊綁定 & 捲動到頂
            document.querySelectorAll("#pagination .page-link").forEach(el => {
                const page = el.getAttribute("data-page");
                if (page !== null) {
                    el.addEventListener("click", (e) => {
                        e.preventDefault();
                        currentPage = parseInt(page);
                        loadPets();
                        window.scrollTo({ top: 0, behavior: "smooth" });
                    });
                }
            });

            // 前往頁按鈕事件
            document.getElementById("gotoPageBtn")?.addEventListener("click", () => {
                const input = document.getElementById("gotoPageInput");
                const val = parseInt(input.value);
                if (!isNaN(val) && val >= 1 && val <= totalPages) {
                    currentPage = val - 1;
                    loadPets();
                    window.scrollTo({ top: 0, behavior: "smooth" });
                } else {
                    input.classList.add("is-invalid");
                }
            });

        })
        .catch(err => {
            console.error('載入失敗：', err);
            alert('資料載入失敗，請稍後再試。');
        })
        .finally(() => {
            hideLoading();
        });
};



const loadShelters = () => {
    fetch("/api/shelters")
        .then(res => res.json())
        .then(data => {
            const sel = document.getElementById("shelterFilter");
            sel.innerHTML = '<option value="">所有收容所</option>';
            data.forEach(name => {
                const opt = document.createElement("option");
                opt.value = name;
                opt.textContent = name;
                sel.appendChild(opt);
            });
        });
};

const sexMap = { M: "公", F: "母" };
const ageMap = { CHILD: "幼年", ADULT: "成年" };

const loadSexes = () => {
    fetch("/api/sexes")
        .then(res => res.json())
        .then(data => {
            const sel = document.getElementById("sexFilter");
            sel.innerHTML = '<option value="">所有性別</option>';
            data.forEach(sex => {
                const opt = document.createElement("option");
                opt.value = sex;
                opt.textContent = sexMap[sex.toUpperCase()] || sex;
                sel.appendChild(opt);
            });
        });
};

const loadAges = () => {
    fetch("/api/ages")
        .then(res => res.json())
        .then(data => {
            const sel = document.getElementById("ageFilter");
            sel.innerHTML = '<option value="">所有年齡</option>';
            data.forEach(age => {
                const opt = document.createElement("option");
                opt.value = age;
                opt.textContent = ageMap[age.toUpperCase()] || age;
                sel.appendChild(opt);
            });
        });
};

const loadKinds = () => {
    fetch("/api/kinds")
        .then(res => res.json())
        .then(data => {
            const sel = document.getElementById("kindFilter");
            sel.innerHTML = '<option value="">所有種類</option>';
            data.forEach(kind => {
                const opt = document.createElement("option");
                opt.value = kind;
                opt.textContent = kind;
                sel.appendChild(opt);
            });
        });
};

document.getElementById("shelterFilter").addEventListener("change", () => { currentPage = 0; loadPets(); });
document.getElementById("kindFilter").addEventListener("change", () => { currentPage = 0; loadPets(); });
document.getElementById("sexFilter").addEventListener("change", () => { currentPage = 0; loadPets(); });
document.getElementById("ageFilter").addEventListener("change", () => { currentPage = 0; loadPets(); });
document.getElementById("searchBtn").addEventListener("click", () => { currentPage = 0; loadPets(); });
document.getElementById("gotoPageInput").addEventListener("input", () => {
    document.getElementById("gotoPageInput").classList.remove("is-invalid");
});

loadShelters();
loadKinds();
loadSexes();
loadAges();
loadPets();
