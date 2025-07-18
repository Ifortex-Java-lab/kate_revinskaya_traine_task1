package org.example.controllers;

import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutController {

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @GetMapping("/checkout")
    public String showCheckoutForm(Model model, Authentication authentication) {
        String email = authentication.getName();
        logger.info("client with email {} asked for stripe checkout page", email);
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
