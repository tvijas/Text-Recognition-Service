package com.example.TextRecognitionService.configuration;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class GoogleVisionConfiguration {
    @Bean
    @SneakyThrows
    public ImageAnnotatorClient getVision() {
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        Path filePath = Paths.get(currentPath.toString(), "src", "main", "resources", "credentials.json");

        return ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(filePath.toString())))).build());
    }
}
