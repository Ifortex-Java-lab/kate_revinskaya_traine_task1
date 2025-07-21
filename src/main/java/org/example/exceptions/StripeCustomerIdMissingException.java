package org.example.exceptions;

public class StripeCustomerIdMissingException extends RuntimeException {

    public StripeCustomerIdMissingException(String message) {
        super(message);
    }
}