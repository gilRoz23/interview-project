package com.interview.interview_project.dto;

public class CreateLinkResponse {

    private String shortUrl;
    private String targetUrl;

    public CreateLinkResponse() {
    }

    public CreateLinkResponse(String shortUrl, String targetUrl) {
        this.shortUrl = shortUrl;
        this.targetUrl = targetUrl;
    }

    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
}
