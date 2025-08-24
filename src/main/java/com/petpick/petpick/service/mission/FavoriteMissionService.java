package com.petpick.petpick.service.mission;

import java.util.List;

import com.petpick.petpick.entity.mission.FavoriteMission;
import com.petpick.petpick.entity.mission.Mission;
import com.petpick.petpick.entity.mission.UserInfo;
import com.petpick.petpick.repository.mission.FavoriteMissionRepository;
import com.petpick.petpick.repository.mission.MissionRepository;
import com.petpick.petpick.repository.mission.UserinfoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteMissionService {
    private final FavoriteMissionRepository favoriteRepo;
    private final UserinfoRepository userRepo;
    private final MissionRepository missionRepo;

    public FavoriteMissionService(FavoriteMissionRepository favoriteRepo,
            UserinfoRepository userRepo,
            MissionRepository missionRepo) {
        this.favoriteRepo = favoriteRepo;
        this.userRepo = userRepo;
        this.missionRepo = missionRepo;
    }

    @Transactional
    public FavoriteMission addFavorite(Long userId, Long missionId) {
        if (favoriteRepo.existsByUser_UserIdAndMission_MissionId(userId, missionId)) {
            return favoriteRepo.findByUser_UserIdAndMission_MissionId(userId, missionId).get();
        }

        UserInfo user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Mission mission = missionRepo.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        FavoriteMission fav = new FavoriteMission();
        fav.setUser(user);
        fav.setMission(mission);

        try {
            return favoriteRepo.save(fav);
        } catch (DataIntegrityViolationException e) {
            return favoriteRepo.findByUser_UserIdAndMission_MissionId(userId, missionId)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public void removeFavorite(Long userId, Long missionId) {
        favoriteRepo.deleteByUser_UserIdAndMission_MissionId(userId, missionId);
    }

    public boolean isFavorited(Long userId, Long missionId) {
        return favoriteRepo.existsByUser_UserIdAndMission_MissionId(userId, missionId);
    }

    public List<FavoriteMission> listFavorites(Long userId) {
        return favoriteRepo.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    public long countFavoritesOfMission(Long missionId) {
        return favoriteRepo.countByMission_MissionId(missionId);
    }
}