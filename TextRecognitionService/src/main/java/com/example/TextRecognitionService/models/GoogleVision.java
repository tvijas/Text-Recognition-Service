package com.example.TextRecognitionService.models;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Data
@Component
public class GoogleVision {
    private final ImageAnnotatorClient vision;
@Autowired
    public GoogleVision(ImageAnnotatorClient vision) {
        this.vision = vision;
    }
}
