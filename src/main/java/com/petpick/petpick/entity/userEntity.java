package com.petpick.petpick.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="userinfo")
@Data
public class userEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long user_id;

    private String phonenumber;
    private String username;
    private String password;
    private String gender;
    private String accountemail;
    private String city;
    private String district;
    private String experience;
    private String daily;
    private String activities;
    private String pet;
    private String pet_activities;
    private String isaccount;
    private String isblacklist;

    // 新增email驗證相關欄位
    @Column(name = "email_verification_code")
    private String emailVerificationCode;

    @Column(name = "email_verified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailVerified = false;





}
