package com.bugtracker.repository;

import com.bugtracker.model.Bug;
import com.bugtracker.model.BugHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugHistoryRepository extends JpaRepository<BugHistory, Long> {

    // All history entries for a bug, most recent first
    List<BugHistory> findByBugOrderByChangedAtDesc(Bug bug);
}