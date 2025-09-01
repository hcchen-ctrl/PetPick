(() => {
    if (window.__petpickThemeInited) return;   // 防重覆初始化
    window.__petpickThemeInited = true;

    let lastScrollTop = 0;

    const navbar = document.querySelector('.navbar');

    window.addEventListener("scroll", function () {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

        if (scrollTop > lastScrollTop) {
            navbar.classList.add("hide-navbar");
        } else {
            navbar.classList.remove("hide-navbar");
        }

        lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;
    }, false);

    // 置頂按鈕功能
    const backToTopBtn = document.getElementById("backToTop");
    if (backToTopBtn) {
        window.addEventListener("scroll", () => {
            if (window.scrollY > 200) {
                backToTopBtn.style.display = "flex";
            } else {
                backToTopBtn.style.display = "none";
            }
        });

        backToTopBtn.addEventListener("click", function () {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }
})();