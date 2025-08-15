package com.petpick.petpick.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.petpick.petpick.service.userService;

@Controller
public class userController {
    @Autowired
    private userService userService;

    // 處理登入
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("message", "帳號或密碼錯誤");
        }
        if (logout != null) {
            model.addAttribute("message", "您已成功登出");
        }
        return "userlogin"; // 對應你的登入頁 userlogin.html
    }


    @GetMapping("/customer/index")
    public String index() {
        return "index"; // --> 對應 src/main/resources/templates/index.html
    }

    @RequestMapping("/error")
    public String handleError() {
        return "error"; // 對應 error.html 或 error.jsp
    }

}



