package org.example.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import org.example.data.User;
import org.example.exceptions.StripeCustomerIdMissingException;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CreateStripeSessionsService {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @Value("${payment.url.success}")
    private String successUrl;
    @Value("${payment.url.cancel}")
    private String cancelUrl;

    @Value("${subscription.manage.url.return}")
    private String returnUrl;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    public com.stripe.model.checkout.Session createCheckoutStripeSession(String userEmail, String priceId)
            throws StripeException {
        Stripe.apiKey = secretKey;
        User currentUser = userService.findUserByEmail(userEmail);
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

        return com.stripe.model.checkout.Session.create(params);
    }

    public com.stripe.model.billingportal.Session createCustomerPortalSession(User currentUser)
            throws StripeException {
        String stripeCustomerId = currentUser.getStripeCustomerId();
        if (stripeCustomerId == null) {
            throw new StripeCustomerIdMissingException("Cannot create customer portal for a user without stripeCustomerId.");
        }

        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setReturnUrl(returnUrl)
                .build();
        return com.stripe.model.billingportal.Session.create(params);
    }

    private String getOrCreateStripeCustomer(User currentUser) throws StripeException {
        if (currentUser.getStripeCustomerId() != null) {
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

        return stripeCustomerId;
    }
}
