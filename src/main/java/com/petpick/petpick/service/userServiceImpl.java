package com.petpick.petpick.service;


import com.petpick.petpick.entity.userEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.petpick.petpick.repository.userRepository;
import com.petpick.petpick.service.userService;

@Service
public class userServiceImpl implements userService{

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        userEntity user = userRepository.findByAccountemail(accountemail);
        if (user == null) {
            return false;
        }
        // 檢查email是否已驗證
        if (!user.isEmailVerified()) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }

    @Override
    public boolean login(String accountemail, String password) {
        return loginByEmail(accountemail, password);
    }

    @Override
    public userEntity findById(Long user_id) {
        return userRepository.findById(user_id)
                .orElseThrow(() -> new RuntimeException("找不到會員"));
    }

    @Override
    public void save(userEntity user) {
        userRepository.save(user);
    }

    @Override
    public void update(userEntity user) {
        userRepository.save(user);
    }

    @Override
    public userEntity findByAccountemail(String accountemail) {
        return userRepository.findByAccountemail(accountemail);
    }
}
