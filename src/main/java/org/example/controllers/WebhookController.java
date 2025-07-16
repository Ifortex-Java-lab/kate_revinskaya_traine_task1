package org.example.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.example.data.Customer;
import org.example.dto.CheckoutSessionDto;
import org.example.dto.SubscriptionDto;
import org.example.repository.CustomerRepository;
import org.example.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @PostMapping("/stripe-webhooks")
    public String handleStripeWebhook(
            @RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        if (sigHeader == null) {
            logger.warn("Stripe-Signature header is null. Aborting.");
            return "";
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info(">>> SUCCESS: Signature verification successful! Event ID: {}", event.getId());
        } catch (SignatureVerificationException e) {
            logger.error(">>> ERROR: Webhook signature verification failed!", e);
            return "";
        }

        String dataObjectJson = event.getDataObjectDeserializer().getRawJson();
        logger.error(">>> Event data object raw JSON: {}", dataObjectJson);

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    logger.info("Entering case: 'checkout.session.completed'");
                    CheckoutSessionDto sessionDto = objectMapper.readValue(dataObjectJson, CheckoutSessionDto.class);
                    handleCheckoutSessionCompleted(sessionDto);
                    break;

                case "customer.subscription.deleted":
                    logger.info("Entering case: 'customer.subscription.deleted'");
                    SubscriptionDto subscription = objectMapper.readValue(dataObjectJson, SubscriptionDto.class);
                    handleSubscriptionDeleted(subscription);
                    break;

                case "customer.subscription.updated":
                    logger.info("Entering case: 'customer.subscription.updated'");
                    SubscriptionDto updatedSubscription = objectMapper.readValue(dataObjectJson, SubscriptionDto.class);
                    handleSubscriptionUpdated(updatedSubscription);
                    break;

                default:
                    logger.warn("Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            logger.error(">>> ERROR: Failed to deserialize or process event data object from JSON.", e);
            return "";
        }

        logger.info("--- [END] Webhook handling finished. ---");
        return "";
    }

    private void handleCheckoutSessionCompleted(CheckoutSessionDto sessionDto) {

        String stripeSubscriptionId = sessionDto.getSubscriptionId();
        if (stripeSubscriptionId == null) {
            logger.warn("--> [handleCheckoutSessionCompleted] Received checkout.session.completed event " +
                    "for session {} but it has no subscription ID. Aborting.", sessionDto.getSessionId());
            return;
        }

        String stripeCustomerId = sessionDto.getCustomerId();
        logger.info("--> [handleCheckoutSessionCompleted] Stripe Customer ID: {}, " +
                "Stripe Subscription ID: {}", stripeCustomerId, stripeSubscriptionId);

        Customer customer = customerRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseGet(() -> {
                    String customerEmail = sessionDto.getCustomerDetails().getEmail();
                    logger.info("--> [handleCheckoutSessionCompleted] Customer not found. " +
                            "Creating new customer with email: {}", customerEmail);
                    Customer newCustomer = new Customer(customerEmail, stripeCustomerId);
                    return customerRepository.save(newCustomer);
                });

        logger.info("--> [handleCheckoutSessionCompleted] Customer object is ready with ID: {}", customer.getId());
        org.example.data.Subscription newSubscription = new org.example.data.Subscription(
                stripeSubscriptionId,
                "active",
                customer
        );

        try {
            subscriptionRepository.save(newSubscription);
            logger.info(">>> SUCCESS: [handleCheckoutSessionCompleted] Successfully saved new subscription {} " +
                    "for customer {}", stripeSubscriptionId, stripeCustomerId);
        } catch (Exception e) {
            logger.error(">>> ERROR: [handleCheckoutSessionCompleted] Failed to save subscription to database", e);
        }
    }

    private void handleSubscriptionDeleted(SubscriptionDto subscription) {
        logger.info("--> [handleSubscriptionDeleted] Started for subscription ID: {}.", subscription.getId());
        subscriptionRepository.findByStripeSubscriptionId(subscription.getId())
                .ifPresent(sub -> {
                    sub.setStatus("canceled");
                    subscriptionRepository.save(sub);
                    logger.info("--> [handleSubscriptionDeleted] Updated subscription {} status to 'canceled'",
                            sub.getStripeSubscriptionId());
                });
    }

    private void handleSubscriptionUpdated(SubscriptionDto subscription) {
        logger.info("--> [handleSubscriptionUpdated] Started for subscription ID: {}.", subscription.getId());
        subscriptionRepository.findByStripeSubscriptionId(subscription.getId())
                .ifPresent(sub -> {
                    String newStatus = subscription.getStatus();

                    if (subscription.isCancelAtPeriodEnd()) {
                        newStatus = "pending_cancellation";
                        logger.info("--> Subscription {} is scheduled for cancellation.", subscription.getId());
                    }

                    sub.setStatus(newStatus);
                    subscriptionRepository.save(sub);
                    logger.info("--> [handleSubscriptionUpdated] Updated subscription {} status to '{}'",
                            sub.getStripeSubscriptionId(), newStatus);
                });
    }
}