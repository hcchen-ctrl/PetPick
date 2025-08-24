package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;


import com.petpick.petpick.entity.mission.MissionApplication;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationItemDTO {
    public Long applicationId;
    public String missionTitle;
    public Long missionId;
    public String applicantName;
    public String ownerName;
    public LocalDateTime applyTime;
    public MissionApplication.Status status;
    public String userRole;
    public String contactPhone;

}
