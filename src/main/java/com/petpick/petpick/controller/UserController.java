package com.petpick.petpick.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.repository.UserRepository;

@RestController
@RequestMapping("/api/user") // 單數，和你 Security 的 "/api/user/**" 對齊
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    private final UserRepository userRepository;
    public UserController(UserRepository userRepository){ this.userRepository = userRepository; }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 取登入帳號（你專案登入帳號是 email）
        String email;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();         // 一般會是 email
        } else {
            email = auth.getName();
        }

        UserEntity u = userRepository.findByAccountemail(email);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 只回前端要用到的欄位
        return ResponseEntity.ok(new MeDTO(
                u.getUserid(),
                u.getUsername(),     // 顯示名稱
                u.getPhonenumber(),  // 手機
                u.getAccountemail()  // email
        ));
    }

    public record MeDTO(Long id, String username, String phonenumber, String email) {}
}
