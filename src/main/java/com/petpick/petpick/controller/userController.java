package com.petpick.petpick.controller;

import com.petpick.petpick.JwtUtil.JwtUtil;
import com.petpick.petpick.entity.userEntity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.petpick.petpick.service.userService;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class userController {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final userService userService;

    public userController(userService userService) {
        this.userService = userService;
    }

    // 登入頁面為首頁（不需驗證）
    @GetMapping("/userlogin")
    public String showLoginPage() {
        return "userlogin";
    }

    // 註冊頁面
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    // 處理註冊
    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String accountemail,
            @RequestParam String phonenumber,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(required = false) String smsCode,
            @RequestParam(required = false) String subscribeNewsletter,
            Model model
    ) {
        try {
            // 驗證密碼一致性
            if (!password.equals(confirmPassword)) {
                model.addAttribute("message", "註冊失敗：密碼不一致");
                return "register";
            }

            userEntity user = new userEntity();
            user.setUsername(username);
            user.setAccountemail(accountemail);
            user.setPhonenumber(phonenumber);
            user.setPassword(password);

            // 生成email驗證碼（簡化版，實際應該發送email）
            String verificationCode = UUID.randomUUID().toString().substring(0, 6);
            user.setEmailVerificationCode(verificationCode);
            user.setEmailVerified(false);

            userService.register(user);

            // TODO: 這裡應該發送email驗證碼給用戶
            System.out.println("Email驗證碼: " + verificationCode); // 測試用

            model.addAttribute("message", "註冊成功，請檢查您的信箱並輸入驗證碼");
            model.addAttribute("accountemail", accountemail);
            return "email-verification"; // 新增驗證頁面
        } catch (Exception e) {
            model.addAttribute("message", "註冊失敗：" + e.getMessage());
            return "register";
        }
    }

    // email驗證
    @PostMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String accountemail,
            @RequestParam String verificationCode,
            Model model
    ) {
        try {
            userEntity user = userService.findByAccountemail(accountemail);
            if (user != null && verificationCode.equals(user.getEmailVerificationCode())) {
                user.setEmailVerified(true);
                user.setEmailVerificationCode(null); // 清除驗證碼
                userService.update(user);
                model.addAttribute("message", "Email驗證成功");
                return "sucess";
            } else {
                model.addAttribute("message", "驗證碼錯誤");
                model.addAttribute("accountemail", accountemail);
                return "email-verification";
            }
        } catch (Exception e) {
            model.addAttribute("message", "驗證失敗：" + e.getMessage());
            model.addAttribute("accountemail", accountemail);
            return "email-verification";
        }
    }

    // 處理登入
    @PostMapping("/userlogin")
    public String login(@RequestParam String accountemail,
                        @RequestParam String password,
                        HttpServletResponse response,
                        Model model) {
        userEntity user = userService.findByAccountemail(accountemail);

        if (user == null) {
            model.addAttribute("message", "帳號不存在");
            return "userlogin";
        }

        if (!user.isEmailVerified()) {
            model.addAttribute("message", "請先完成Email驗證");
            return "userlogin";
        }

        if (passwordEncoder.matches(password, user.getPassword())) {
            String jwt = jwtUtil.generateToken(accountemail);
            Cookie cookie = new Cookie("jwt", jwt);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(36000); // 10小時
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("message", "密碼錯誤");
            return "userlogin";
        }
    }

    // 成功註冊頁面
    @GetMapping("/sucess")
    public String showSuccessPage() {
        return "sucess";
    }
}


