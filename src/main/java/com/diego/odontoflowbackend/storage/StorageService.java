package com.diego.odontoflowbackend.storage;

import java.time.Duration;

/** Abstraction over object storage (S3/MinIO) so it can be mocked in tests. */
public interface StorageService {

    /** Pre-signed PUT URL for direct browser upload. */
    String presignUpload(String key, String contentType, Duration ttl);

    /** Pre-signed GET URL for temporary download. */
    String presignDownload(String key, Duration ttl);

    void delete(String key);
}
