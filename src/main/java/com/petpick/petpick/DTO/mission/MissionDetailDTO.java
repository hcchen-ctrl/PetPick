package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


import com.petpick.petpick.entity.mission.Mission;
import com.petpick.petpick.entity.mission.MissionImage;
import com.petpick.petpick.entity.mission.Tag;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MissionDetailDTO {

    private Long missionId;
    private String title;
    private String description;
    private String city;
    private String district;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer price;

    private String petName;
    private String petAge;
    private Mission.PetGender petGender;
    private String contactPhone;

    private List<String> imageUrls;
    private List<String> tags;

    private PosterDTO poster;

    public static class PosterDTO {

        public Long posterId;
        public String name;
        public String avatarUrl;
        public String email;
        public String location;
        public Integer missionCount;
        public String replyRate;
    }

    public MissionDetailDTO(Mission m) {
        this.missionId = m.getMissionId();
        this.title = m.getTitle();
        this.description = m.getDescription();
        this.city = m.getCity();
        this.district = m.getDistrict();
        this.startTime = m.getStartTime();
        this.endTime = m.getEndTime();
        this.price = m.getPrice();
        this.petName = m.getPetName();
        this.petAge = m.getPetAge();
        this.petGender = m.getPetGender();
        this.contactPhone = m.getContactPhone();
        

        this.imageUrls = m.getImages().stream().map(MissionImage::getImageUrl).distinct().toList();
        this.tags = m.getTags().stream().map(Tag::getName).toList();

        PosterDTO p = new PosterDTO();
        p.posterId = m.getPoster().getUserId();
        p.name = m.getPoster().getUsername();
        p.avatarUrl = "/api/users/avatar/" + p.posterId;
        p.email = maskEmail(m.getPoster().getAccountemail());
        p.location = safeJoin(m.getPoster().getCity(), m.getPoster().getDistrict());
        this.poster = p;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "—";
        String[] parts = email.split("@");
        String left = parts[0];
        String masked = (left.length() <= 2) ? left.charAt(0) + "*" : left.substring(0, 2) + "****";
        return masked + "@" + parts[1];
    }
    private String safeJoin(String a, String b) {
        return String.join(" ", Arrays.stream(new String[]{a, b}).filter(s -> s != null && !s.isBlank()).toList());
    }
}