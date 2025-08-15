package com.petpick.petpick.service;


import com.petpick.petpick.entity.userEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.petpick.petpick.repository.userRepository;

@Service
public class userServiceImpl implements userService, UserDetailsService {

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 建構子注入，去除無參建構子
    public userServiceImpl(userRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        userEntity user = userRepository.findByAccountemail(email)
                .orElseThrow(() -> new UsernameNotFoundException("帳號不存在或未驗證"));

        return new org.springframework.security.core.userdetails.User(
                user.getAccountemail(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList(user.getAuthority())
        );
    }
}



//
//    @Override
//    public boolean login(String accountemail, String password) {
//        return loginByEmail(accountemail, password);
//    }
//
//    @Override
//    public userEntity findById(Long user_id) {
//        return userRepository.findById(user_id)
//                .orElseThrow(() -> new RuntimeException("找不到會員"));
//    }
//
//    @Override
//    public void save(userEntity user) {
//        userRepository.save(user);
//    }
//
//    @Override
//    public void update(userEntity user) {
//        userRepository.save(user);
//    }
//
//    @Override
//    public userEntity findByAccountemail(String accountemail) {
//        return userRepository.findByAccountemail(accountemail);
//    }

