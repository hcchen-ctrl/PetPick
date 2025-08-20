package com.petpick.petpick_server.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick_server.dto.MissionDetailDTO;
import com.petpick.petpick_server.dto.MissionSummaryDTO;
import com.petpick.petpick_server.dto.MissionUploadRequest.MissionUpdateRequest;
import com.petpick.petpick_server.service.MissionService;

@RestController
@RequestMapping("/api/missions")
public class MissionController {
    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public List<MissionSummaryDTO> list(@RequestParam(name = "userId", required = false) Long userId) {
        if (userId != null) {
            return missionService.listForUser(userId);
        }
        return missionService.getAllMissions();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MissionDetailDTO> getMissionById(@PathVariable Long id) {
        MissionDetailDTO dto = missionService.getMissionDetail(id);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}")
    public MissionDetailDTO updateMission(@PathVariable Long id, @RequestParam Long posterId,
            @RequestBody MissionUpdateRequest req) {
        return missionService.updateMission(id, posterId, req);
    }

    @DeleteMapping("/{id}")
    public void deleteMission(@PathVariable Long id, @RequestParam Long posterId) {
        missionService.deleteMission(id, posterId);
    }

}