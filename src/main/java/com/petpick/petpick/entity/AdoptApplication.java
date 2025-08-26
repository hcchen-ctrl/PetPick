package com.petpick.petpick.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import com.petpick.petpick.model.enums.ApplicationStatus;

@Entity
@Table(name = "adopt_applications")
@Data
public class AdoptApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 對應 DB: post_id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // 對應 DB: applicant_user_id
    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    // 對應 DB: message (text)
    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", insertable = false, updatable = false)
    private UserEntity applicant;


//    // 對應 DB: status enum('pending','approved','rejected','cancelled')
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false, length = 20)
//    private ApplicationStatus status = ApplicationStatus.pending; // ← 小寫

    //GPT修改(和併用)
//    @ManyToOne
//    @JoinColumn(name = "post_id", nullable = false)
//    private AdoptPost post;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    // 對應 DB: reviewed_by_employee_id
    @Column(name = "reviewed_by_employee_id")
    private Long reviewedByEmployeeId;

    // 對應 DB: created_at / updated_at / approved_at / rejected_at / reject_reason
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason")
    private String rejectReason;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
