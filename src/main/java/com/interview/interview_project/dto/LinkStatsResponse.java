package com.interview.interview_project.dto;

import java.math.BigDecimal;
import java.util.List;

public class LinkStatsResponse {

    private String url;
    private long totalClicks;
    private BigDecimal totalEarnings;
    private List<MonthlyBreakdown> monthlyBreakdown;

    public LinkStatsResponse() {
    }

    public LinkStatsResponse(String url, long totalClicks, BigDecimal totalEarnings,
                             List<MonthlyBreakdown> monthlyBreakdown) {
        this.url = url;
        this.totalClicks = totalClicks;
        this.totalEarnings = totalEarnings;
        this.monthlyBreakdown = monthlyBreakdown;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public List<MonthlyBreakdown> getMonthlyBreakdown() { return monthlyBreakdown; }
    public void setMonthlyBreakdown(List<MonthlyBreakdown> monthlyBreakdown) { this.monthlyBreakdown = monthlyBreakdown; }
}
