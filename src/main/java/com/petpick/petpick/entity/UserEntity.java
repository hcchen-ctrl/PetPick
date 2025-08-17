package com.petpick.petpick.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "userinfo") // 對應 MySQL 的 users 表
@Data
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userid;

    private String accountemail;

    private String username;

    private String password;

    private String role;

    public UserEntity(String accountemail, String password) {
        this.username = accountemail;
        this.password = password;
    }

}
