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
    private String pet_activities;
    private String isaccount;
    private String isblacklist;
}
