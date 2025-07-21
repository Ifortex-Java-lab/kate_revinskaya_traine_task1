package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionDto {
    private String id;

    private String status;

    @JsonProperty("cancel_at_period_end")
    private boolean cancelAtPeriodEnd;
}