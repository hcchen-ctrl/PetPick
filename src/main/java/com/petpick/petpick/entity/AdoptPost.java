package com.petpick.petpick.entity;

import com.petpick.petpick.model.enums.AgeLimit;
import com.petpick.petpick.model.enums.ContactMethod;
import com.petpick.petpick.model.enums.NeuterStatus;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Entity
@Table(name = "adopt_posts")
public class AdoptPost {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private String species;
  private String breed;
  private String sex;
  private String age;

  @Column(name = "body_type") private String bodyType;
  private String color;
  private String city;
  private String district;

  @Column(columnDefinition = "TEXT")
  private String description;

  // ✅ 改這裡：對應你的三個欄位
  @Column(name = "image1") private String image1;
  @Column(name = "image2") private String image2;
  @Column(name = "image3") private String image3;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private SourceType sourceType;

  @Enumerated(EnumType.STRING)
  private PostStatus status = PostStatus.pending;

  @Column(name = "posted_by_user_id")     private Long postedByUserId;
  @Column(name = "posted_by_employee_id") private Long postedByEmployeeId;

  @Column(name = "contact_name")  private String contactName;
  @Column(name = "contact_phone") private String contactPhone;
  @Column(name = "contact_line")  private String contactLine;

  @Column(name="created_at", insertable=false, updatable=false)
  private java.sql.Timestamp createdAt;

  @Column(name="updated_at", insertable=false, updatable=false)
  private java.sql.Timestamp updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "neutered", nullable = false)
  private NeuterStatus neutered = NeuterStatus.unknown;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_method", nullable = false)
  private ContactMethod contactMethod = ContactMethod.call_sms;

  @Enumerated(EnumType.STRING)
  @Column(name = "adopter_age_limit", nullable = false)
  private AgeLimit adopterAgeLimit = AgeLimit.any;

  @Column(name = "require_home_visit", nullable = false)
  private boolean requireHomeVisit = false;

  @Column(name = "require_contract", nullable = false)
  private boolean requireContract = false;

  @Column(name = "require_followup", nullable = false)
  private boolean requireFollowup = false;

  // 方便前端用的便利方法（不會映射到資料庫）
  @Transient
  public java.util.List<String> getImages() {
    return java.util.stream.Stream.of(image1, image2, image3)
        .filter(s -> s != null && !s.isBlank())
        .toList();
  }

  @Transient
  private Long pendingApplications;   // 申請中的數量（顯示 badge 用）

  @Transient
  private Boolean appliedByMe;        // 當前登入者是否已申請

  @Transient
  private Long myPendingApplicationId;   // 當前登入者在此貼文的 pending 申請 id
}