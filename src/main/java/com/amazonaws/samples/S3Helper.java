package com.amazonaws.samples;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

/**
 * Small helper utility for basic S3 operations used by the frontend workflow.
 * - uploadFile: server-side upload
 * - downloadFile: server-side download to local file
 * - generatePresignedUploadUrl: generate a presigned PUT url so frontend can upload directly to S3
 */
public class S3Helper {

    private final AmazonS3 s3;

    public S3Helper() {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    /**
     * Upload a file from the server to S3.
     */
    public void uploadFile(String bucketName, String key, File file) {
        s3.putObject(new PutObjectRequest(bucketName, key, file));
    }

    /**
     * Download an object from S3 to a local destination file.
     */
    public void downloadFile(String bucketName, String key, File destination) throws Exception {
        S3Object obj = s3.getObject(new GetObjectRequest(bucketName, key));
        try (InputStream in = obj.getObjectContent(); FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    /**
     * Generate a presigned URL (PUT) that the frontend can use to upload directly to S3.
     * expirationMinutes sets how long the URL is valid from now.
     */
    public URL generatePresignedUploadUrl(String bucketName, String key, int expirationMinutes) {
        Date expiration = getExpirationDate(expirationMinutes);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(com.amazonaws.HttpMethod.PUT)
                .withExpiration(expiration);
        return s3.generatePresignedUrl(req);
    }

    private Date getExpirationDate(int minutes) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, minutes);
        return c.getTime();
    }
}
