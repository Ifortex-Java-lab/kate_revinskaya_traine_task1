package org.example.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stripeSubscriptionId;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Subscription() {
    }

    public Subscription(String stripeSubscriptionId, String status, Customer customer) {
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.status = status;
        this.customer = customer;
    }
}
