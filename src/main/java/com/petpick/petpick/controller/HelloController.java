package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HelloController {

    @Autowired
    private UserService userService;

    public HelloController(UserService userService) {
        this.userService = userService;
    }

    // ✅ 整合首頁和登入後首頁
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String userName = getUserName(authentication);
            model.addAttribute("userName", userName);
        }

        return "index";
    }

    // ✅ 若有人訪問 /index，導回 /
    @RequestMapping("/index")
    public String redirectToHome() {
        return "redirect:/";
    }

    // 註冊頁面
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String processRegisterForm(@ModelAttribute("registerRequest") RegisterRequest request, Model model) {
        boolean success = userService.registerNewUser(request);
        if (!success) {
            model.addAttribute("errorMessage", "註冊失敗：信箱已註冊或密碼不一致");
            return "register";
        }
        return "success";
    }

    // 登入頁面
    @GetMapping("/loginpage")
    public String loginpage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "登入失敗，請檢查帳號密碼");
        }
        return "loginpage";
    }

    // 修改個人資料
    @GetMapping("/rename")
    public String showRenameForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        UserEntity user = userService.findByAccountemail(email);
        model.addAttribute("user", user);
        return "rename";
    }

    @PostMapping("/rename")
    public String processRename(@ModelAttribute("user") UserEntity formUser,
                                Authentication authentication, Model model) {
        String email = authentication.getName();
        boolean updated = userService.updateUserByEmail(email, formUser);
        model.addAttribute("successMessage", updated ? "更新成功" : "更新失敗");
        model.addAttribute("user", userService.findByAccountemail(email));
        return "rename";
    }

    // 修改密碼
    @PostMapping("/rename/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        String resultMessage = userService.changePassword(email, currentPassword, newPassword, confirmPassword);
        redirectAttributes.addFlashAttribute("passwordMessage", resultMessage);
        return "redirect:/rename/change-password";
    }

    @GetMapping("/rename/change-password")
    public String showChangePasswordPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("user", userService.findByAccountemail(email));
        return "rename";
    }

    // 公開頁面
    @GetMapping("/gov-list-page")
    public String showGovListPage() {
        return "adopt/gov-list-page";
    }

    @GetMapping("/adopt-list")
    public String showAdoptList() {
        return "adopt/adopt-list";
    }

    @GetMapping("/shop/commodity")
    public String commodity() {
        return "shop/commodity";
    }

    // 管理者後台
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @GetMapping("/managersIndex")
    public String managersIndex() {
        return "managersIndex";
    }

    // ✅ 前端 AJAX 用來查登入狀態
    @ResponseBody
    @GetMapping("/api/auth/status")
    public Map<String, Object> getStatus(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            result.put("loggedIn", true);
            result.put("uid", authentication.getName());
            result.put("role", getUserRole(authentication));
            result.put("name", getUserName(authentication));
        } else {
            result.put("loggedIn", false);
        }

        return result;
    }

    // ✅ 前端登出 AJAX
    @ResponseBody
    @PostMapping("/api/auth/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.logout(); // Spring Security 處理登出
    }

    // Helper 方法
    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");
    }

    private String getUserName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            // 優先取用 name 屬性，若無則 fallback email
            String name = oauthUser.getAttribute("name");
            if (name == null || name.isEmpty()) {
                name = oauthUser.getAttribute("email");
            }
            return name != null ? name : "訪客";
        } else {
            // 非 OAuth2，從資料庫抓使用者真實名稱
            String email = authentication.getName();
            UserEntity user = userService.findByAccountemail(email);
            if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                return user.getUsername();
            }
            // fallback 顯示 username 或 email
            return email;
        }
    }
}
