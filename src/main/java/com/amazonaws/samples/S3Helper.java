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

public class S3Helper {

    private final AmazonS3 s3;

    public S3Helper() {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    // Upload a local file to S3
    public void uploadFile(String bucketName, String key, File file) {
        s3.putObject(new PutObjectRequest(bucketName, key, file));
    }

    // Download an S3 object to a local file
    public void downloadFile(String bucketName, String key, File destination) throws Exception {
        S3Object obj = s3.getObject(new GetObjectRequest(bucketName, key));

        try (InputStream in = obj.getObjectContent();
             FileOutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int len;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    // Generate a presigned PUT URL for frontend upload
    public URL generatePresignedUploadUrl(String bucketName, String key, int expirationMinutes) {
        Date expiration = getExpirationDate(expirationMinutes);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(com.amazonaws.HttpMethod.PUT)
                .withExpiration(expiration);

        return s3.generatePresignedUrl(request);
    }

    // Download image from external URL, upload it to S3, and return S3 URL
    public String uploadImageFromUrl(String bucketName, String imageUrl, String key) throws Exception {
        File tempFile = File.createTempFile("artist-image-", ".jpg");

        try (InputStream in = new URL(imageUrl).openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int len;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        uploadFile(bucketName, key, tempFile);
        tempFile.delete();

        return "https://" + bucketName + ".s3.amazonaws.com/" + key;
    }

    // Create a safe S3 key from artist name
    public String buildImageKey(String artist) {
        return "artists/" + artist.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
    }

    private Date getExpirationDate(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }
}