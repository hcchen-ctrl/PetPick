package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import com.petpick.petpick.service.userService1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HelloController {
    @Autowired
    private UserService userService;

    //放行首頁
    @RequestMapping(value = "/index", method = RequestMethod.GET)
        public String index() {
            return "index"; // 對應 src/main/resources/templates/index.html
        }


//註冊頁面
    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String processRegisterForm(@ModelAttribute("registerRequest") RegisterRequest request, Model model) {
        boolean success = userService.registerNewUser(request);
        if (!success) {
            model.addAttribute("errorMessage", "註冊失敗：信箱已註冊或密碼不一致");
            return "register";
        }
        return "success";
    }


//新增google地方登入API
    @RequestMapping("/")
    public String welcome(Model model, Authentication authentication) {
        String userIdentifier;

        if (authentication instanceof OAuth2AuthenticationToken) {
            // 是 Google 登入
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauth2Token.getPrincipal();
            userIdentifier = oauthUser.getAttribute("email"); // Google 登入者 email
        } else {
            // 是一般表單登入
            userIdentifier = authentication.getName(); // 你的 UserDetails.username
        }

        model.addAttribute("userName", userIdentifier);
        return "welcome";
    }

//會員登入頁面
    @RequestMapping(value = "/loginpage", method = RequestMethod.GET)
    public String loginpage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "登入失敗，請檢查帳號密碼");
        }
        return "loginpage";
    }

    //更新個人資料頁面
    @RequestMapping(value = "/rename", method = RequestMethod.GET)
    public String showRenameForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        UserEntity user = userService.findByAccountemail(email);
        model.addAttribute("user", user);
        return "rename";
    }

    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    public String processRename(@ModelAttribute("user") UserEntity formUser,
                                Authentication authentication, Model model) {
        String email = authentication.getName();
        boolean updated = userService.updateUserByEmail(email, formUser);
        model.addAttribute("successMessage", updated ? "更新成功" : "更新失敗");
        model.addAttribute("user", userService.findByAccountemail(email));
        return "rename";
    }

    //修改密碼
    // 修改密碼請求 (POST)
    public HelloController(UserService userService) {
        this.userService = userService;
    }
    @RequestMapping(value = "/rename/change-password", method = RequestMethod.POST)
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        String resultMessage = userService.changePassword(email, currentPassword, newPassword, confirmPassword);

        redirectAttributes.addFlashAttribute("passwordMessage", resultMessage); // ✅ 關鍵
        return "redirect:/rename/change-password";
    }


    @RequestMapping(value = "/rename/change-password", method = RequestMethod.GET)
    public String showChangePasswordPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("user", userService.findByAccountemail(email));
        return "rename"; // ✅ Thymeleaf 模板名稱，沒有加斜線
    }



    //回傳公認領養的html
    @RequestMapping("/gov-list-page")
    public String showGovListPage() {
        return "adopt/gov-list-page";
    }

    //回傳商城首頁
    @RequestMapping(value = "/shop/commodity", method = RequestMethod.GET)
    public String commodity() {
        return "shop/commodity";
    }



    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @RequestMapping("/managersIndex")
    public String managersIndex() {
        return "managersIndex";
    }



}
