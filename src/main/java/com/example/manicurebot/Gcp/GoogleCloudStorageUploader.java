package com.example.manicurebot.Gcp;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;

@Component
public class GoogleCloudStorageUploader {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorageUploader.class);

    private final GcpPropertiesConfig gcpProperties;

    @Autowired
    public GoogleCloudStorageUploader(GcpPropertiesConfig gcpProperties) {
        this.gcpProperties = gcpProperties;
        initializeStorage();
    }

    @PostConstruct
    private void initializeStorage() {
        try {
            Storage storage = StorageOptions.newBuilder()
                    .setProjectId(gcpProperties.getProjectId())
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(gcpProperties.getKeyPath())))
                    .build()
                    .getService();
        } catch (IOException e) {
            logger.error("Failed to initialize Google Cloud Storage:", e);
        }
    }


}
