package com.petpick.petpick.service;

import com.petpick.petpick.repository.userRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class authService {
    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public authService(userRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean login(Long id,String username, String password) {
        return userRepository.findById(id)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

}
