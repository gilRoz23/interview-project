package com.interview.interview_project.controller;

import com.interview.interview_project.dto.CreateLinkRequest;
import com.interview.interview_project.dto.CreateLinkResponse;
import com.interview.interview_project.dto.LinkStatsResponse;
import com.interview.interview_project.model.Link;
import com.interview.interview_project.service.ClickTrackingService;
import com.interview.interview_project.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final ClickTrackingService clickTrackingService;

    public LinkController(LinkService linkService, ClickTrackingService clickTrackingService) {
        this.linkService = linkService;
        this.clickTrackingService = clickTrackingService;
    }

    /**
     * POST /links - Create a short link for a target URL.
     * Idempotent: returns existing short link if target URL was already shortened.
     */
    @PostMapping("/links")
    public ResponseEntity<CreateLinkResponse> createLink(@RequestBody CreateLinkRequest request,
                                                         HttpServletRequest httpRequest) {
        Link link = linkService.createShortLink(request.getTargetUrl());

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + ":" + httpRequest.getServerPort();
        String shortUrl = baseUrl + "/" + link.getShortCode();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateLinkResponse(shortUrl, link.getTargetUrl()));
    }

    /**
     * GET /:shortCode - Redirect to target URL and track the click asynchronously.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        Link link = linkService.getByShortCode(shortCode);

        clickTrackingService.processClick(link);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getTargetUrl()))
                .build();
    }

    /**
     * GET /stats - Paginated global analytics for all links.
     */
    @GetMapping("/stats")
    public ResponseEntity<Page<LinkStatsResponse>> getStats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<LinkStatsResponse> stats = linkService.getStats(pageable);
        return ResponseEntity.ok(stats);
    }
}
