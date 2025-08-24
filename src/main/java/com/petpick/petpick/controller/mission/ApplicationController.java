package com.petpick.petpick.controller.mission;

import java.util.List;

import com.petpick.petpick.DTO.mission.ApplicationItemDTO;
import com.petpick.petpick.entity.mission.MissionApplication;
import com.petpick.petpick.service.mission.ApplicationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService svc;

    public ApplicationController(ApplicationService svc) {
        this.svc = svc;
    }

    // 列出我申請的
    @GetMapping("/me/applicant")
    public List<ApplicationItemDTO> myAsApplicant(@RequestParam Long userId) {
        return svc.listApplication(userId);
    }

    // 列出我收到的（我是發案者）
    @GetMapping("/me/owner")
    public List<ApplicationItemDTO> myAsOwner(@RequestParam Long userId) {
        return svc.listForOwner(userId);
    }

    // 建立申請
    @PostMapping
    public Long create(@RequestParam Long missionId, @RequestParam Long applicantId) {
        return svc.create(missionId, applicantId);
    }

    // 擁有者接受/拒絕
    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id,
            @RequestParam Long ownerId,
            @RequestParam MissionApplication.Status status) {
    }

    // 申請者取消
    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id, @RequestParam Long applicantId) {
        svc.cancelByApplicant(id, applicantId);
    }

}
