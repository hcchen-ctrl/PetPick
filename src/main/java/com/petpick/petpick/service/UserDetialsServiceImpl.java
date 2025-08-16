package com.petpick.petpick.service;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.repository.UserDao;
import com.petpick.petpick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetialsServiceImpl implements UserDetailsService {


    private UserRepository userRepository;

    @Autowired
    public UserDetialsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        System.out.println("User found: " + user.getUsername() + ", password: " + user.getPassword());
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // 要有前綴 {noop}
                .roles(user.getRole()) // 不要加 "ROLE_" 前綴，Spring Security 會自動加
                .build();
    }
}
