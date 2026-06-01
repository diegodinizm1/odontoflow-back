package com.diego.odontoflowbackend.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(S3Client s3, S3Presigner presigner, @Value("${storage.bucket}") String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @PostConstruct
    void ensureBucket() {
        try {
            s3.headBucket(b -> b.bucket(bucket));
        } catch (NoSuchBucketException e) {
            log.info("Creating storage bucket '{}'", bucket);
            s3.createBucket(b -> b.bucket(bucket));
        } catch (Exception e) {
            log.warn("Could not verify storage bucket '{}': {}", bucket, e.getMessage());
        }
    }

    @Override
    public String presignUpload(String key, String contentType, Duration ttl) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build();
        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(ttl).putObjectRequest(objectRequest).build();
        return presigner.presignPutObject(presign).url().toString();
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl).getObjectRequest(objectRequest).build();
        return presigner.presignGetObject(presign).url().toString();
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(b -> b.bucket(bucket).key(key));
    }
}
