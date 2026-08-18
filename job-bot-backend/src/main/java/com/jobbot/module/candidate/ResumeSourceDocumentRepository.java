package com.jobbot.module.candidate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeSourceDocumentRepository extends JpaRepository<ResumeSourceDocument, UUID> {
    List<ResumeSourceDocument> findByProfileId(UUID profileId);
}

