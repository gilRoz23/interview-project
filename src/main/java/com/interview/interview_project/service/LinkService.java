package com.interview.interview_project.service;

import com.interview.interview_project.dto.LinkStatsResponse;
import com.interview.interview_project.dto.MonthlyBreakdown;
import com.interview.interview_project.exception.LinkNotFoundException;
import com.interview.interview_project.model.ClickEvent;
import com.interview.interview_project.model.Link;
import com.interview.interview_project.repository.ClickEventRepository;
import com.interview.interview_project.repository.LinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkService {

    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_RETRIES = 10;

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;
    private final SecureRandom random = new SecureRandom();

    public LinkService(LinkRepository linkRepository, ClickEventRepository clickEventRepository) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Creates a short link for the given target URL.
     * If the target URL already has a short link, returns the existing one (idempotent).
     */
    @Transactional
    public Link createShortLink(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl is required and cannot be empty");
        }

        Optional<Link> existing = linkRepository.findByTargetUrl(targetUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        String shortCode = generateUniqueShortCode();
        Link link = new Link(shortCode, targetUrl);
        return linkRepository.save(link);
    }

    /**
     * Looks up a link by its short code.
     */
    public Link getByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));
    }

    /**
     * Returns paginated link stats with monthly breakdowns.
     */
    public Page<LinkStatsResponse> getStats(Pageable pageable) {
        Page<Link> links = linkRepository.findAll(pageable);
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM/yyyy");

        return links.map(link -> {
            long totalClicks = clickEventRepository.countByLinkId(link.getId());
            BigDecimal totalEarnings = clickEventRepository.sumCreditsByLinkId(link.getId());

            List<ClickEvent> clicks = clickEventRepository.findByLinkId(link.getId());
            Map<YearMonth, BigDecimal> monthlyMap = new TreeMap<>();
            for (ClickEvent click : clicks) {
                YearMonth ym = YearMonth.from(click.getClickedAt());
                monthlyMap.merge(ym, click.getCreditAwarded(), BigDecimal::add);
            }

            List<MonthlyBreakdown> monthlyBreakdown = monthlyMap.entrySet().stream()
                    .map(e -> new MonthlyBreakdown(e.getKey().format(monthFormat), e.getValue()))
                    .collect(Collectors.toList());

            return new LinkStatsResponse(
                    link.getTargetUrl(),
                    totalClicks,
                    totalEarnings,
                    monthlyBreakdown
            );
        });
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = generateShortCode();
            if (linkRepository.findByShortCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique short code after " + MAX_RETRIES + " retries");
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }
}
