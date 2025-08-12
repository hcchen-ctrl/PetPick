package com.petpick.petpick.controller;

import com.petpick.petpick.entity.userEntity;
import com.petpick.petpick.service.userServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.petpick.petpick.service.userService;


@RestController
@RequestMapping("/api/auth")
public class userController {
    private final userService userService; // final + Constructor Injection

    public userController(userService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody userEntity user) {
        userService.register(user);
        return "註冊成功";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        boolean success = userServiceImpl.login(String Username, String password);
        return success ? "登入成功" : "帳號或密碼錯誤";
    }
}
