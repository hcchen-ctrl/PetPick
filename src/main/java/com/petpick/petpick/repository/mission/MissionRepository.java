package com.petpick.petpick.repository.mission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.mission.Mission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {
    @EntityGraph(attributePaths = { "poster", "images", "tags" })
    Optional<Mission> findWithAllByMissionId(Long missionId);

    // 發案次數
    long countByPoster_UserId(Long userId);

    @EntityGraph(attributePaths = { "images", "tags" })
    @Query("""
            SELECT m FROM Mission m
            WHERE NOT EXISTS (
                SELECT 1 FROM MissionApplication a
                WHERE a.mission = m AND a.status = 'ACCEPTED'
            )
            """)
    List<Mission> findAllWithoutMatched();

    List<Mission> findByPoster_UserIdOrderByStartTimeDesc(Long posterId);

    @Query("""
                SELECT m FROM Mission m
                WHERE (m.endTime IS NULL OR m.endTime >= :now)
                  AND (m.poster.userId <> :userId)
            """)
    List<Mission> findActiveMissions(LocalDateTime now, Long userId);

    @Modifying
    @Query("UPDATE Mission m SET m.score = 0")
    void updateAllScoresToZero();

}
