package org.example.services.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.example.data.User;
import org.example.dto.CheckoutSessionDto;
import org.example.dto.SubscriptionDto;
import org.example.repository.SubscriptionRepository;
import org.example.repository.UserRepository;
import org.example.services.StripeWebhookSupportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookSupportServiceImpl implements StripeWebhookSupportService {
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookSupportServiceImpl.class);

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Override
    public String processStripeWebhook(String payload, String sigHeader) {
        if (sigHeader == null) {
            logger.warn("Stripe-Signature header is null. Aborting.");
            return "";
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("signature verification was successful, event ID: {}", event.getId());
        } catch (SignatureVerificationException e) {
            logger.error(">>> ERROR: Webhook signature verification failed!", e);
            return "";
        }

        String dataObjectJson = event.getDataObjectDeserializer().getRawJson();
        logger.debug(">>> Event data object raw JSON: {}", dataObjectJson);

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    logger.info("Entering case: 'checkout.session.completed'");
                    CheckoutSessionDto sessionDto = objectMapper.readValue(dataObjectJson, CheckoutSessionDto.class);
                    handleCheckoutSessionCompleted(sessionDto);
                    break;

                case "customer.subscription.deleted":
                    logger.info("Entering case: 'customer.subscription.deleted'");
                    SubscriptionDto deleteSubscription = objectMapper.readValue(dataObjectJson, SubscriptionDto.class);
                    saveSubscriptionByNewStatus(deleteSubscription, deleteSubscription.getStatus());
                    break;

                case "customer.subscription.updated":
                    logger.info("Entering case: 'customer.subscription.updated'");
                    SubscriptionDto updatedSubscription = objectMapper.readValue(dataObjectJson, SubscriptionDto.class);
                    String newStatus = updatedSubscription.getStatus();
                    if (updatedSubscription.isCancelAtPeriodEnd()) {
                        newStatus = "pending_cancellation";
                        logger.info("--> Subscription {} is scheduled for cancellation.", updatedSubscription.getId());
                    }
                    saveSubscriptionByNewStatus(updatedSubscription, newStatus);
                    break;

                default:
                    logger.warn("Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            logger.error(">>> ERROR: Failed to deserialize or process event data object from JSON.", e);
            return "";
        }
        return "";
    }

    private void handleCheckoutSessionCompleted(CheckoutSessionDto sessionDto) {

        String stripeSubscriptionId = sessionDto.getSubscriptionId();
        if (stripeSubscriptionId == null) {
            logger.warn("received checkout.session.completed event " +
                    "for session {} but it has no subscription ID. Aborting.", sessionDto.getSessionId());
            return;
        }

        String stripeCustomerId = sessionDto.getCustomerId();
        logger.info("--> [handleCheckoutSessionCompleted] Stripe Customer ID: {}, " +
                "Stripe Subscription ID: {}", stripeCustomerId, stripeSubscriptionId);

        User customer = findOrCreateUserWithStripeCustomerIdAndEmail(
                stripeCustomerId, sessionDto.getCustomerDetails().getEmail()
        );

        logger.info("--> [handleCheckoutSessionCompleted] Customer object is ready with db ID: {}", customer.getId());
        if (subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).isPresent()) {
            logger.warn("--> [handleCheckoutSessionCompleted] Subscription with stripeSubscriptionId {} " +
                    "is already exists in db", stripeSubscriptionId);
            return;
        }
        org.example.data.Subscription newSubscription = new org.example.data.Subscription();
        newSubscription.setStatus("active");
        newSubscription.setStripeSubscriptionId(stripeSubscriptionId);
        newSubscription.setUser(customer);

        try {
            subscriptionRepository.save(newSubscription);
            logger.info("--> [handleCheckoutSessionCompleted] Successfully saved new subscription {} " +
                    "for customer {}", stripeSubscriptionId, stripeCustomerId);
        } catch (
                Exception e) {
            logger.error(">>> ERROR: [handleCheckoutSessionCompleted] Failed to save subscription to database", e);
        }
    }

    private void saveSubscriptionByNewStatus(SubscriptionDto subscription, String status) {
        subscriptionRepository.findByStripeSubscriptionId(subscription.getId())
                .ifPresent(sub -> {
                    sub.setStatus(status);
                    subscriptionRepository.save(sub);
                    logger.info("Updated subscription {} status to '{}'",
                            sub.getStripeSubscriptionId(), status);
                });
    }

    private User findOrCreateUserWithStripeCustomerIdAndEmail(String stripeCustomerId, String customerEmail) {
        return userRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseGet(() -> {
                    logger.info("--> [handleCheckoutSessionCompleted] stripe customer not found, " +
                            "creating new customer with email: {}", customerEmail);
                    User newUser = new User(customerEmail, stripeCustomerId);
                    return userRepository.save(newUser);
                });
    }
}
