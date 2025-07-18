package org.example.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import org.example.data.User;
import org.example.exceptions.StripeCustomerIdMissingException;
import org.example.repository.UserRepository;
import org.example.services.CreateStripeSessionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreateStripeSessionsServiceImpl implements CreateStripeSessionsService {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @Value("${payment.url.success}")
    private String successUrl;
    @Value("${payment.url.cancel}")
    private String cancelUrl;

    @Value("${subscription.manage.url.return}")
    private String returnUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userServiceImpl;

    private static final Logger logger = LoggerFactory.getLogger(CreateStripeSessionsServiceImpl.class);

    @Override
    public com.stripe.model.checkout.Session createCheckoutStripeSession(String userEmail, String priceId)
            throws StripeException {
        Stripe.apiKey = secretKey;
        User currentUser = userServiceImpl.findUserByEmail(userEmail);
        String stripeCustomerId = getOrCreateStripeCustomer(currentUser);
        com.stripe.param.checkout.SessionCreateParams params =
                com.stripe.param.checkout.SessionCreateParams.builder()
                        .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .setCustomer(stripeCustomerId)
                        .addLineItem(
                                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                        .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build()
                        )
                        .build();
        logger.info("try to create stripe checkout session for user {} with price id {} and stripeCustomerId {}",
                userEmail, priceId, stripeCustomerId);
        return com.stripe.model.checkout.Session.create(params);
    }

    @Override
    public com.stripe.model.billingportal.Session createCustomerPortalSession(User currentUser)
            throws StripeException {
        String stripeCustomerId = currentUser.getStripeCustomerId();
        if (stripeCustomerId == null) {
            throw new StripeCustomerIdMissingException(
                    "Cannot create customer portal for a user without stripeCustomerId.");
        }

        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setReturnUrl(returnUrl)
                .build();
        logger.info("try to create customer portal session for user with stripeCustomerId {}", stripeCustomerId);
        return com.stripe.model.billingportal.Session.create(params);
    }

    private String getOrCreateStripeCustomer(User currentUser) throws StripeException {
        if (currentUser.getStripeCustomerId() != null) {
            logger.info("user {} who want url for checkout session already has stripeCustomerId {}",
                    currentUser.getEmail(), currentUser.getStripeCustomerId());
            return currentUser.getStripeCustomerId();
        }
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(currentUser.getEmail())
                .setName(currentUser.getEmail())
                .build();
        Customer newStripeCustomer = Customer.create(customerParams);
        String stripeCustomerId = newStripeCustomer.getId();

        currentUser.setStripeCustomerId(stripeCustomerId);
        userRepository.save(currentUser);
        logger.info("create new stripe customer with stripeCustomerId {} for user {}",
                currentUser.getStripeCustomerId(), currentUser.getEmail());
        return stripeCustomerId;
    }
}
