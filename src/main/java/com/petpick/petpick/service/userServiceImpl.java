package com.petpick.petpick.service;


import com.petpick.petpick.entity.userEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.petpick.petpick.repository.userRepository;

@Service
public class userServiceImpl implements userService{

    private  final userRepository userRepository;
private  final PasswordEncoder passwordEncoder;

    public userServiceImpl(userRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public void register(userEntity user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("帳號已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // 密碼加密
        userRepository.save(user);
    }

    @Override
    public boolean login(String username, String password) {
        userEntity user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }

}
