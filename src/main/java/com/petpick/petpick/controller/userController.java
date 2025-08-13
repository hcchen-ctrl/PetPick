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
            userEntity user = new userEntity();
            user.setUsername(username);
            user.setAccountemail(accountemail);
            user.setPhonenumber(phonenumber);
            user.setPassword(password);

            userService.register(user);
            model.addAttribute("message", "註冊成功");
            return "sucess"; // 註冊成功顯示 sucess.html
        } catch (Exception e) {
            model.addAttribute("message", "註冊失敗：" + e.getMessage());
            return "userlogin"; // 註冊失敗回到原畫面
        }
    }

    @GetMapping("/register")
    public String showRegisterPage() { return "userlogin"; }

    @GetMapping("/userlogin")
    public String showLoginPage() {
        return "userlogin"; // login.html 必須在 templates 資料夾
    }

    @PostMapping("/userlogin")
    public String login(@RequestParam String accountemail, @RequestParam String password, Model model) {
        boolean success = userService.login(accountemail, password);
        if (success) {
            return "index";
        } else {
            model.addAttribute("message", "帳號或密碼錯誤");
            return "userlogin"; // 回到登入頁並顯示錯誤訊息
        }
    }
    }

