package com.petpick.petpick_server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petpick.petpick_server.entity.FavoriteMission;

@Repository
public interface FavoriteMissionRepository extends JpaRepository<FavoriteMission, Long> {

    boolean existsByUser_UserIdAndMission_MissionId(Long userId, Long missionId);

    Optional<FavoriteMission> findByUser_UserIdAndMission_MissionId(Long userId, Long missionId);

    void deleteByUser_UserIdAndMission_MissionId(Long userId, Long missionId);

    List<FavoriteMission> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    long countByMission_MissionId(Long missionId);
}
