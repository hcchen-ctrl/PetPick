package com.petpick.petpick.entity;

import jakarta.persistence.*;

@Entity
@Table(name="userinfo")
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
}
