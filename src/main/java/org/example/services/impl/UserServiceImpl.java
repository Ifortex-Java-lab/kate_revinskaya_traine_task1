package org.example.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.data.User;
import org.example.exceptions.UserNotFoundException;
import org.example.repository.UserRepository;
import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public boolean registerNewUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            return false;
        }
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("ROLE_USER");
        userRepository.save(newUser);
        logger.info("user with email {} was saved in db", email);
        return true;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user " + email + "not found in database"));
    }

    @Override
    public boolean hasStripeCustomer(String email) {
        User currentUser = findUserByEmail(email);
        return currentUser.getStripeCustomerId() != null;
    }
}