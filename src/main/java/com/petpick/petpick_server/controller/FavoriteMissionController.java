package com.petpick.petpick_server.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick_server.entity.FavoriteMission;
import com.petpick.petpick_server.service.FavoriteMissionService;

@RestController
@RequestMapping("/favorites")
public class FavoriteMissionController {
    private final FavoriteMissionService favoriteService;

    public FavoriteMissionController(FavoriteMissionService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestParam Long userId, @RequestParam Long missionId) {
        FavoriteMission saved = favoriteService.addFavorite(userId, missionId);
        return ResponseEntity.ok(Map.of(
            "favoriteId", saved.getFavoriteId(),
            "userId", saved.getUser().getUserId(),
            "missionId", saved.getMission().getMissionId()
        ));
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@RequestParam Long userId, @RequestParam Long missionId) {
        favoriteService.removeFavorite(userId, missionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam Long userId, @RequestParam Long missionId) {
        boolean ok = favoriteService.isFavorited(userId, missionId);
        return Map.of("favorited", ok);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam Long userId) {
        List<Long> missionIds = favoriteService.listFavorites(userId).stream()
                .map(f -> f.getMission().getMissionId())
                .collect(Collectors.toList());
        return Map.of("userId", userId, "missions", missionIds);
    }

    @GetMapping("/count")
    public Map<String, Object> count(@RequestParam Long missionId) {
        long cnt = favoriteService.countFavoritesOfMission(missionId);
        return Map.of("missionId", missionId, "favoriteCount", cnt);
    }
}
