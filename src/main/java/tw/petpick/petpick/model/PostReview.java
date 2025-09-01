package tw.petpick.petpick.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "post_reviews")
public class PostReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // adopt_posts.id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // MySQL 是 enum('approve','reject')，用字串相容
    @Column(name = "action", nullable = false, length = 10)
    private String action; // "approve" or "reject"

    @Column(name = "reason")
    private String reason;

    // ✅ 改這裡：employee → user
    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    // DB 端有 DEFAULT CURRENT_TIMESTAMP
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
