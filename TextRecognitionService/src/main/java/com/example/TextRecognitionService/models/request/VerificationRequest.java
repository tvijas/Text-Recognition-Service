package com.example.TextRecognitionService.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data
public class VerificationRequest {
    private String email;
    @JsonProperty("verification_code")
    private int vcode;
}
