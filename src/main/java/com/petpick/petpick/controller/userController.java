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


@Controller
@RequestMapping("/auth")
public class userController {
    @Autowired
    private PasswordEncoder passwordEncoder;
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

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/userlogin")
    public String login(@RequestParam String accountemail, @RequestParam String password,
                        HttpServletResponse response, Model model) {
        userEntity user = userService.findByAccountemail(accountemail);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            String jwt = jwtUtil.generateToken(accountemail);
            Cookie cookie = new Cookie("jwt", jwt);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("message", "帳號或密碼錯誤");
            return "userlogin";
        }
    }

    @GetMapping("/index")
    public String index() {
        return "index"; // 會對應 src/main/resources/templates/index.html
    }

    @GetMapping("/profileUpdate")
    public String showProfileUpdatePage(Model model, Principal principal) {
        // principal.getName() 通常就是登入時的 accountemail
        String accountemail = principal.getName();
        userEntity user = userService.findByAccountemail(accountemail);
        model.addAttribute("user", user);
        return "profileUpdate";
    }

    @PostMapping("/profileUpdate")
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

            // 多選欄位可用逗號串接存入資料庫
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

            userService.update(user); // 這裡要呼叫 update 方法
            model.addAttribute("message", "資料更新成功");
            model.addAttribute("user", user);
            return "profileUpdate"; // 回到同一頁
        } catch (Exception e) {
            model.addAttribute("message", "資料更新失敗：" + e.getMessage());
            return "profileUpdate";
        }
    }

}
