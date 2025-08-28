package tw.petpick.petpick.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "petreport_adoptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetReportAdoption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "pet_name", nullable = false)
    private String petName;

    @Column(name = "adoption_date", nullable = false)
    private LocalDate adoptionDate;

    @Column(name = "required_reports", nullable = false)
    private Integer requiredReports;

    @Column(name = "status", nullable = false)
    private String status;  // active / done

    @Column(name = "adopter_user_id_ext")
    private Long adopterUserIdExt;

    @Column(name = "post_id_ext")
    private Long postIdExt;

    @Column(name = "image_url")
    private String imageUrl;   // ✅ 新增：寵物圖片網址

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
