package org.example.services;

import org.example.data.User;

public interface UserService {
    boolean registerNewUser(String email, String password);

    User findUserByEmail(String email);

    boolean hasStripeCustomer(String email);


}