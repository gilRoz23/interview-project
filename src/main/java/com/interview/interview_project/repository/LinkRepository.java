package com.interview.interview_project.repository;

import com.interview.interview_project.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByTargetUrl(String targetUrl);

    Optional<Link> findByShortCode(String shortCode);
}
