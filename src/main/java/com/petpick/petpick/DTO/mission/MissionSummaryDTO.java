package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionSummaryDTO {
    private Long missionId;
    private String title;
    private String city;
    private String district;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int price;
    private List<String> tags;
    private int score;
    private String imageUrl;


    

    public MissionSummaryDTO(Long missionId, String title, String city, String district,
                             LocalDateTime startTime, LocalDateTime endTime, int price,
                             List<String> tags, int score, String imageUrl) {
        this.missionId = missionId;
        this.title = title;
        this.city = city;
        this.district = district;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.tags = tags;
        this.score = score;
        this.imageUrl = imageUrl;
    }

}
