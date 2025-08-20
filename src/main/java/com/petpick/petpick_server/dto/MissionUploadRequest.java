package com.petpick.petpick_server.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.petpick.petpick_server.entity.Mission;
import com.petpick.petpick_server.entity.Mission.PetGender;

public class MissionUploadRequest {
    public Long posterId;
    public String title;
    public String description;
    public String city;
    public String district;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public Integer price;

    public String petName;
    public String petAge;
    public Mission.PetGender petGender;
    public String contactPhone;

    public List<Long> tags;

    
    public record MissionUpdateRequest(
            String title, String description, String city, String district,
            LocalDateTime startTime, LocalDateTime endTime, Integer price,
            String petName, String petAge, PetGender petGender,
            String contactPhone, List<Long> tagIds) {
    }

}
