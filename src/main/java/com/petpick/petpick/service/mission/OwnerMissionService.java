package com.petpick.petpick.service.mission;

import com.petpick.petpick.DTO.mission.MissionOwnerItemDTO;
import com.petpick.petpick.repository.mission.MissionApplicationRepository;
import com.petpick.petpick.repository.mission.MissionRepository;
import org.springframework.stereotype.Service;
import com.petpick.petpick.entity.mission.MissionApplication;

import java.util.List;


@Service
public class OwnerMissionService {
  private final MissionRepository missionRepo;
  private final MissionApplicationRepository appRepo;

  public OwnerMissionService(MissionRepository missionRepo, MissionApplicationRepository appRepo) {
    this.missionRepo = missionRepo; this.appRepo = appRepo;
  }

  public List<MissionOwnerItemDTO> listMyMissions(Long posterId){
    return missionRepo.findByPoster_UserIdOrderByStartTimeDesc(posterId).stream().map(m -> {
      long total   = appRepo.countByMission_MissionId(m.getMissionId());
      long pending = appRepo.countByMission_MissionIdAndStatus(
          m.getMissionId(), MissionApplication.Status.PENDING);
      boolean hasAccepted = appRepo.existsByMission_MissionIdAndStatus(
          m.getMissionId(), MissionApplication.Status.ACCEPTED);
      String cover = m.getImages().isEmpty()? null : m.getImages().iterator().next().getImageUrl();
      List<String> tags = m.getTags().stream().map(t -> t.getName()).toList();
      return new MissionOwnerItemDTO(
        m.getMissionId(), m.getTitle(), m.getCity(), m.getDistrict(),
        m.getStartTime(), m.getEndTime(), m.getPrice(), tags, m.getScore(), cover,
        total, pending, hasAccepted
      );
    }).toList();
  }
}
