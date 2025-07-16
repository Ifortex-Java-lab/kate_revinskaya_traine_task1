package org.example.services;

import lombok.RequiredArgsConstructor;
import org.example.data.User;
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
            throw new IllegalStateException("User with this email already exists");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_USER");

        return userRepository.save(newUser);
    }

    public boolean hasStripeCustomer(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user " + email +
                        " not found in database. Data integrity issue."));
        return currentUser.getStripeCustomerId() != null;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user " + email +
                        " not found in database. Data integrity issue."));
    }
}