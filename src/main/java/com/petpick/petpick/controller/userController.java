package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.LoginRequest;
import com.petpick.petpick.entity.userEntity;
import com.petpick.petpick.service.userServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.petpick.petpick.service.userService;


@Controller
@RequestMapping("/auth")
public class userController {
    private final userService userService; // final + Constructor Injection

    public userController(userService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(    @RequestParam String username,
                               @RequestParam String accountemail,
                               @RequestParam String phonenumber,
                               @RequestParam String password,
                               @RequestParam(required = false) String confirmPassword,
                               @RequestParam(required = false) String smsCode,
                               @RequestParam(required = false) String subscribeNewsletter,
                               Model model) {
            userEntity user = new userEntity();
            user.setUsername(username);
            user.setAccountemail(accountemail);
            user.setPhonenumber(phonenumber);
            user.setPassword(password); // 建議加密

            userService.register(user); // 寫入資料庫

            model.addAttribute("message", "註冊成功");
            return "userlogin";
        }

    @GetMapping("/register")
    public String showRegisterPage() { return "userlogin"; }

    @GetMapping("/login")
    public String showLoginPage() {
        return "userlogin"; // login.html 必須在 templates 資料夾
    }

    @PostMapping("/userlogin")
    public String login(@RequestParam String accountemail, @RequestParam String password, Model model) {
        boolean success = userService.loginByEmail(accountemail, password);
        model.addAttribute("message", success ? "登入成功" : "帳號或密碼錯誤");
        return "index"; // 登入結果頁 (需有 index.html)
    }
    }

