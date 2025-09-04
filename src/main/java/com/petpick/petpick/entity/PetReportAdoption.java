package com.petpick.petpick.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "petreport_adoptions")
public class PetReportAdoption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "pet_name", nullable = false, length = 100)
    private String petName;

    @Column(name = "adoption_date")
    private java.time.LocalDate adoptionDate;

    // 這兩個一定要加 @Builder.Default，否則用 builder 會是 null
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "active";

    @Builder.Default
    @Column(name = "required_reports", nullable = false)
    private Integer requiredReports = 12;

    @Column(name = "post_id_ext")
    private Long postIdExt;

    @Column(name = "adopter_user_id_ext")
    private Long adopterUserIdExt;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
