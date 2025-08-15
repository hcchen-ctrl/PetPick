package com.petpick.petpick.service;

import com.petpick.petpick.entity.userEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface userService {
//    void register(userEntity user);

    UserDetails loadUserByUsername(String email) throws UsernameNotFoundException;


//    userEntity findById(Long user_id);
//    void save(userEntity user);
//
//    void update(userEntity user);
}
