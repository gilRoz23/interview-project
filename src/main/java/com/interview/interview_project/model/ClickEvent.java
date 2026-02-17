package com.interview.interview_project.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events", indexes = {
    @Index(name = "idx_click_link_id", columnList = "link_id"),
    @Index(name = "idx_click_clicked_at", columnList = "clickedAt")
})
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    private Boolean fraudValid;

    @Column(precision = 10, scale = 2)
    private BigDecimal creditAwarded = BigDecimal.ZERO;

    public ClickEvent() {
    }

    public ClickEvent(Link link) {
        this.link = link;
        this.clickedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Link getLink() { return link; }
    public void setLink(Link link) { this.link = link; }

    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }

    public Boolean getFraudValid() { return fraudValid; }
    public void setFraudValid(Boolean fraudValid) { this.fraudValid = fraudValid; }

    public BigDecimal getCreditAwarded() { return creditAwarded; }
    public void setCreditAwarded(BigDecimal creditAwarded) { this.creditAwarded = creditAwarded; }
}
