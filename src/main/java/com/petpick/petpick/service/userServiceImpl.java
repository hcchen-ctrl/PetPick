package com.petpick.petpick.service;


import com.petpick.petpick.entity.userEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.petpick.petpick.repository.userRepository;
import com.petpick.petpick.service.userService;

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
        if (userRepository.existsByAccountemail(user.getAccountemail())) {
            throw new RuntimeException("電子信箱已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // 密碼加密
        userRepository.save(user);
    }

    @Override
    public boolean loginByEmail(String accountemail, String password) {
        return false;
    }

    @Override
    public boolean login(String accountemail, String password) {
        userEntity user = userRepository.findByAccountemail(accountemail);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }

}
