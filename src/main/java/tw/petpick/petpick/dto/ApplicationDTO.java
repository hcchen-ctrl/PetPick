package tw.petpick.petpick.dto;

import java.time.LocalDateTime;

import lombok.Data;
import tw.petpick.petpick.model.AdoptApplication;
import tw.petpick.petpick.model.AdoptPost;

@Data
public class ApplicationDTO {
    private Long id;
    private Long postId;
    private Long applicantUserId;
    private String message;
    private String status;
    private Long reviewedByEmployeeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectReason;

    private PostSummaryDTO post; // 讓前端可顯示貼文縮圖/標題

    public static ApplicationDTO from(AdoptApplication a, AdoptPost p){
        ApplicationDTO d = new ApplicationDTO();
        d.id = a.getId();
        d.postId = a.getPostId();
        d.applicantUserId = a.getApplicantUserId();
        d.message = a.getMessage();
        d.status = a.getStatus().name(); // enum 轉字串
        d.reviewedByEmployeeId = a.getReviewedByEmployeeId();
        d.createdAt = a.getCreatedAt();
        d.updatedAt = a.getUpdatedAt();
        d.approvedAt = a.getApprovedAt();
        d.rejectedAt = a.getRejectedAt();
        d.rejectReason = a.getRejectReason();
        if (p != null) d.post = PostSummaryDTO.from(p);
        return d;
    }
}
