package com.petpick.petpick_server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick_server.dto.ApplicationItemDTO;
import com.petpick.petpick_server.entity.Mission;
import com.petpick.petpick_server.entity.MissionApplication;
import com.petpick.petpick_server.entity.MissionApplication.Status;
import com.petpick.petpick_server.entity.UserInfo;
import com.petpick.petpick_server.repository.MissionApplicationRepository;
import com.petpick.petpick_server.repository.MissionRepository;
import com.petpick.petpick_server.repository.UserinfoRepository;

@Service
public class ApplicationService {

    private final MissionApplicationRepository appRepo;
    private final MissionRepository missionRepo;
    private final UserinfoRepository userRepo;


    public ApplicationService(MissionApplicationRepository appRepo,
            MissionRepository missionRepo,
            UserinfoRepository userRepo) {
        this.appRepo = appRepo;
        this.missionRepo = missionRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<ApplicationItemDTO> listApplication(long userId) {
        return appRepo.findByApplicant_UserIdOrderByApplyTimeDesc(userId)
                .stream().map(a -> new ApplicationItemDTO(
                        a.getApplicationId(),
                        a.getMission().getTitle(),
                        a.getMission().getMissionId(),
                        a.getApplicant().getUsername(),
                        a.getOwner().getUsername(),
                        a.getApplyTime(),
                        a.getStatus(),
                        "applicant",
                        a.getOwner().getPhonenumber()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationItemDTO> listForOwner(Long userId) {
        return appRepo.findByOwner_UserIdOrderByApplyTimeDesc(userId)
                .stream().map(a -> new ApplicationItemDTO(
                        a.getApplicationId(),
                        a.getMission().getTitle(),
                        a.getMission().getMissionId(),
                        a.getApplicant().getUsername(),
                        a.getOwner().getUsername(),
                        a.getApplyTime(),
                        a.getStatus(),
                        "owner",
                        a.getApplicant().getPhonenumber()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long create(Long missionId, Long applicantId) {
        Mission m = missionRepo.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("mission not found: " + missionId));
        UserInfo applicant = userRepo.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("applicant not found: " + applicantId));
        UserInfo owner = m.getPoster();

        if (appRepo.existsByMission_MissionIdAndApplicant_UserId(missionId, applicantId)) {
            throw new IllegalStateException("already applied");
        }

        MissionApplication a = new MissionApplication();
        a.setMission(m);
        a.setApplicant(applicant);
        a.setOwner(owner);
        a.setApplyTime(LocalDateTime.now());
        a.setStatus(Status.pending);
        a.setScore(0);
        return appRepo.save(a).getApplicationId();
    }

    @Transactional
    public void updateStatusByOwner(Long applicationId, Long ownerId, Status status) {
        MissionApplication a = appRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("application not found: " + applicationId));
        if (!a.getOwner().getUserId().equals(ownerId))
            throw new SecurityException("not owner");
        if (status == Status.pending)
            throw new IllegalArgumentException("invalid status");
        a.setStatus(status);
    }

    @Transactional
    public void cancelByApplicant(Long applicationId, Long applicantId) {
        MissionApplication a = appRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("application not found: " + applicationId));
        if (!a.getApplicant().getUserId().equals(applicantId))
            throw new SecurityException("not applicant");
        appRepo.delete(a);
    }

}
