package com.bugtracker.service;

import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.model.Attachment;
import com.bugtracker.model.Bug;
import com.bugtracker.repository.AttachmentRepository;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * AttachmentService — handles file upload, storage, and retrieval.
 *
 * Files are stored in the local filesystem under the configured
 * upload directory. The database stores only metadata.
 */
@Service
public class AttachmentService {

    private static final Logger log =
            LoggerFactory.getLogger(AttachmentService.class);

    /** Maximum file size: 10 MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Allowed file types (MIME types) */
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "text/plain",
            "application/zip",
            "application/vnd.openxmlformats-officedocument"
                    + ".wordprocessingml.document"
    );

    private final AttachmentRepository attachmentRepository;
    private final BugRepository        bugRepository;

    /**
     * Directory where uploaded files are stored.
     * Configured in application.properties as app.upload.dir
     * Defaults to "uploads" in the project root.
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             BugRepository        bugRepository) {
        this.attachmentRepository = attachmentRepository;
        this.bugRepository        = bugRepository;
    }

    /**
     * Saves an uploaded file to the filesystem and records
     * its metadata in the database.
     *
     * Security checks performed:
     *   1. File must not be empty
     *   2. File size must be under MAX_FILE_SIZE (10 MB)
     *   3. MIME type must be in the ALLOWED_TYPES whitelist
     *   4. Original filename is sanitized — we never use it
     *      as the stored filename to prevent path traversal
     *
     * @param bugId the bug this file is attached to
     * @param file  the uploaded file from the multipart form
     * @return the saved Attachment metadata record
     */
    @Transactional
    public Attachment uploadAttachment(Long bugId, MultipartFile file)
            throws IOException {

        // Validate: not empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Validate: size limit
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds limit of 10 MB");
        }

        // Validate: allowed MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + contentType);
        }

        // Load the bug
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bug not found with ID: " + bugId));

        // Generate a safe stored filename using UUID
        // This prevents name collisions and path traversal attacks
        String extension  = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString() + extension;

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Write file to disk
        Path targetPath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath,
                StandardCopyOption.REPLACE_EXISTING);

        log.info("File '{}' stored as '{}' ({} bytes)",
                file.getOriginalFilename(), storedName, file.getSize());

        // Save metadata to database
        Attachment attachment = new Attachment(
                file.getOriginalFilename(),
                storedName,
                contentType,
                file.getSize(),
                bug,
                SecurityUtils.getCurrentUser()
        );

        return attachmentRepository.save(attachment);
    }

    /**
     * Loads a file from the filesystem as a Spring Resource.
     * Used by the download endpoint to serve file bytes.
     *
     * @param attachmentId the attachment record ID
     * @return the file as a Resource (for streaming to the client)
     */
    @Transactional(readOnly = true)
    public Resource loadAttachment(Long attachmentId)
            throws MalformedURLException {

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with ID: " + attachmentId));

        Path filePath = Paths.get(uploadDir)
                .resolve(attachment.getStoredFilename());

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException(
                    "File not found on server: "
                            + attachment.getOriginalFilename());
        }

        return resource;
    }

    /**
     * Returns the Attachment metadata for a given ID.
     */
    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with ID: " + attachmentId));
    }

    /**
     * Returns all attachments for a bug, newest first.
     */
    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsForBug(Long bugId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bug not found: " + bugId));
        return attachmentRepository.findByBugOrderByUploadedAtDesc(bug);
    }

    /**
     * Deletes a file from both the filesystem and the database.
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found: " + attachmentId));

        // Delete from filesystem first
        Path filePath = Paths.get(uploadDir)
                .resolve(attachment.getStoredFilename());
        Files.deleteIfExists(filePath);

        log.info("Deleted attachment '{}' (file: '{}')",
                attachment.getOriginalFilename(),
                attachment.getStoredFilename());

        // Delete database record
        attachmentRepository.delete(attachment);
    }

    // ── Private Helpers ───────────────────────────────────────

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}