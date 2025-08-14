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

    // 依ID查詢會員
    public userEntity findById(Long user_id) {
        return userRepository.findById(user_id)
                .orElseThrow(() -> new RuntimeException("找不到會員"));
    }

    @Override
    public void save(userEntity user) {

    }



    // 更新會員資料
    @Override
    public void update(userEntity user) {
        userRepository.save(user);
    }

}
