package com.interview.interview_project.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Simulates fraud validation for click events.
 * Takes 500ms to complete and returns true/false with 50% probability.
 */
@Service
public class FraudValidationService {

    private final SecureRandom random = new SecureRandom();

    public boolean validate() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return random.nextBoolean();
    }
}
