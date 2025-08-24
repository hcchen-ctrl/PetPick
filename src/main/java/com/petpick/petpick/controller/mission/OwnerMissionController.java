package com.petpick.petpick.controller.mission;

import com.petpick.petpick.DTO.mission.MissionOwnerItemDTO;
import com.petpick.petpick.service.mission.OwnerMissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/owners")
public class OwnerMissionController {
  private final OwnerMissionService svc;
  public OwnerMissionController(OwnerMissionService svc){ this.svc = svc; }

  @GetMapping("/{userId}/missions")
  public java.util.List<MissionOwnerItemDTO> myMissions(@PathVariable Long userId){
    return svc.listMyMissions(userId);
  }
}

