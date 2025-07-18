package org.example.controllers;

import org.example.services.StripeWebhookSupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
    @Autowired
    private StripeWebhookSupportService stripeWebhookSupportService;

    @PostMapping("/stripe-webhooks")
    public String handleStripeWebhook(
            @RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        return stripeWebhookSupportService.processStripeWebhook(payload, sigHeader);
    }
}