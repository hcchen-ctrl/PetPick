package com.petpick.petpick_server.service;

import org.springframework.stereotype.Service;

import com.petpick.petpick_server.dto.MissionOwnerItemDTO;
import com.petpick.petpick_server.repository.MissionApplicationRepository;
import com.petpick.petpick_server.repository.MissionRepository;

@Service
public class OwnerMissionService {
  private final MissionRepository missionRepo;
  private final MissionApplicationRepository appRepo;

  public OwnerMissionService(MissionRepository missionRepo, MissionApplicationRepository appRepo) {
    this.missionRepo = missionRepo; this.appRepo = appRepo;
  }

  public java.util.List<MissionOwnerItemDTO> listMyMissions(Long posterId){
    return missionRepo.findByPoster_UserIdOrderByStartTimeDesc(posterId).stream().map(m -> {
      long total   = appRepo.countByMission_MissionId(m.getMissionId());
      long pending = appRepo.countByMission_MissionIdAndStatus(
          m.getMissionId(), com.petpick.petpick_server.entity.MissionApplication.Status.pending);
      boolean hasAccepted = appRepo.existsByMission_MissionIdAndStatus(
          m.getMissionId(), com.petpick.petpick_server.entity.MissionApplication.Status.accepted);
      String cover = m.getImages().isEmpty()? null : m.getImages().iterator().next().getImageUrl();
      java.util.List<String> tags = m.getTags().stream().map(t -> t.getName()).toList();
      return new MissionOwnerItemDTO(
        m.getMissionId(), m.getTitle(), m.getCity(), m.getDistrict(),
        m.getStartTime(), m.getEndTime(), m.getPrice(), tags, m.getScore(), cover,
        total, pending, hasAccepted
      );
    }).toList();
  }
}
