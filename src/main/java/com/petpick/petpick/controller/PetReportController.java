package tw.petpick.petpick.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import tw.petpick.petpick.dto.AdoptionView;
import tw.petpick.petpick.entity.PetReportAdoption;
import tw.petpick.petpick.entity.PetReportFeedback;
import tw.petpick.petpick.model.ReportStatus;
import tw.petpick.petpick.repository.PetReportAdoptionRepository;
import tw.petpick.petpick.repository.PetReportFeedbackRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/petreport")
public class PetReportController {

    @Autowired
    private PetReportAdoptionRepository adoptionRepo;
    @Autowired
    private PetReportFeedbackRepository feedbackRepo;

    // === 路徑工具（跟 WebResourceConfig 同邏輯） ===
    private static java.nio.file.Path moduleRoot() {
        try {
            // .../target/classes → parent=.../target → parent=.../專案根
            return java.nio.file.Paths.get(
                    PetReportController.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent()
                    .getParent().normalize();
        } catch (Exception e) {
            return java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
    }

    private static final java.nio.file.Path UPLOADS_DIR = moduleRoot().resolve("uploads").normalize();
    private static final java.nio.file.Path FEEDBACK_DIR = UPLOADS_DIR.resolve("feedback");
    private static final String FEEDBACK_URL_PREFIX = "/uploads/feedback/";

    // 相對路徑設定（有預設值，application.properties 可覆寫）
    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    @Value("${app.upload.feedback-dir:feedback}")
    private String feedbackDir;

    // dataURL → 寫檔到 uploads/feedback 並回傳「相對URL」；不是 dataURL 就原樣回傳
    private static String normalizeImageUrl(String maybeDataUrl, Long adoptionId) {
        if (maybeDataUrl == null || maybeDataUrl.isBlank())
            return null;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$")
                .matcher(maybeDataUrl.trim());

        // 已經是 /uploads/... 或 http(s):// 就直接回傳
        if (!m.find())
            return maybeDataUrl.trim();

        try {
            String ext = m.group(1);
            if ("jpg".equalsIgnoreCase(ext))
                ext = "jpeg";
            byte[] bytes = java.util.Base64.getDecoder().decode(m.group(2));

            java.nio.file.Files.createDirectories(FEEDBACK_DIR); // 只建這個，不會冒出別夾

            String filename = "fb-" + adoptionId + "-" + System.currentTimeMillis() + "." + ext.toLowerCase();
            java.nio.file.Path out = FEEDBACK_DIR.resolve(filename);

            java.nio.file.Files.write(out, bytes,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

            // 方便你核對實際落地位置
            System.out.printf("[PetReport] write -> %s (url=%s)%n", out, FEEDBACK_URL_PREFIX + filename);

            // DB 只存相對 URL，前端用 /uploads/** 拿
            return FEEDBACK_URL_PREFIX + filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* ======================= 會員清單：need / done ======================= */

    /** 需要回報（回報次數 < 需求次數，且狀態 active） */
    @GetMapping("/adoptions/need")
    public List<AdoptionView> listNeed() {
        return adoptionRepo.listNeedAdoptions();
    }

    /** 已完成/無須回報（回報次數 >= 需求次數 或 狀態非 active） */
    @GetMapping("/adoptions/done")
    public List<AdoptionView> listDone() {
        return adoptionRepo.listDoneAdoptions();
    }

    /* ======================= 單筆領養與回報列表 ======================= */

    /** 左側卡片資料（找不到回 204） */
    @GetMapping("/adoptions/{id}")
    public ResponseEntity<AdoptionView> getAdoption(@PathVariable Long id) {
        return adoptionRepo.findViewById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 右側回報列表（依日期新→舊）；用 DTO 避免 lazy 代理序列化問題 */
    @GetMapping("/adoptions/{id}/reports")
    public List<FeedbackView> listReports(@PathVariable("id") Long adoptionId) {
        return feedbackRepo.findByAdoptionIdOrderByReportDateDesc(adoptionId)
                .stream().map(FeedbackView::from).collect(Collectors.toList());
    }

    /* ======================= 新增回報（JSON + 可含 dataURL 圖片） ======================= */

    /** 前端 JSON payload */
    public static class CreateReportPayload {
        public String reportDate; // yyyy-MM-dd（可空→用今天）
        public String notes; // 可空
        public String status; // SUBMITTED / VERIFIED / REJECTED（預設 SUBMITTED）
        public String imageUrl; // 可為 dataURL；也可直接 /uploads/... 或 http(s)://
    }

    private static final Pattern DATA_URL = Pattern.compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$");

    private Path uploadsRoot() {
        // 以目前工作目錄為基準（相對）
        return Paths.get(System.getProperty("user.dir"))
                .resolve(baseDir)
                .toAbsolutePath()
                .normalize();
    }

    /** 新增一筆回報（前端已支援 204，因此回 204 No Content） */
    @PostMapping("/adoptions/{id}/reports")
    public ResponseEntity<Void> createReport(@PathVariable("id") Long adoptionId,
            @RequestBody CreateReportPayload payload) {
        if (payload == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        // 綁定 adoption（避免前端竄改）
        PetReportAdoption adoption = PetReportAdoption.builder().id(adoptionId).build();

        // 日期：空值用今天
        LocalDate date;
        try {
            date = (payload.reportDate == null || payload.reportDate.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(payload.reportDate.trim());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 圖片處理（可能是 dataURL）
        String imageUrl = normalizeImageUrl(payload.imageUrl, adoptionId);

        // 狀態（預設 SUBMITTED；大小寫不敏感）
        ReportStatus status = ReportStatus.SUBMITTED;
        if (payload.status != null && !payload.status.isBlank()) {
            try {
                status = ReportStatus.valueOf(payload.status.trim().toUpperCase());
            } catch (IllegalArgumentException ignore) {
            }
        }

        feedbackRepo.save(PetReportFeedback.builder()
                .adoption(adoption)
                .reportDate(date)
                .notes(payload.notes)
                .imageUrl(imageUrl)
                .status(status)
                .build());

        return ResponseEntity.noContent().build();
    }

    /* ======================= 管理員：刪除 / 搜尋 ======================= */

    /** 刪除單筆回報 */
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        if (feedbackRepo.existsById(id)) {
            feedbackRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /** 全域關鍵字搜尋（以 notes 為主）；回傳 DTO 避免 lazy 代理序列化 */
    @GetMapping("/search")
    public List<FeedbackView> search(@RequestParam(name = "q", required = false) String keyword) {
        keyword = (keyword == null) ? "" : keyword.trim();
        if (keyword.length() < 2)
            return List.of();
        return feedbackRepo.searchByNotes(keyword)
                .stream().map(FeedbackView::from).collect(Collectors.toList());
    }

    /* ========= 僅回傳必要欄位的 View DTO（避免序列化 adoption lazy 代理） ========= */
    public static class FeedbackView {
        public Long id;
        public Long adoptionId;
        public LocalDate reportDate;
        public String notes;
        public String imageUrl;
        public ReportStatus status;

        public static FeedbackView from(PetReportFeedback f) {
            FeedbackView v = new FeedbackView();
            v.id = f.getId();
            v.adoptionId = (f.getAdoption() != null ? f.getAdoption().getId() : null);
            v.reportDate = f.getReportDate();
            v.notes = f.getNotes();
            v.imageUrl = f.getImageUrl();
            v.status = f.getStatus();
            return v;
        }
    }

    // 取得目前登入會員的「我的收養清單」
    @GetMapping("/my-adoptions")
    public ResponseEntity<?> myAdoptions(HttpServletRequest req) {
        // TODO: 依照你的登入機制取得 userId
        // 這裡假設 userId 存在 session 裡，key = "uid"
        Long uid = (Long) req.getSession().getAttribute("uid");
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NOT_LOGIN");
        }

        return ResponseEntity.ok(adoptionRepo.findMyActiveAdoptions(uid));
    }

}
