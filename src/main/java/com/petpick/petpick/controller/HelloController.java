package com.petpick.petpick.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {
	
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
