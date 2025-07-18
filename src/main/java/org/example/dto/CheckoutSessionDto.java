package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutSessionDto {
    @JsonProperty("subscription")
    private String subscriptionId;

    @JsonProperty("customer")
    private String customerId;

    @JsonProperty("customer_details")
    private CustomerDetailsDto customerDetails;

    @JsonProperty("id")
    private String sessionId;
}