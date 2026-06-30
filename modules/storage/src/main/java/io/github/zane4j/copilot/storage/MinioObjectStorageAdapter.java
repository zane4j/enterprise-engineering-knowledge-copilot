package io.github.zane4j.copilot.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.DigestInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/** MinIO/S3-compatible implementation for original document objects. */
public final class MinioObjectStorageAdapter implements ObjectStoragePort {

    private static final long PART_SIZE = 10L * 1024 * 1024;
    private static final int MAX_PRESIGNED_URL_SECONDS = 7 * 24 * 60 * 60;

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioObjectStorageAdapter(MinioClient minioClient, String bucketName) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName must not be null");
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to initialize object storage bucket", exception);
        }
    }

    @Override
    public StoredObject put(UploadCommand command, InputStream content) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (command.contentLength() <= 0) {
            throw new ObjectStorageException("Object content length must be greater than zero");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(content, digest)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(command.objectKey())
                        .contentType(command.contentType())
                        .stream(digestInput, command.contentLength(), PART_SIZE)
                        .build());
            }
            return new StoredObject(command.objectKey(), HexFormat.of().formatHex(digest.digest()));
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to upload object to storage", exception);
        }
    }

    @Override
    public URI createDownloadUrl(String objectKey, Duration ttl) {
        Objects.requireNonNull(objectKey, "objectKey must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        long seconds = ttl.toSeconds();
        if (seconds <= 0 || seconds > MAX_PRESIGNED_URL_SECONDS) {
            throw new ObjectStorageException("Download URL expiry must be between 1 second and 7 days");
        }

        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry((int) seconds)
                    .build());
            return URI.create(url);
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create download URL", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to delete object from storage", exception);
        }
    }
}
