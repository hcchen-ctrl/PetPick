package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import com.petpick.petpick.service.userService1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HelloController {
    @Autowired
    private UserService userService;

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



    @RequestMapping("/")
    public String welcome() {
        return "welcome";
    }

    @RequestMapping("/loginpage")
    public String loginpage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "登入失敗，請檢查帳號密碼");
        }
        return "loginpage";
    }

    @RequestMapping("/fail")
    @ResponseBody
    public String fail() {
        return "fail";
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





    @RequestMapping("/adminpage")
    @ResponseBody
    public String adminpage() {
        return "adminpage";
    }

    @RequestMapping("/managerpage")
    @ResponseBody
    public String managerpage() {
        return "managerpage";
    }

    @RequestMapping("/employeepage")
    @ResponseBody
    public String employeepage() {
        return "employeepage";
    }

}
