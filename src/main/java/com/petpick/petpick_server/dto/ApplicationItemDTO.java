package com.petpick.petpick_server.dto;

import java.time.LocalDateTime;

import com.petpick.petpick_server.entity.MissionApplication;

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
