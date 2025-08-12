package com.petpick.petpick.controller;

import com.petpick.petpick.entity.userEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.petpick.petpick.service.userService;

@RestController
@RequestMapping("/api/auth")
public class userController {
    @Autowired
    private final userService userService;

    public userController(userService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody userEntity user) {
        userService.register(user);
        return "註冊成功";
    }






}
