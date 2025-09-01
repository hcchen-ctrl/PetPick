package tw.petpick.petpick.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.dto.ApplicationDTO;
import tw.petpick.petpick.model.AdoptApplication;
import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.PetReportAdoption;
import tw.petpick.petpick.model.enums.ApplicationStatus;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.model.enums.SourceType;
import tw.petpick.petpick.repository.AdoptApplicationRepository;
import tw.petpick.petpick.repository.AdoptPostRepository;
import tw.petpick.petpick.repository.PetReportAdoptionRepository;
import tw.petpick.petpick.repository.UserinfoRepository;

@Service
@RequiredArgsConstructor
public class AdoptApplicationService {

    private final AdoptApplicationRepository appRepo;
    private final AdoptPostRepository postRepo;
    private final UserinfoRepository userRepo;
    private final PetReportAdoptionRepository petReportAdoptionRepo;

// 送出申請（官方貼文才開放）
    public ApplicationDTO apply(Long postId, Long uid, String message){
        AdoptPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "貼文不存在"));

        if (post.getSourceType() != SourceType.platform)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "這是民眾貼文，直接聯絡即可");

        if (post.getStatus() != PostStatus.approved)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "貼文未上架，無法申請");

        if (appRepo.existsByPostIdAndStatus(postId, ApplicationStatus.approved))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此貼文已完成配對");

        var existed = appRepo.findTopByPostIdAndApplicantUserIdOrderByIdDesc(postId, uid).orElse(null);
        if (existed != null) {
            if (existed.getStatus() == ApplicationStatus.pending || existed.getStatus() == ApplicationStatus.approved)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "你已申請過了");

            // 允許退回/取消後重送
            existed.setStatus(ApplicationStatus.pending);
            existed.setMessage(message);
            existed.setReviewedByUserId(null);
            existed.setApprovedAt(null);
            existed.setRejectedAt(null);
            existed.setRejectReason(null);
            existed.setUpdatedAt(LocalDateTime.now());

            var saved = appRepo.save(existed);                                    // ✅ 真的存
            var u = userRepo.findById(uid).orElse(null);
            return ApplicationDTO.from(saved, post, u == null ? null : u.getUsername()); // ✅ 帶名字
        }

        AdoptApplication a = new AdoptApplication();
        a.setPostId(postId);
        a.setApplicantUserId(uid);
        a.setMessage(message);
        a.setStatus(ApplicationStatus.pending);

        var saved = appRepo.save(a);
        var u = userRepo.findById(uid).orElse(null);
        return ApplicationDTO.from(saved, post, u == null ? null : u.getUsername());
    }

    // 我的申請列表
    public Page<ApplicationDTO> myApps(Long uid, String status, Pageable pageable){
        Page<AdoptApplication> page =
            (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? appRepo.findByApplicantUserId(uid, pageable)
                : appRepo.findByApplicantUserIdAndStatus(uid, ApplicationStatus.valueOf(status.toLowerCase()), pageable);

        return page.map(a -> {
            AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
            var u = userRepo.findById(a.getApplicantUserId()).orElse(null);
            return ApplicationDTO.from(a, p, u == null ? null : u.getUsername());
        });
    }

    // 取消（只有 pending 才能取消）
    public void cancel(Long appId, Long uid){
        AdoptApplication a = appRepo.findById(appId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Objects.equals(a.getApplicantUserId(), uid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有審核中才能取消");
        a.setStatus(ApplicationStatus.cancelled);
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);
    }

    // ===== 管理員 =====

    public Page<ApplicationDTO> adminSearch(String status, String species, String q, Pageable pageable){
        ApplicationStatus st = null;
        if (status != null && !"all".equalsIgnoreCase(status)) {
            st = ApplicationStatus.valueOf(status.toLowerCase());
        }
        var page = appRepo.adminSearch(st, species, q, pageable);
        return page.map(a -> {
            AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
            var u = userRepo.findById(a.getApplicantUserId()).orElse(null);
            return ApplicationDTO.from(a, p, u == null ? null : u.getUsername());
        });
    }

    public ApplicationDTO get(Long id){
    AdoptApplication a = appRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    AdoptPost p = postRepo.findById(a.getPostId()).orElse(null);
    var u = userRepo.findById(a.getApplicantUserId()).orElse(null);   // ← 查名字
    return ApplicationDTO.from(a, p, u == null ? null : u.getUsername()); // ← 把名字帶進 DTO
    }

    @Transactional
    public void approve(Long id, Long reviewerId){
        AdoptApplication a = appRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能核准審核中申請");

        Long postId = a.getPostId();

        if (appRepo.existsByPostIdAndStatus(postId, ApplicationStatus.approved))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此貼文已核准他人");

        // 1) 核准
        a.setStatus(ApplicationStatus.approved);
        a.setReviewedByUserId(reviewerId);
        a.setApprovedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);

        // 2) 退回其它 pending
        appRepo.rejectOthersPendingOfPost(postId, a.getId(), reviewerId, "已由其他申請者獲准");

        // 3) 關閉貼文
        AdoptPost post = postRepo.findById(postId).orElse(null);
        if (post != null && post.getStatus() != PostStatus.closed) {
            post.setStatus(PostStatus.closed);
            postRepo.save(post);
        }

        // 4) 建立收養追蹤對象（若不存在）
        createReportAdoptionIfAbsent(a, post);
    }

    private void createReportAdoptionIfAbsent(AdoptApplication app, AdoptPost cachedPost) {
        Long postId    = app.getPostId();
        Long adopterId = app.getApplicantUserId();

        if (petReportAdoptionRepo.existsByPostIdExtAndAdopterUserIdExt(postId, adopterId))
            return;

        AdoptPost p = (cachedPost != null) ? cachedPost : postRepo.findById(postId).orElse(null);
        var u = userRepo.findById(adopterId).orElse(null);
        if (p == null || u == null) return;

        LocalDate date = (app.getApprovedAt() != null)
                ? app.getApprovedAt().toLocalDate()
                : LocalDate.now();

        var entity = PetReportAdoption.builder()
                .ownerName(u.getUsername())
                .petName(p.getTitle())
                .adoptionDate(date)
                .status("active")
                .requiredReports(12)          // **一定給值**
                .postIdExt(p.getId())
                .adopterUserIdExt(u.getUserId())
                .imageUrl(firstNonBlank(p.getImage1(), p.getImage2(), p.getImage3()))
                .build();

        try {
            petReportAdoptionRepo.save(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException ignore) {
            // 競態建立，無視
        }
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x;
        return null;
    }

    public void reject(Long id, Long reviewerId, String reason){
        AdoptApplication a = appRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.getStatus() != ApplicationStatus.pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        a.setStatus(ApplicationStatus.rejected);
        a.setReviewedByUserId(reviewerId);        // ✅ 改這裡
        a.setRejectedAt(LocalDateTime.now());
        a.setRejectReason(reason);
        a.setUpdatedAt(LocalDateTime.now());
        appRepo.save(a);
    }
}
