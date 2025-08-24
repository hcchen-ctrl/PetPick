let selectedFiles = [];
const selectedTags = new Set();

$(function () {
  const $form = $('#taskForm');

  // ---------- 預覽欄位綁定 ----------
  const fieldIds = ['#title', '#description', '#city', '#district', '#starttime', '#endtime', '#price', '#imageUrl', '#petname', '#petage', '#petgender', '#phone'];
  fieldIds.forEach(sel => $(sel).on('input', updatePreview));

  // ---------- 時間不得早於現在 ----------
  (function setMinDatetime() {
    const pad = (n) => String(n).padStart(2, '0');
    const d = new Date();
    const v = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    $('#starttime,#endtime').attr('min', v);
  })();

  // ---------- 多圖選擇與預覽 ----------
  $('#imageUrl').on('change', function (ev) {
    const newFiles = Array.from(ev.target.files);
    if (selectedFiles.length + newFiles.length > 5) {
      alert('最多只能上傳 5 張圖片');
      $(this).val('');
      return;
    }
    selectedFiles = selectedFiles.concat(newFiles);
    renderImagePreviews();
    $(this).val('');
  });

  function renderImagePreviews() {
    const $previewContainer = $('#previewImages').empty();
    const $carousel = $('#carouselImages').empty();

    selectedFiles.forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const $wrapper = $('<div class="position-relative"></div>');
        const $img = $('<img class="img-thumbnail" />').attr('src', e.target.result).css({ width: '120px', height: '120px', objectFit: 'cover' });
        const $close = $('<button type="button" class="btn btn-sm position-absolute top-0 end-0">&times;</button>')
          .css('transform', 'translate(50%, -50%)')
          .on('click', () => {
            selectedFiles.splice(index, 1);
            renderImagePreviews();
          });

        $wrapper.append($img, $close);
        $previewContainer.append($wrapper);

        const isActive = index === 0 ? ' active' : '';
        const $slide = $(`<div class="carousel-item${isActive}"><img class="d-block w-100" style="height:300px;object-fit:cover;"></div>`);
        $slide.find('img').attr('src', e.target.result);
        $carousel.append($slide);
      };
      reader.readAsDataURL(file);
    });
  }

  // ---------- 標籤選擇 ----------
  const $hiddenTagInput = $('#selectedTags');
  $('.tag-btn').on('click', function () {
    const id = parseInt($(this).data('value'), 10);
    if (selectedTags.has(id)) {
      selectedTags.delete(id);
      $(this).removeClass('btn-secondary').addClass('btn-outline-secondary');
    } else {
      selectedTags.add(id);
      $(this).removeClass('btn-outline-secondary').addClass('btn-secondary');
    }
    $hiddenTagInput.val([...selectedTags].join(','));
    updatePreview();
  });

  // ---------- 城市/地區 ----------
  const TW_AREAS = {
    "臺北市": ["中正區","大同區","中山區","松山區","大安區","萬華區","信義區","士林區","北投區","內湖區","南港區","文山區"],
    "新北市": ["萬里區","金山區","板橋區","汐止區","深坑區","石碇區","瑞芳區","平溪區","雙溪區","貢寮區","新店區","坪林區","烏來區","永和區","中和區","土城區","三峽區","樹林區","鶯歌區","三重區","新莊區","泰山區","林口區","蘆洲區","五股區","八里區","淡水區","三芝區","石門區"],
    "桃園市": ["中壢區","平鎮區","龍潭區","楊梅區","新屋區","觀音區","桃園區","龜山區","八德區","大溪區","復興區","大園區","蘆竹區"],
    "臺中市": ["中區","東區","南區","西區","北區","北屯區","西屯區","南屯區","太平區","大里區","霧峰區","烏日區","豐原區","后里區","石岡區","東勢區","和平區","新社區","潭子區","大雅區","神岡區","大肚區","沙鹿區","龍井區","梧棲區","清水區","大甲區","外埔區","大安區"],
    "臺南市": ["中西區","東區","南區","北區","安平區","安南區","永康區","歸仁區","新化區","左鎮區","玉井區","楠西區","南化區","仁德區","關廟區","龍崎區","官田區","麻豆區","佳里區","西港區","七股區","將軍區","學甲區","北門區","新營區","後壁區","白河區","東山區","六甲區","下營區","柳營區","鹽水區","善化區","大內區","山上區","新市區","安定區"],
    "高雄市": ["新興區","前金區","苓雅區","鹽埕區","鼓山區","旗津區","前鎮區","三民區","楠梓區","小港區","左營區","仁武區","大社區","東沙群島","南沙群島","岡山區","路竹區","阿蓮區","田寮區","燕巢區","橋頭區","梓官區","彌陀區","永安區","湖內區","鳳山區","大寮區","林園區","鳥松區","大樹區","旗山區","美濃區","六龜區","內門區","杉林區","甲仙區","桃源區","那瑪夏區","茂林區","茄萣區"],
    "基隆市": ["仁愛區","信義區","中正區","中山區","安樂區","暖暖區","七堵區"],
    "新竹市": ["東區","北區","香山區"],
    "嘉義市": ["東區","西區"],
    "新竹縣": ["竹北市","湖口鄉","新豐鄉","新埔鎮","關西鎮","芎林鄉","寶山鄉","竹東鎮","五峰鄉","橫山鄉","尖石鄉","北埔鄉","峨眉鄉"],
    "苗栗縣": ["苗栗市","苑裡鎮","通霄鎮","竹南鎮","頭份市","後龍鎮","卓蘭鎮","大湖鄉","公館鄉","銅鑼鄉","南庄鄉","頭屋鄉","三義鄉","西湖鄉","造橋鄉","三灣鄉","獅潭鄉","泰安鄉"],
    "彰化縣": ["彰化市","鹿港鎮","和美鎮","線西鄉","伸港鄉","福興鄉","秀水鄉","花壇鄉","芬園鄉","員林市","溪湖鎮","田中鎮","大村鄉","埔鹽鄉","埔心鄉","永靖鄉","社頭鄉","二水鄉","北斗鎮","二林鎮","田尾鄉","埤頭鄉","溪州鄉","竹塘鄉","大城鄉","芳苑鄉"],
    "南投縣": ["南投市","中寮鄉","草屯鎮","國姓鄉","埔里鎮","仁愛鄉","名間鄉","集集鎮","水里鄉","魚池鄉","信義鄉","竹山鎮","鹿谷鄉"],
    "雲林縣": ["斗六市","斗南鎮","虎尾鎮","西螺鎮","土庫鎮","北港鎮","莿桐鄉","林內鄉","二崙鄉","崙背鄉","麥寮鄉","東勢鄉","褒忠鄉","臺西鄉","元長鄉","四湖鄉","口湖鄉","水林鄉","古坑鄉"],
    "嘉義縣": ["太保市","朴子市","布袋鎮","大林鎮","民雄鄉","溪口鄉","新港鄉","六腳鄉","東石鄉","義竹鄉","鹿草鄉","水上鄉","中埔鄉","竹崎鄉","梅山鄉","番路鄉","大埔鄉","阿里山鄉"],
    "屏東縣": ["屏東市","潮州鎮","東港鎮","恆春鎮","萬丹鄉","長治鄉","麟洛鄉","九如鄉","里港鄉","鹽埔鄉","高樹鄉","萬巒鄉","內埔鄉","竹田鄉","新埤鄉","枋寮鄉","新園鄉","崁頂鄉","林邊鄉","南州鄉","佳冬鄉","琉球鄉","車城鄉","滿州鄉","枋山鄉","三地門鄉","霧臺鄉","瑪家鄉","泰武鄉","來義鄉","春日鄉","獅子鄉"],
    "宜蘭縣": ["宜蘭市","頭城鎮","礁溪鄉","壯圍鄉","員山鄉","羅東鎮","三星鄉","大同鄉","五結鄉","冬山鄉","蘇澳鎮","南澳鄉"],
    "花蓮縣": ["花蓮市","鳳林鎮","玉里鎮","新城鄉","吉安鄉","壽豐鄉","光復鄉","豐濱鄉","瑞穗鄉","富里鄉","秀林鄉","萬榮鄉","卓溪鄉"],
    "臺東縣": ["臺東市","成功鎮","關山鎮","卑南鄉","鹿野鄉","池上鄉","東河鄉","長濱鄉","太麻里鄉","大武鄉","綠島鄉","海端鄉","延平鄉","金峰鄉","達仁鄉","蘭嶼鄉"],
    "澎湖縣": ["馬公市","湖西鄉","白沙鄉","西嶼鄉","望安鄉","七美鄉"],
    "金門縣": ["金城鎮","金沙鎮","金湖鎮","金寧鄉","烈嶼鄉","烏坵鄉"],
    "連江縣": ["南竿鄉","北竿鄉","莒光鄉","東引鄉"]
  };

  const $city = $('#city');
  const $district = $('#district');

  // 初始城市
  Object.keys(TW_AREAS).forEach((c) => $city.append(`<option value="${c}">${c}</option>`));

  // 選城市 → 產生地區
  $city.on('change', function () {
    const c = $(this).val();
    $district.prop('disabled', !c).empty();
    if (!c) {
      $district.append('<option value="" selected disabled>請先選擇城市</option>');
      return;
    }
    $district.append('<option value="" selected disabled>請選擇地區</option>');
    (TW_AREAS[c] || []).forEach((d) => $district.append(`<option value="${d}">${d}</option>`));

    // 更新預覽地點
    const dVal = $district.val() || '';
    $('#previewLocation').text(`${c || ''} ${dVal || ''}`.trim());
    updatePreview();
  });

  // 選地區 → 更新預覽
  $district.on('change', function () {
    const c = $city.val() || '';
    const d = $(this).val() || '';
    $('#previewLocation').text(`${c} ${d}`.trim());
    updatePreview();
  });

  // ---------- 送出 ----------
    $form.on('submit', async function (e) {
        e.preventDefault();

        // 時間檢查
        const now = new Date();
        const s = new Date($('#starttime').val());
        const ed = new Date($('#endtime').val());

        if (isNaN(s) || isNaN(ed)) {
            alert('請選擇開始與結束時間');
            return;
        }
        if (s < now) { alert('開始時間不能早於現在'); return; }
        if (ed < now) { alert('結束時間不能早於現在'); return; }
        if (ed <= s) { alert('結束時間必須晚於開始時間'); return; }

        const payload = {
            posterId: CURRENT_USER_ID,
            title: $('#title').val().trim(),
            description: $('#description').val().trim(),
            city: $('#city').val()?.trim() || '',
            district: $('#district').val()?.trim() || '',
            startTime: toIso($('#starttime').val()),
            endTime: toIso($('#endtime').val()),
            price: Number($('#price').val()),
            petName: $('#petname').val().trim(),
            petAge: $('#petage').val().trim(),
            petGender: $('#petgender').val(), // "公"/"母"
            contactPhone: $('#phone').val().trim(),
            tags: [...selectedTags]
        };

        const fd = new FormData();
        fd.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
        selectedFiles.slice(0, 5).forEach((f) => fd.append('images', f));

        const $submit = $(e.originalEvent.submitter || 'button[type="submit"]');
        $submit.prop('disabled', true);

        // 取得 XSRF Token 的函式
        function getCookie(name) {
            const value = `; ${document.cookie}`;
            const parts = value.split(`; ${name}=`);
            if (parts.length === 2) return parts.pop().split(';').shift();
        }
        const xsrfToken = getCookie('XSRF-TOKEN');

        try {
            const res = await fetch('/api/missions/upload', {
                method: 'POST',
                body: fd,
                credentials: 'include',  // 這行很重要，讓fetch帶cookie
                headers: {
                    'X-XSRF-TOKEN': xsrfToken // 加入 XSRF Token header
                }
            });

            const json = await res.json().catch(() => ({}));
            if (!res.ok) {
                alert('上傳失敗：' + (json.message || res.status));
                return;
            }
            alert('上傳成功');
        } catch (err) {
            console.error(err);
            alert('發生錯誤，請稍後重試');
        } finally {
            $submit.prop('disabled', false);
        }
    });


    // ---------- 首次更新預覽 ----------
  updatePreview();
});

// ---------- 工具：時間/預覽 ----------
function toIso(v) {
  if (!v) return null;
  const d = new Date(v);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

function updatePreview() {
  // 讀取欄位
  const title = $('#title').val() || '';
  const petname = $('#petname').val() || '';
  const petage = $('#petage').val() || '';
  const petgender = $('#petgender').val() || '';
  const phone = $('#phone').val() || '';
  const city = $('#city').val() || '';
  const district = $('#district').val() || '';
  const s = $('#starttime').val();
  const e = $('#endtime').val();
  const price = $('#price').val() || '';
  const description = $('#description').val() || '';
  const tags = $('#selectedTags').val() || '';

  const fmt = (v) => (v ? new Date(v).toLocaleString('zh-TW', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }) : '');

  // 寫入預覽
  $('#previewTitle').text(title);
  $('#previewPetName').text(petname);
  $('#previewPetAge').text(petage);
  $('#previewPetGender').text(petgender);
  $('#previewPhone').text(phone);
  $('#previewLocation').text(`${city} ${district}`.trim());
  $('#previewStartTime').text(fmt(s));
  $('#previewEndTime').text(fmt(e));
  $('#previewPrice').text(price);
  $('#previewTag').text(tags);
  $('#previewDescription').text(description);
}
