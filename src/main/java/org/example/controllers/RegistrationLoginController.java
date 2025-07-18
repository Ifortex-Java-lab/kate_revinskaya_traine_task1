package org.example.controllers;

import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationLoginController {

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(RegistrationLoginController.class);

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        logger.info("client with email {} registers", email);
        if (userService.registerNewUser(email, password)) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration was success!");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage",
                "User with email" + email + "already exists");
        return "redirect:/register";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}