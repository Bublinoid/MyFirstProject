package com.example.manicurebot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpPropertiesConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String keyPath;


    public String getProjectId() {
        return projectId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKeyPath() {
        return keyPath;
    }
}
