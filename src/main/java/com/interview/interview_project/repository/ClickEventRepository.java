package com.interview.interview_project.repository;

import com.interview.interview_project.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByLinkId(Long linkId);

    @Query("SELECT COALESCE(SUM(c.creditAwarded), 0) FROM ClickEvent c WHERE c.link.id = :linkId")
    java.math.BigDecimal sumCreditsByLinkId(@Param("linkId") Long linkId);

    List<ClickEvent> findByLinkId(Long linkId);
}
