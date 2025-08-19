package tw.petpick.petpick.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_reviews")
public class PostReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // adopt_posts.id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // MySQL 是 enum('approve','reject')，這裡用字串最保險
    @Column(name = "action", nullable = false, length = 10)
    private String action; // "approve" or "reject"

    @Column(name = "reason")
    private String reason;

    // employees.employee_id
    @Column(name = "reviewer_employee_id")
    private Integer reviewerEmployeeId;

    // DB 已有 DEFAULT CURRENT_TIMESTAMP
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // getter / setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getReviewerEmployeeId() { return reviewerEmployeeId; }
    public void setReviewerEmployeeId(Integer reviewerEmployeeId) { this.reviewerEmployeeId = reviewerEmployeeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
