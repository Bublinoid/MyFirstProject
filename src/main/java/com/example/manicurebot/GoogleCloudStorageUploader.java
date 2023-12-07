package com.example.manicurebot;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class GoogleCloudStorageUploader {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorageUploader.class);

    private Storage storage;
    private GcpPropertiesConfig gcpProperties;

    @Autowired
    public GoogleCloudStorageUploader(GcpPropertiesConfig gcpProperties) {
        this.gcpProperties = gcpProperties;
        initializeStorage();
    }

    @PostConstruct
    private void initializeStorage() {
        try {
            storage = StorageOptions.newBuilder()
                    .setProjectId(gcpProperties.getProjectId())
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpProperties.getKeyPath())))
                    .build()
                    .getService();
        } catch (IOException e) {
            logger.error("Failed to initialize Google Cloud Storage:", e);
        }
    }

    public void uploadFile(String localFilePath, String destinationFileName) {
        try (InputStream inputStream = new FileInputStream(localFilePath)) {
            BlobId blobId = BlobId.of(gcpProperties.getBucketName(), destinationFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            storage.create(blobInfo, Files.readAllBytes(Paths.get(localFilePath)));
            logger.info("File successfully uploaded to the bucket.");
        } catch (IOException e) {
            logger.error("Failed to upload file to Google Cloud Storage:", e);
        }
    }

    public void uploadFile(InputStream inputStream, String destinationFileName) {
        try {
            BlobId blobId = BlobId.of(gcpProperties.getBucketName(), destinationFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            byte[] bytes = readBytesFromInputStream(inputStream);
            storage.create(blobInfo, bytes);
            logger.info("File successfully uploaded to the bucket.");
        } catch (IOException e) {
            logger.error("Failed to upload file to Google Cloud Storage:", e);
        }
    }

    private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}
