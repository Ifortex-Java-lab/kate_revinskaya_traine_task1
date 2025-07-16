package org.example.controllers;

import org.example.data.User;
import org.example.repository.UserRepository;
import org.example.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutController {

    @Autowired
    private UserService userService;

    @GetMapping("/checkout")
    public String showCheckoutForm(Model model, Authentication authentication) {
        String email = authentication.getName();
        boolean hasStripeCustomer = userService.hasStripeCustomer(email);
        model.addAttribute("hasStripeCustomer", hasStripeCustomer);
        return "checkout";
    }

    @GetMapping("/payment-success")
    public String paymentSuccessPage() {
        return "success";
    }

    @GetMapping("/payment-cancel")
    public String paymentCancelPage() {
        return "cancel";
    }
}
