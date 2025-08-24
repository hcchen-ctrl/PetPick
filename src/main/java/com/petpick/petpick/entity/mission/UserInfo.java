package com.petpick.petpick.entity.mission;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString(exclude = {"missions", "icon"})
@Entity
@Table(name = "userinfo")
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid")
    private Long userId;

    private String username;
    private String phonenumber;
    private String city;
    private String district;
    private String accountemail;
    private String gender;


    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column(name = "icon")
    private byte[] icon;

    @JsonIgnore
    @OneToMany(mappedBy = "poster")
    private List<Mission> missions;

    public UserInfo(Long userId, String username, String phonenumber, String city, String district, String accountemail,
            byte[] icon) {
        this.userId = userId;
        this.username = username;
        this.phonenumber = phonenumber;
        this.city = city;
        this.district = district;
        this.accountemail = accountemail;
        this.icon = icon;
    }

    public UserInfo() {
    }

}
