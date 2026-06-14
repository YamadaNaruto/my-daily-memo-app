package org.example.mydailymemoapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller

public class main {
    private final UserService userService;

    public main(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginForm() {
        System.out.println("ログイン画面表示");
        return "login";
    }
    @GetMapping("/register")
    public String registerForm(){
        return "register";
    }
    @PostMapping("/register")
    public String register(@ModelAttribute User user) {
        System.out.println("register呼び出し");
        userService.register(user.getUsername(), user.getPassword());
        return "redirect:/login";
    }
    @GetMapping("/home")
    public  String home(){
        return "home";
    }

}
