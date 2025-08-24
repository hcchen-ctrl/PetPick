package com.petpick.petpick.repository.mission;

import java.util.List;

import com.petpick.petpick.entity.mission.MissionApplication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


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

}
