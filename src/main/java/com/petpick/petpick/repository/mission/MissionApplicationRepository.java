package com.petpick.petpick.repository.mission;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petpick.petpick.entity.mission.MissionApplication;


@Repository
public interface MissionApplicationRepository extends JpaRepository<MissionApplication, Long> {

    @EntityGraph(attributePaths = { "mission", "mission.poster", "applicant", "owner" })
    List<MissionApplication> findByApplicant_UserIdOrderByApplyTimeDesc(Long userId);

    @EntityGraph(attributePaths = { "mission", "mission.poster", "applicant", "owner" })
    List<MissionApplication> findByOwner_UserIdOrderByApplyTimeDesc(Long userId);

    boolean existsByMission_MissionIdAndApplicant_UserId(Long missionId, Long applicantId);

    long countByMission_MissionId(Long missionId);

    long countByMission_MissionIdAndStatus(Long missionId, MissionApplication.Status status);

    boolean existsByMission_MissionIdAndStatus(Long missionId, MissionApplication.Status status);

    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MissionApplication m SET m.status = :status WHERE m.applicationId = :appId AND m.owner.userId = :ownerId")
    int updateStatusByOwner(
            @Param("appId") Long appId,
            @Param("ownerId") Long ownerId,
            @Param("status") MissionApplication.Status status);


    int deleteByApplicationIdAndApplicant_UserId(Long applicationId, Long userId);
}
