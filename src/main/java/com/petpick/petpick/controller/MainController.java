package com.petpick.petpick.controller;

import com.petpick.petpick.entity.userEntity;
import com.petpick.petpick.service.userService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

// 主頁面控制器（需要驗證）
@Controller
public class MainController {

    @Autowired
    private userService userService;

    // 首頁（需要登入）
    @GetMapping("/index")
    public String index() {
        return "index";
    }

    // 修改資料頁面
    @GetMapping("/auth/profile")
    public String profile(Principal principal) {
        // 如果使用者沒登入，principal 會是 null
        String username = principal.getName(); // ← NullPointerException
        return "profile";
    }

    // 更新資料
    @PostMapping("/auth/profileUpdate")
    public String updateProfile(
            @RequestParam Long user_id,
            @RequestParam String username,
            @RequestParam String phonenumber,
            @RequestParam String gender,
            @RequestParam String accountemail,
            @RequestParam String city,
            @RequestParam String district,
            @RequestParam String experience,
            @RequestParam String daily,
            @RequestParam(required = false) String[] pet,
            @RequestParam(required = false) String[] pet_activities,
            Model model
    ) {
        try {
            userEntity user = userService.findById(user_id);
            user.setUsername(username);
            user.setPhonenumber(phonenumber);
            user.setGender(gender);
            user.setAccountemail(accountemail);
            user.setCity(city);
            user.setDistrict(district);
            user.setExperience(experience);
            user.setDaily(daily);

            if (pet != null) {
                user.setPet(String.join(",", pet));
            } else {
                user.setPet(null);
            }
            if (pet_activities != null) {
                user.setPet_activities(String.join(",", pet_activities));
            } else {
                user.setPet_activities(null);
            }

            userService.update(user);
            model.addAttribute("message", "資料更新成功");
            model.addAttribute("user", user);
            return "profileUpdate";
        } catch (Exception e) {
            model.addAttribute("message", "資料更新失敗：" + e.getMessage());
            return "profileUpdate";
        }
    }

    // 登出
    @PostMapping("/auth/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/auth/userlogin";
    }
}