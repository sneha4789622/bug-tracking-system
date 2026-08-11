package com.bugtracker.repository;

import com.bugtracker.model.Attachment;
import com.bugtracker.model.Bug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByBugOrderByUploadedAtDesc(Bug bug);

    long countByBug(Bug bug);
}