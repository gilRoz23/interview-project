package com.interview.interview_project.service;

import com.interview.interview_project.model.ClickEvent;
import com.interview.interview_project.model.Link;
import com.interview.interview_project.repository.ClickEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Handles asynchronous click processing: records the click,
 * runs fraud validation, and awards credit if valid.
 */
@Service
public class ClickTrackingService {

    private static final BigDecimal CREDIT_AMOUNT = new BigDecimal("0.05");

    private final ClickEventRepository clickEventRepository;
    private final FraudValidationService fraudValidationService;

    public ClickTrackingService(ClickEventRepository clickEventRepository,
                                FraudValidationService fraudValidationService) {
        this.clickEventRepository = clickEventRepository;
        this.fraudValidationService = fraudValidationService;
    }

    @Async
    public void processClick(Link link) {
        ClickEvent click = new ClickEvent(link);

        boolean valid = fraudValidationService.validate();

        click.setFraudValid(valid);
        click.setCreditAwarded(valid ? CREDIT_AMOUNT : BigDecimal.ZERO);

        clickEventRepository.save(click);
    }
}
