package com.petpick.petpick.entity.mission;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="mission_applications")
public class MissionApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="mission_id", nullable=false)
    private Mission mission;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="applicant_id", nullable=false)
    private UserInfo applicant;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="owner_id", nullable=false)
    private UserInfo owner;

    private LocalDateTime applyTime;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING','ACCEPTED','REJECTED')", nullable = false)
    private Status status;

    @Column(nullable = false)
    private Integer score = 0;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
