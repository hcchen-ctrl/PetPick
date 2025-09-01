package tw.petpick.petpick.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import tw.petpick.petpick.model.AdoptApplication;
import tw.petpick.petpick.model.enums.ApplicationStatus;

public interface AdoptApplicationRepository extends JpaRepository<AdoptApplication, Long> {

    Optional<AdoptApplication> findTopByPostIdAndApplicantUserIdOrderByIdDesc(Long postId, Long applicantUserId);

    boolean existsByPostIdAndApplicantUserIdAndStatusIn(
            Long postId, Long applicantUserId, Collection<ApplicationStatus> statuses);

    Page<AdoptApplication> findByApplicantUserId(Long uid, Pageable pageable);
    Page<AdoptApplication> findByApplicantUserIdAndStatus(Long uid, ApplicationStatus status, Pageable pageable);

    Page<AdoptApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    // ======= Admin 查詢 =======
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
    Page<AdoptApplication> adminSearch(@Param("status") ApplicationStatus status,
                                       @Param("species") String species,
                                       @Param("q") String q,
                                       Pageable pageable);

    // 每個貼文 pending 數
    @Query("""
      select a.postId as postId, count(a) as cnt
      from AdoptApplication a
      where a.status = tw.petpick.petpick.model.enums.ApplicationStatus.pending
        and a.postId in :postIds
      group by a.postId
      """)
    List<Object[]> countPendingByPostIds(@Param("postIds") Collection<Long> postIds);

    // 當前會員已申請（pending/approved）的貼文 id
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

    // 這貼文是否已有一筆核准申請
    boolean existsByPostIdAndStatus(Long postId, ApplicationStatus status);

    // ✅ 改 reviewer 欄位為 reviewedByUserId
    @Modifying
    @Transactional
    @Query("""
      update AdoptApplication a
        set a.status = tw.petpick.petpick.model.enums.ApplicationStatus.rejected,
            a.rejectReason = :reason,
            a.reviewedByUserId = :reviewerId,
            a.rejectedAt = CURRENT_TIMESTAMP
      where a.postId = :postId
        and a.status = tw.petpick.petpick.model.enums.ApplicationStatus.pending
        and a.id <> :approvedId
    """)
    int rejectOthersPendingOfPost(@Param("postId") Long postId,
                                  @Param("approvedId") Long approvedId,
                                  @Param("reviewerId") Long reviewerId,
                                  @Param("reason") String reason);
}
