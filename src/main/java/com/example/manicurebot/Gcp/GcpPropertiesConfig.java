package com.example.manicurebot.Gcp;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для свойств GCP, exception by null, final object, security
 */
@Getter
@Configuration
public class GcpPropertiesConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String keyPath;


}
