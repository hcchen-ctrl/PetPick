package com.petpick.petpick.entity;

import jakarta.persistence.*;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "userinfo")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userid;

    private String phonenumber;
    private byte[] icon;
    private String gender;
    private String city;
    private String district;
    private String experience;
    private String daily;
    private String activities;
    private String pet;
    private String pet_activities;
    private String isaccount;
    private String isblacklist;

    private String accountemail;
    private String username;
    private String password;
    private String role;


    // 自定義建構子 (你可以保留這個)
    public UserEntity(String accountemail, String password) {
        this.username = accountemail;
        this.password = password;
    }

    @Transient
    private List<String> petList;

    @Transient
    private List<String> petActivitiesList;

    public UserEntity() {
        // 空的無參數建構子
    }


    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getDaily() {
        return daily;
    }

    public void setDaily(String daily) {
        this.daily = daily;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }

    public String getPet() {
        return pet;
    }

    public void setPet(String pet) {
        this.pet = pet;
    }

    public String getPet_activities() {
        return pet_activities;
    }

    public void setPet_activities(String pet_activities) {
        this.pet_activities = pet_activities;
    }


    public List<String> getPetList() {
        if (petList == null && pet != null && !pet.isEmpty()) {
            petList = Arrays.asList(pet.split(","));
        }
        return petList;
    }

    public void setPetList(List<String> petList) {
        this.petList = petList;
        if (petList != null && !petList.isEmpty()) {
            this.pet = String.join(",", petList);
        } else {
            this.pet = null;
        }
    }

    // 新增 getter/setter for petActivitiesList (List<String>)

    public List<String> getPetActivitiesList() {
        if (petActivitiesList == null && pet_activities != null && !pet_activities.isEmpty()) {
            petActivitiesList = Arrays.asList(pet_activities.split(","));
        }
        return petActivitiesList;
    }

    public void setPetActivitiesList(List<String> petActivitiesList) {
        this.petActivitiesList = petActivitiesList;
        if (petActivitiesList != null && !petActivitiesList.isEmpty()) {
            this.pet_activities = String.join(",", petActivitiesList);
        } else {
            this.pet_activities = null;
        }
    }



    public String getIsaccount() {
        return isaccount;
    }

    public void setIsaccount(String isaccount) {
        this.isaccount = isaccount;
    }

    public String getIsblacklist() {
        return isblacklist;
    }

    public void setIsblacklist(String isblacklist) {
        this.isblacklist = isblacklist;
    }

    public String getAccountemail() {
        return accountemail;
    }

    public void setAccountemail(String accountemail) {
        this.accountemail = accountemail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

