package com.interview.interview_project.controller;

import com.interview.interview_project.model.ClickEvent;
import com.interview.interview_project.model.Link;
import com.interview.interview_project.repository.ClickEventRepository;
import com.interview.interview_project.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @BeforeEach
    void setUp() {
        clickEventRepository.deleteAll();
        linkRepository.deleteAll();
    }

    @Test
    void postLinks_shouldCreateShortLink() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\": \"https://fiverr.com/seller/gig\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.targetUrl").value("https://fiverr.com/seller/gig"));
    }

    @Test
    void postLinks_shouldReturnSameLinkForSameUrl() throws Exception {
        String body = "{\"targetUrl\": \"https://fiverr.com/same-url\"}";

        MvcResult first = mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        assertEquals(
                first.getResponse().getContentAsString(),
                second.getResponse().getContentAsString(),
                "Same URL should return same short link"
        );
    }

    @Test
    void postLinks_shouldReturn400ForEmptyUrl() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUrl\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("targetUrl is required")));
    }

    @Test
    void postLinks_shouldReturn400ForMissingUrl() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShortCode_shouldRedirectToTargetUrl() throws Exception {
        Link link = new Link("testcode", "https://fiverr.com/redirect-target");
        link.setCreatedAt(LocalDateTime.now());
        linkRepository.save(link);

        mockMvc.perform(get("/testcode"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://fiverr.com/redirect-target"));
    }

    @Test
    void getShortCode_shouldReturn404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/unknowncode"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("unknowncode")));
    }

    @Test
    void getShortCode_shouldTrackClickAsynchronously() throws Exception {
        Link link = new Link("tracked1", "https://fiverr.com/tracked");
        link.setCreatedAt(LocalDateTime.now());
        linkRepository.save(link);

        mockMvc.perform(get("/tracked1"))
                .andExpect(status().isFound());

        // Wait for async processing (fraud validation takes 500ms)
        Thread.sleep(1500);

        long clickCount = clickEventRepository.countByLinkId(link.getId());
        assertEquals(1, clickCount, "Click should be recorded");
    }

    @Test
    void getStats_shouldReturnPaginatedResults() throws Exception {
        // Create a link with click data
        Link link = new Link("stats01", "https://fiverr.com/stats-test");
        link.setCreatedAt(LocalDateTime.now());
        linkRepository.save(link);

        ClickEvent click1 = new ClickEvent(link);
        click1.setFraudValid(true);
        click1.setCreditAwarded(new BigDecimal("0.05"));
        clickEventRepository.save(click1);

        ClickEvent click2 = new ClickEvent(link);
        click2.setFraudValid(false);
        click2.setCreditAwarded(BigDecimal.ZERO);
        clickEventRepository.save(click2);

        mockMvc.perform(get("/stats").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].url").value("https://fiverr.com/stats-test"))
                .andExpect(jsonPath("$.content[0].totalClicks").value(2))
                .andExpect(jsonPath("$.content[0].totalEarnings").value(0.05));
    }

    @Test
    void getStats_shouldReturnEmptyForNoLinks() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getStats_shouldRejectInvalidPageSize() throws Exception {
        mockMvc.perform(get("/stats").param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Page size")));
    }
}
