package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.LoginRequest;
import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.JWT.JwtUtil;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class HelloController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public HelloController(UserService userService,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/auth/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName(); // 取得登入帳號（email）

            // 從資料庫撈出 UserEntity 物件
            UserEntity user = userService.findByAccountemail(email);

            if (user != null) {
                result.put("userId", user.getUserid());  // 使用物件取得 userid
                result.put("username", getUserName(authentication));
                result.put("token", ""); // JWT token 前端自行存
            } else {
                result.put("error", "找不到使用者");
            }
        } else {
            result.put("error", "未登入");
        }

        return result;
    }



    // 註冊新使用者
    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        boolean success = userService.registerNewUser(request);
        if (success) {
            response.put("success", true);
            response.put("message", "註冊成功");
        } else {
            response.put("success", false);
            response.put("message", "註冊失敗：信箱已註冊或密碼不一致");
        }
        return response;
    }

    // 修改個人資料
    @PutMapping("/user/rename")
    public Map<String, Object> rename(@RequestBody UserEntity formUser, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String email = authentication.getName();
        boolean updated = userService.updateUserByEmail(email, formUser);
        response.put("success", updated);
        response.put("message", updated ? "更新成功" : "更新失敗");
        if (updated) {
            response.put("user", userService.findByAccountemail(email));
        }
        return response;
    }

    // 修改密碼
    @PostMapping("/user/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> passwordPayload,
                                              Authentication authentication) {
        String currentPassword = passwordPayload.get("currentPassword");
        String newPassword = passwordPayload.get("newPassword");
        String confirmPassword = passwordPayload.get("confirmPassword");

        Map<String, Object> response = new HashMap<>();
        String email = authentication.getName();
        String resultMessage = userService.changePassword(email, currentPassword, newPassword, confirmPassword);

        boolean success = resultMessage.contains("成功");
        response.put("success", success);
        response.put("message", resultMessage);
        return response;
    }

    // Helper - 取得角色
    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");
    }

    // Helper - 取得名稱
    private String getUserName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            String name = oauthUser.getAttribute("name");
            if (name == null || name.isEmpty()) {
                name = oauthUser.getAttribute("email");
            }
            return name != null ? name : "訪客";
        } else {
            String email = authentication.getName();
            UserEntity user = userService.findByAccountemail(email);
            return (user != null && user.getUsername() != null && !user.getUsername().isEmpty())
                    ? user.getUsername()
                    : email;
        }
    }

    // 登入 API，路徑改成 /auth/login，回傳 JWT token
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getAccountemail(), loginRequest.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername());

            response.put("success", true);
            response.put("token", token);
            response.put("message", "登入成功");
        } catch (BadCredentialsException ex) {
            response.put("success", false);
            response.put("message", "帳號或密碼錯誤");
        } catch (Exception ex) {
            ex.printStackTrace();  // 印出完整錯誤堆疊，方便排查
            response.put("success", false);
            response.put("message", "登入失敗：" + ex.getMessage());
        }
        return response;
    }

}
