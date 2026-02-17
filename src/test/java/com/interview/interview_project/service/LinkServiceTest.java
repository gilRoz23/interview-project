package com.interview.interview_project.service;

import com.interview.interview_project.exception.LinkNotFoundException;
import com.interview.interview_project.model.Link;
import com.interview.interview_project.repository.ClickEventRepository;
import com.interview.interview_project.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(linkRepository, clickEventRepository);
    }

    @Test
    void createShortLink_shouldCreateNewLink() {
        String targetUrl = "https://fiverr.com/seller/gig";
        when(linkRepository.findByTargetUrl(targetUrl)).thenReturn(Optional.empty());
        when(linkRepository.findByShortCode(any())).thenReturn(Optional.empty());
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> {
            Link link = invocation.getArgument(0);
            link.setId(1L);
            return link;
        });

        Link result = linkService.createShortLink(targetUrl);

        assertNotNull(result);
        assertEquals(targetUrl, result.getTargetUrl());
        assertNotNull(result.getShortCode());
        assertEquals(7, result.getShortCode().length());
        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void createShortLink_shouldReturnExistingForSameUrl() {
        String targetUrl = "https://fiverr.com/seller/gig";
        Link existing = new Link("abc1234", targetUrl);
        existing.setId(1L);
        when(linkRepository.findByTargetUrl(targetUrl)).thenReturn(Optional.of(existing));

        Link result = linkService.createShortLink(targetUrl);

        assertEquals("abc1234", result.getShortCode());
        verify(linkRepository, never()).save(any(Link.class));
    }

    @Test
    void createShortLink_shouldRejectNullUrl() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createShortLink(null));
    }

    @Test
    void createShortLink_shouldRejectEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createShortLink(""));
    }

    @Test
    void createShortLink_shouldRejectBlankUrl() {
        assertThrows(IllegalArgumentException.class, () -> linkService.createShortLink("   "));
    }

    @Test
    void getByShortCode_shouldReturnLink() {
        Link link = new Link("abc1234", "https://fiverr.com/seller/gig");
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        Link result = linkService.getByShortCode("abc1234");

        assertEquals("abc1234", result.getShortCode());
    }

    @Test
    void getByShortCode_shouldThrowWhenNotFound() {
        when(linkRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class, () -> linkService.getByShortCode("nope"));
    }

    @Test
    void createShortLink_shortCodeShouldBeAlphanumeric() {
        String targetUrl = "https://fiverr.com/test";
        when(linkRepository.findByTargetUrl(targetUrl)).thenReturn(Optional.empty());
        when(linkRepository.findByShortCode(any())).thenReturn(Optional.empty());
        when(linkRepository.save(any(Link.class))).thenAnswer(i -> i.getArgument(0));

        Link result = linkService.createShortLink(targetUrl);

        assertTrue(result.getShortCode().matches("[a-zA-Z0-9]+"),
                "Short code should be alphanumeric: " + result.getShortCode());
    }
}
