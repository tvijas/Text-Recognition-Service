package com.example.TextRecognitionService.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RefreshRequest {
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("access_token")
    private String accessToken;
}
