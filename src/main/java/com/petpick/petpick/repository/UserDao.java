package com.petpick.petpick.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao {
    @Autowired
    private UserRepository userRepository;

    public User findByUserAccountemail(String accountemail) {
        UserEntity userEntity = userRepository.findByAccountemail(accountemail);

        if (userEntity == null) return null;

        return new User(
                userEntity.getAccountemail(),
                userEntity.getPassword(), // 密碼應為加密格式（如 BCrypt）
                List.of(new SimpleGrantedAuthority(userEntity.getRole()))
        );
    }
}
