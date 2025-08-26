package com.petpick.petpick.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.AdoptApplication;
import com.petpick.petpick.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;



public interface AdoptApplicationRepository extends JpaRepository<AdoptApplication, Long> {

    Optional<AdoptApplication> findTopByPostIdAndApplicantUserIdOrderByIdDesc(Long postId, Long applicantUserId);

    boolean existsByPostIdAndApplicantUserIdAndStatusIn(
            Long postId, Long applicantUserId, Collection<ApplicationStatus> statuses);

    Page<AdoptApplication> findByApplicantUserId(Long uid, Pageable pageable);
    Page<AdoptApplication> findByApplicantUserIdAndStatus(Long uid, ApplicationStatus status, Pageable pageable);

    Page<AdoptApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    // ======= Admin 查詢（原本就有）=======
    @Query("""
      select a
      from AdoptApplication a
      join AdoptPost p on p.id = a.postId
      where (:status is null or a.status = :status)
        and (:species is null or :species = '' or p.species = :species)
        and (:q is null or :q = '' or
             lower(p.title) like lower(concat('%', :q, '%')) or
             lower(p.breed) like lower(concat('%', :q, '%')) or
             lower(p.contactName) like lower(concat('%', :q, '%')) or
             lower(p.contactPhone) like lower(concat('%', :q, '%')))
      """)
    Page<AdoptApplication> adminSearch(
            @Param("status") ApplicationStatus status,
            @Param("species") String species,
            @Param("q") String q,
            Pageable pageable
    );
    // ======= 新增：計算每個貼文的「pending 申請數量」=======
    @Query("SELECT a.postId, COUNT(a) FROM AdoptApplication a WHERE a.postId IN :postIds AND a.status = :status GROUP BY a.postId")
    List<Object[]> countPendingByPostIds(@Param("postIds") List<Long> postIds, @Param("status") ApplicationStatus status);


    // ======= 新增：找出當前會員已申請過（pending/approved）的貼文 id 清單=======
    @Query("""
      select distinct a.postId
      from AdoptApplication a
      where a.applicantUserId = :uid
        and a.status in :statuses
        and a.postId in :postIds
      """)
    List<Long> findAppliedPostIds(@Param("uid") Long uid,
                                  @Param("statuses") Collection<ApplicationStatus> statuses,
                                  @Param("postIds") Collection<Long> postIds);

    // 貼文是否已經有一筆核准的申請
    boolean existsByPostIdAndStatus(Long postId, ApplicationStatus status);

    // 核准其中一人後，把同貼文其它 pending 全部退回
    @Modifying
    @Transactional
    @Query("""
    UPDATE AdoptApplication a 
    SET a.status = :rejectedStatus,
    a.rejectReason = :comment,
            a.updatedAt = CURRENT_TIMESTAMP
    WHERE a.postId = :postId 
      AND a.id <> :acceptedId 
      AND a.status = :pendingStatus
""")
    int rejectOthersPendingOfPost(
            @Param("postId") Long postId,
            @Param("acceptedId") Long acceptedId,
            @Param("pendingStatus") ApplicationStatus pendingStatus,
            @Param("rejectedStatus") ApplicationStatus rejectedStatus,
            @Param("comment") String comment
    );

}
