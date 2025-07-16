package org.example.controllers;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SubscriptionController {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    private final String hardcodedCustomerId = "cus_SgXmebQr3ny5Pk";

    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(
            @RequestBody Map<String, String> payload
    ) throws StripeException {
        String priceId = payload.get("priceId");
        Stripe.apiKey = secretKey;

        String successUrl = "http://localhost:8080/success.html";
        String cancelUrl = "http://localhost:8080/cancel.html";

        com.stripe.param.checkout.SessionCreateParams params = com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                .addPaymentMethodType(com.stripe.param.checkout.SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomer(hardcodedCustomerId)
                .addLineItem(
                        com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .build();

        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("url", session.getUrl());
        return response;
    }

    @PostMapping("/create-customer-portal-session")
    public Map<String, String> createCustomerPortalSession() throws StripeException {
        Stripe.apiKey = secretKey;

        String returnUrl = "http://localhost:8080/checkout.html";

        com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(hardcodedCustomerId)
                .setReturnUrl(returnUrl)
                .build();

        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("url", portalSession.getUrl());
        return response;
    }
}