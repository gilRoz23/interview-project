package com.interview.interview_project.dto;

import java.math.BigDecimal;

public class MonthlyBreakdown {

    private String month;
    private BigDecimal earnings;

    public MonthlyBreakdown() {
    }

    public MonthlyBreakdown(String month, BigDecimal earnings) {
        this.month = month;
        this.earnings = earnings;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public BigDecimal getEarnings() { return earnings; }
    public void setEarnings(BigDecimal earnings) { this.earnings = earnings; }
}
