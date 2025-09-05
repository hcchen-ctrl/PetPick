package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

import com.petpick.petpick.entity.UserEntity;

@Repository
public class UserDao {
    @Autowired
    private UserRepository userRepository;

    public User findByUserAccountemail(String accountemail) {
        UserEntity userEntity = userRepository.findByAccountemail(accountemail).orElse(null);

        if (userEntity == null) return null;

        return new User(
                userEntity.getAccountemail(),
                userEntity.getPassword(), // 密碼應為加密格式（如 BCrypt）
                List.of(new SimpleGrantedAuthority(userEntity.getRole()))
        );
    }
}
