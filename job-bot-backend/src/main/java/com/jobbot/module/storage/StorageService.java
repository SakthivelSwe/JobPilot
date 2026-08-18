package com.jobbot.module.storage;

/**
 * Pluggable binary storage. Render's filesystem is ephemeral (audit §7), so prod must
 * use Supabase Storage. Phase 1 ships a local dev implementation; a Supabase impl is
 * wired in a later phase. Only metadata (path/name/mime/size/checksum) is kept in the DB.
 */
public interface StorageService {

    /**
     * Persist bytes and return an opaque storage path/key.
     *
     * @param folder   logical folder, e.g. "resumes"
     * @param fileName original file name
     * @param bytes    file content
     * @return storage path/key that can later resolve to a URL
     */
    String store(String folder, String fileName, byte[] bytes);

    /** Best-effort public/signed URL for a stored path (may be a local file: URI in dev). */
    String url(String storagePath);
}

