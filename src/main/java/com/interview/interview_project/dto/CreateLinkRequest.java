package com.interview.interview_project.dto;

public class CreateLinkRequest {

    private String targetUrl;

    public CreateLinkRequest() {
    }

    public CreateLinkRequest(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
}
