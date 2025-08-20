package com.petpick.petpick_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString(exclude = "mission")
@Entity
@Table(name = "mission_images")
public class MissionImage {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long imageId;

    @Column(name = "image_url", length = 1000, nullable = false)
    private String imageUrl;

    //---------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id" , nullable = false)
    private Mission mission;


}
