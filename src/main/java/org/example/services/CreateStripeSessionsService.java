package org.example.services;

import com.stripe.exception.StripeException;
import org.example.data.User;

public interface CreateStripeSessionsService {
    com.stripe.model.checkout.Session createCheckoutStripeSession(String userEmail, String priceId)
            throws StripeException;

    com.stripe.model.billingportal.Session createCustomerPortalSession(User currentUser)
            throws StripeException;
}
