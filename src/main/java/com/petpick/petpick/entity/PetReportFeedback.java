package tw.petpick.petpick.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tw.petpick.petpick.model.ReportStatus;

@Entity
@Table(name = "petreport_feedbacks")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PetReportFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="adoption_id", nullable=false)
    @JsonIgnore 
    private PetReportAdoption adoption;

    @Column(nullable=false) private LocalDate reportDate;
    @Lob private String notes;
    private String imageUrl;

    @Column(nullable=false)
    @Builder.Default
    private ReportStatus status = ReportStatus.SUBMITTED;

    private LocalDateTime reviewedAt;

    // 與同學表整合的預留欄位
    private Long verifiedByEmployeeIdExt;

    // DB 產生欄位（在 DDL 用 generated column），避免 JPA 寫入
    @Column(insertable=false, updatable=false)
    private LocalDate reportMonth;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

