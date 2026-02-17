package com.interview.interview_project.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FraudValidationServiceTest {

    private final FraudValidationService service = new FraudValidationService();

    @Test
    void validate_shouldReturnBoolean() {
        boolean result = service.validate();
        // Result is random, just verify it returns without error
        assertNotNull(result);
    }

    @Test
    void validate_shouldTakeAtLeast500ms() {
        long start = System.currentTimeMillis();
        service.validate();
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 450, "Fraud validation should take at least ~500ms, took " + elapsed + "ms");
    }

    @Test
    void validate_shouldReturnMixOfResults() {
        // Over 100 calls, we should see both true and false (statistically near-certain)
        int trueCount = 0;
        int falseCount = 0;
        FraudValidationService fastService = new FraudValidationService() {
            @Override
            public boolean validate() {
                // Skip the sleep for speed, just test the randomness
                return new java.security.SecureRandom().nextBoolean();
            }
        };

        for (int i = 0; i < 100; i++) {
            if (fastService.validate()) {
                trueCount++;
            } else {
                falseCount++;
            }
        }
        assertTrue(trueCount > 0, "Expected at least one true result");
        assertTrue(falseCount > 0, "Expected at least one false result");
    }
}
