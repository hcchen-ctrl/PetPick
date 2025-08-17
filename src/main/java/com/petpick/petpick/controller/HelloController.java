package com.petpick.petpick.controller;

import com.petpick.petpick.DTO.RegisterRequest;
import com.petpick.petpick.entity.UserEntity;
import com.petpick.petpick.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        return "redirect:/loginpage?registerSuccess";
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
