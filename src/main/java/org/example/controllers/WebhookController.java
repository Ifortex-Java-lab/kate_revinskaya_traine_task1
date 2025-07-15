package org.example.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.example.data.Customer;
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
            return "";
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Webhook signature verification failed.", e);
            return "";
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        if (dataObjectDeserializer.getObject().isEmpty()) {
            return "";
        }
        StripeObject stripeObject = dataObjectDeserializer.getObject().get();

        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) stripeObject;
                handleCheckoutSessionCompleted(session);
                break;
            case "customer.subscription.deleted":
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                handleSubscriptionDeleted(subscription);
                break;
            default:
                logger.info("Unhandled event type: {}", event.getType());
        }
        return "";
    }

    private void handleCheckoutSessionCompleted(Session session) {
        logger.info("Checkout session completed for session ID: {}", session.getId());

        String stripeSubscriptionId = session.getSubscription();
        if (stripeSubscriptionId == null) {
            logger.warn("Received checkout.session.completed event for session {} but it has no subscription ID", session.getId());
            return;
        }

        String stripeCustomerId = session.getCustomer();
        logger.info("Stripe Customer ID: {}", stripeCustomerId);
        logger.info("Stripe Subscription ID: {}", stripeSubscriptionId);

        Customer customer = customerRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseGet(() -> {
                    String customerEmail = session.getCustomerDetails().getEmail();
                    logger.info("Creating new customer with email: {}", customerEmail);
                    Customer newCustomer = new Customer(customerEmail, stripeCustomerId);
                    return customerRepository.save(newCustomer);
                });

        org.example.data.Subscription newSubscription = new org.example.data.Subscription(
                stripeSubscriptionId,
                "active",
                customer
        );

        try {
            subscriptionRepository.save(newSubscription);
            logger.info("Successfully saved new subscription {} for customer {}", stripeSubscriptionId, stripeCustomerId);
        } catch (Exception e) {
            logger.error("Failed to save subscription to database", e);
        }
    }

    private void handleSubscriptionDeleted(com.stripe.model.Subscription subscription) {
        logger.info("Subscription {} deleted.", subscription.getId());
        subscriptionRepository.findByStripeSubscriptionId(subscription.getId())
                .ifPresent(sub -> {
                    sub.setStatus("canceled");
                    subscriptionRepository.save(sub);
                    logger.info("Updated subscription {} status to 'canceled'", sub.getStripeSubscriptionId());
                });
    }
}