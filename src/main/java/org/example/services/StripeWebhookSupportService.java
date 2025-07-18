package org.example.services;

public interface StripeWebhookSupportService {
    String processStripeWebhook(String payload, String sigHeader);
}
