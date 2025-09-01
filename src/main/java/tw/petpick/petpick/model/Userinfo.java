package tw.petpick.petpick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "userinfo")
public class Userinfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

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

    @Column(name = "pet_activities")
    private String petActivities;

    private String isaccount;
    private String isblacklist;

    // ✅ 新增的欄位（資料表裡有的）
    private String authority;   // e.g. ROLE_USER / ROLE_ADMIN
    private String role;        // e.g. USER / ADMIN

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "email_verification_code")
    private String emailVerificationCode;
}
