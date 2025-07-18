package org.example.controllers;

import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Session;
import org.example.data.User;
import org.example.services.CreateStripeSessionsService;
import org.example.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SubscriptionController {

    @Autowired
    private UserService userService;
    @Autowired
    private CreateStripeSessionsService createStripeSessionsService;

    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(
            @RequestBody Map<String, String> payload,
            Authentication authentication
    ) throws StripeException {
        String priceId = payload.get("priceId");
        String userEmail = authentication.getName();

        com.stripe.model.checkout.Session session = createStripeSessionsService
                .createCheckoutStripeSession(userEmail, priceId);

        Map<String, String> response = new HashMap<>();
        response.put("url", session.getUrl());
        return response;
    }

    @PostMapping("/create-customer-portal-session")
    public Map<String, String> createCustomerPortalSession(Authentication authentication) throws StripeException {
        User currentUser = userService.findUserByEmail(authentication.getName());

        Session portalSession = createStripeSessionsService.createCustomerPortalSession(currentUser);

        Map<String, String> response = new HashMap<>();
        response.put("url", portalSession.getUrl());
        return response;
    }
}