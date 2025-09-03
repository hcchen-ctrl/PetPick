package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.LoginRequest;
import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.JWT.JwtUtil;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.CustomOAuth2User;
import com.petpick.petpick.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    // 🔄 修改後的用戶資訊端點 - 支援 JWT + OAuth2 混合認證
    @GetMapping("/auth/me")
    public Map<String, Object> getCurrentUser(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {

            // 🎯 判斷認證類型並處理
            if (authentication instanceof OAuth2AuthenticationToken) {
                // OAuth2 認證（Google 登入）
                return handleOAuth2Authentication((OAuth2AuthenticationToken) authentication, request);
            } else {
                // JWT 認證（一般登入）
                return handleJwtAuthentication(authentication);
            }
        } else {
            result.put("loggedIn", false);
            result.put("authenticated", false);
            result.put("authType", "none");
            result.put("error", "未登入");
        }

        return result;
    }

    // 🔑 處理 OAuth2 認證的用戶資訊
    private Map<String, Object> handleOAuth2Authentication(OAuth2AuthenticationToken oauthToken, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            OAuth2User oauthUser = oauthToken.getPrincipal();
            HttpSession session = request.getSession(false);

            // 📋 從 OAuth2User 或 Session 中獲取資訊
            String email = null;
            Long userId = null;
            String role = "USER";

            // 🔍 優先從 CustomOAuth2User 獲取資訊
            if (oauthUser instanceof CustomOAuth2User) {
                CustomOAuth2User customUser = (CustomOAuth2User) oauthUser;
                userId = customUser.getUserid();
                role = customUser.getRole();
                email = customUser.getAttribute("email");
            } else {
                // 📧 從標準 OAuth2User 獲取 email
                email = oauthUser.getAttribute("email");
            }

            // 🏪 如果沒有 userId，嘗試從 Session 獲取
            if (userId == null && session != null) {
                userId = (Long) session.getAttribute("uid");
                String sessionRole = (String) session.getAttribute("role");
                if (sessionRole != null) {
                    role = sessionRole;
                }
            }

            // 🔄 如果還是沒有，嘗試從資料庫查找
            if (userId == null && email != null) {
                UserEntity user = userService.findByAccountemail(email);
                if (user != null) {
                    userId = user.getUserid();
                }
            }

            result.put("loggedIn", true);
            result.put("authenticated", true);
            result.put("authType", "oauth2");
            result.put("provider", "google");
            result.put("userId", userId);
            result.put("uid", userId);
            result.put("username", getOAuth2UserName(oauthUser));
            result.put("role", role);
            result.put("email", email);
            result.put("token", ""); // OAuth2 不使用 JWT token

            System.out.println("✅ OAuth2 用戶資訊: userId=" + userId + ", email=" + email + ", role=" + role);

        } catch (Exception e) {
            System.err.println("❌ OAuth2 用戶資訊處理失敗: " + e.getMessage());
            result.put("loggedIn", false);
            result.put("error", "OAuth2 用戶資訊處理失敗: " + e.getMessage());
        }

        return result;
    }

    // 🛡️ 處理 JWT 認證的用戶資訊
    private Map<String, Object> handleJwtAuthentication(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        try {
            String email = authentication.getName(); // JWT 中的用戶名（email）

            // 🔍 從資料庫查找用戶
            UserEntity user = userService.findByAccountemail(email);

            if (user != null) {
                result.put("loggedIn", true);
                result.put("authenticated", true);
                result.put("authType", "jwt");
                result.put("userId", user.getUserid());
                result.put("uid", user.getUserid());
                result.put("username", user.getUsername() != null ? user.getUsername() : email);
                result.put("role", getUserRole(authentication));
                result.put("email", email);
                result.put("token", ""); // JWT token 前端自行管理

                System.out.println("✅ JWT 用戶資訊: userId=" + user.getUserid() + ", email=" + email);
            } else {
                result.put("loggedIn", false);
                result.put("authType", "jwt");
                result.put("error", "找不到使用者");
            }
        } catch (Exception e) {
            System.err.println("❌ JWT 用戶資訊處理失敗: " + e.getMessage());
            result.put("loggedIn", false);
            result.put("error", "JWT 用戶資訊處理失敗: " + e.getMessage());
        }

        return result;
    }

    // ✅ 新增：OAuth2 登出端點
    @PostMapping("/auth/oauth2/logout")
    public Map<String, Object> oauthLogout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                System.out.println("✅ OAuth2 Session 已清除");
            }

            response.put("success", true);
            response.put("message", "OAuth2 登出成功");
        } catch (Exception e) {
            System.err.println("❌ OAuth2 登出失敗: " + e.getMessage());
            response.put("success", false);
            response.put("message", "OAuth2 登出失敗");
        }

        return response;
    }

    // ✅ 新增：生成 JWT Token 給 OAuth2 用戶（可選功能）
    @PostMapping("/auth/oauth2/generate-jwt")
    public Map<String, Object> generateJwtForOAuth2User(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication instanceof OAuth2AuthenticationToken) {
            try {
                OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
                String email = oauthUser.getAttribute("email");

                if (email != null) {
                    // 🎟️ 為 OAuth2 用戶生成 JWT Token
                    String jwtToken = jwtUtil.generateToken(email);

                    response.put("success", true);
                    response.put("token", jwtToken);
                    response.put("message", "JWT Token 生成成功");

                    System.out.println("✅ 為 OAuth2 用戶生成 JWT: " + email);
                } else {
                    response.put("success", false);
                    response.put("message", "無法獲取用戶 email");
                }
            } catch (Exception e) {
                System.err.println("❌ OAuth2 用戶 JWT 生成失敗: " + e.getMessage());
                response.put("success", false);
                response.put("message", "JWT 生成失敗");
            }
        } else {
            response.put("success", false);
            response.put("message", "非 OAuth2 認證用戶");
        }

        return response;
    }

    // ✅ 認證狀態檢查端點（重用 getCurrentUser 邏輯）
    @GetMapping("/auth/status")
    public Map<String, Object> getAuthStatus(Authentication authentication, HttpServletRequest request) {
        return getCurrentUser(authentication, request);
    }

    // 📝 註冊新使用者（保持不變）
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

    // 🔐 JWT 登入端點（保持不變）
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
            response.put("authType", "jwt");
            response.put("message", "登入成功");

            System.out.println("✅ JWT 登入成功: " + userDetails.getUsername());
        } catch (BadCredentialsException ex) {
            response.put("success", false);
            response.put("message", "帳號或密碼錯誤");
        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("success", false);
            response.put("message", "登入失敗：" + ex.getMessage());
        }
        return response;
    }

    // 🔧 修改個人資料（需要支援兩種認證方式）
    @PutMapping("/user/rename")
    public Map<String, Object> rename(@RequestBody UserEntity formUser,
                                      Authentication authentication,
                                      HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String email = null;

        // 🎯 根據認證類型獲取 email
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            email = oauthUser.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        if (email != null) {
            boolean updated = userService.updateUserByEmail(email, formUser);
            response.put("success", updated);
            response.put("message", updated ? "更新成功" : "更新失敗");
            if (updated) {
                response.put("user", userService.findByAccountemail(email));
            }
        } else {
            response.put("success", false);
            response.put("message", "無法獲取用戶信息");
        }

        return response;
    }

    // 🔒 修改密碼（需要支援兩種認證方式）
    @PostMapping("/user/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> passwordPayload,
                                              Authentication authentication,
                                              HttpServletRequest request) {
        String currentPassword = passwordPayload.get("currentPassword");
        String newPassword = passwordPayload.get("newPassword");
        String confirmPassword = passwordPayload.get("confirmPassword");

        Map<String, Object> response = new HashMap<>();

        // 🚫 OAuth2 用戶不能修改密碼
        if (authentication instanceof OAuth2AuthenticationToken) {
            response.put("success", false);
            response.put("message", "OAuth2 登入用戶無法修改密碼，請使用 Google 帳號管理");
            return response;
        }

        // 🔐 只有 JWT 用戶可以修改密碼
        String email = authentication.getName();
        String resultMessage = userService.changePassword(email, currentPassword, newPassword, confirmPassword);

        boolean success = resultMessage.contains("成功");
        response.put("success", success);
        response.put("message", resultMessage);
        return response;
    }

    // 🔧 Helper 方法：取得角色
    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .orElse("USER");
    }

    // 🔧 Helper 方法：取得 OAuth2 用戶名稱
    private String getOAuth2UserName(OAuth2User oauthUser) {
        String name = oauthUser.getAttribute("name");
        if (name == null || name.isEmpty()) {
            name = oauthUser.getAttribute("email");
        }
        return name != null ? name : "訪客";
    }

    // 🔧 Helper 方法：取得用戶名稱（支援兩種認證）
    private String getUserName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            return getOAuth2UserName(oauthUser);
        } else {
            String email = authentication.getName();
            UserEntity user = userService.findByAccountemail(email);
            return (user != null && user.getUsername() != null && !user.getUsername().isEmpty())
                    ? user.getUsername()
                    : email;
        }
    }
}