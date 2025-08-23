package com.petpick.petpick.service;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private UserEntity user;  // 你的資料庫使用者物件
    private Map<String, Object> attributes;

    public CustomOAuth2User(UserEntity user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public Long getUserid() {
        return user.getUserid();
    }

    public String getRole() {
        return user.getRole();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getName() {
        return user.getAccountemail();
    }
}
