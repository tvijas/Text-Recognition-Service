package com.example.TextRecognitionService.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class RecognizeRequest {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("base64Data")
    private String base64Data;
}

