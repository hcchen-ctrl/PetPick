package tw.petpick.petpick.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;          // ✅ 補上
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;       // ✅ 派生命週期回呼用來補欄位
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;              // ✅ 讓預設值在 builder 生效
import tw.petpick.petpick.model.enums.ReportStatus;

@Entity
@Table(name = "petreport_feedbacks")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PetReportFeedback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adoption_id", nullable = false)
    private PetReportAdoption adoption;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "report_month", nullable = false, length = 7)
    private String reportMonth;            // ✅ NOT NULL，所以一定要幫你算出來

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    // TEXT 你可擇一：@Lob 或 columnDefinition="TEXT"。現在就用後者即可
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Default
    private ReportStatus status = ReportStatus.SUBMITTED;   // ✅ builder 也會帶到這個預設值

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "verified_by_employee_id_ext")
    private Long verifiedByEmployeeIdExt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ 自動把 reportMonth 依 reportDate 補起來（避免 NOT NULL 觸發 500）
    @PrePersist @PreUpdate
    private void syncDerivedFields() {
        if (this.reportDate != null) {
            // yyyy-MM
            this.reportMonth = this.reportDate.toString().substring(0, 7);
        }
        if (this.status == null) {
            this.status = ReportStatus.SUBMITTED;
        }
    }
}
