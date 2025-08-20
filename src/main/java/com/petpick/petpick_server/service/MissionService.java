package com.petpick.petpick_server.service;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick_server.dto.MissionDetailDTO;
import com.petpick.petpick_server.dto.MissionSummaryDTO;
import com.petpick.petpick_server.dto.MissionUploadRequest.MissionUpdateRequest;
import com.petpick.petpick_server.entity.Mission;
import com.petpick.petpick_server.entity.Tag;
import com.petpick.petpick_server.repository.MissionRepository;
import com.petpick.petpick_server.repository.TagRepository;
import com.petpick.petpick_server.repository.UserinfoRepository;

@Service
public class MissionService {
    private final MissionRepository missionRepo;
    private final TagRepository tagRepo;
    private final ScoringService scoringService;

    public MissionService(MissionRepository missionRepo,
            TagRepository tagRepo,
            UserinfoRepository userRepo,
            FileStorageService fs,
            ScoringService scoringService) {
        this.missionRepo = missionRepo;
        this.tagRepo = tagRepo;
        this.scoringService = scoringService;
    }

    public MissionDetailDTO getMissionDetail(Long id) {
        Mission m = missionRepo.findWithAllByMissionId(id)
                .orElseThrow(() -> new RuntimeException("Mission not found: " + id));
        MissionDetailDTO dto = new MissionDetailDTO(m);
        dto.getPoster().missionCount = (int) missionRepo.countByPoster_UserId(m.getPoster().getUserId());
        return dto;
    }

    // 主頁
    public List<MissionSummaryDTO> getAllMissions() {
        List<Mission> missions = missionRepo.findAllWithoutMatched();
        return missions.stream()
                .map(m -> new MissionSummaryDTO(
                        m.getMissionId(),
                        m.getTitle(),
                        m.getCity(),
                        m.getDistrict(),
                        m.getStartTime(),
                        m.getEndTime(),
                        m.getPrice(),
                        m.getTags().stream().map(Tag::getName).collect(Collectors.toList()),
                        m.getScore(),
                        m.getImages().stream().findFirst().map(img -> img.getImageUrl()).orElse(null)))
                .collect(Collectors.toList());
    }

    // 首頁（依使用者計算推薦分數）
    public List<MissionSummaryDTO> listForUser(Long userId) {
        //歸零
        scoringService.resetScores();
        //重算分數
        scoringService.recomputeForUser(userId);

        // 再查出清單（建議只拿未配對 / 未結束的任務；若無此 repo 方法可改用 findAllWithoutMatched）
        List<Mission> missions = missionRepo.findAllWithoutMatched();

        // 依 score 由高到低排序（null 視為 0）
        missions.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0,
                a.getScore() != null ? a.getScore() : 0));

        return missions.stream()
                .map(m -> new MissionSummaryDTO(
                        m.getMissionId(),
                        m.getTitle(),
                        m.getCity(),
                        m.getDistrict(),
                        m.getStartTime(),
                        m.getEndTime(),
                        m.getPrice(),
                        m.getTags().stream().map(Tag::getName).collect(Collectors.toList()),
                        m.getScore(),
                        m.getImages().stream().findFirst().map(img -> img.getImageUrl()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Transactional
    public MissionDetailDTO updateMission(Long id, Long posterId,
            MissionUpdateRequest r) {
        var m = missionRepo.findWithAllByMissionId(id).orElseThrow();
        if (!m.getPoster().getUserId().equals(posterId))
            throw new SecurityException("forbidden");

        if (r.title() != null)
            m.setTitle(r.title());
        if (r.description() != null)
            m.setDescription(r.description());
        if (r.city() != null)
            m.setCity(r.city());
        if (r.district() != null)
            m.setDistrict(r.district());
        if (r.startTime() != null)
            m.setStartTime(r.startTime());
        if (r.endTime() != null)
            m.setEndTime(r.endTime());
        if (r.price() != null)
            m.setPrice(r.price());
        if (r.petName() != null)
            m.setPetName(r.petName());
        if (r.petAge() != null)
            m.setPetAge(r.petAge());
        if (r.petGender() != null)
            m.setPetGender(r.petGender());
        if (r.contactPhone() != null)
            m.setContactPhone(r.contactPhone());
        if (r.tagIds() != null) {
            var tags = tagRepo.findAllById(r.tagIds());
            m.setTags(new java.util.LinkedHashSet<>(tags));
        }
        return new MissionDetailDTO(m);
    }

    @Transactional
    public void deleteMission(Long id, Long posterId) {
        var m = missionRepo.findById(id).orElseThrow();
        if (!m.getPoster().getUserId().equals(posterId))
            throw new SecurityException("forbidden");
        missionRepo.delete(m);
    }
}