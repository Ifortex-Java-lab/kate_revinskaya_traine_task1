package org.example.services;

import lombok.RequiredArgsConstructor;
import org.example.data.User;
import org.example.exceptions.UserNotFoundException;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerNewUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("User with email" + email + "already exists");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_USER");

        return userRepository.save(newUser);
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user " + email + "not found in database"));
    }

    public boolean hasStripeCustomer(String email) {
        User currentUser = findUserByEmail(email);
        return currentUser.getStripeCustomerId() != null;
    }
}