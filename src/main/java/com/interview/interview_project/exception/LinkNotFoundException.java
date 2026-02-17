package com.interview.interview_project.exception;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String shortCode) {
        super("Link not found with short code: " + shortCode);
    }
}
