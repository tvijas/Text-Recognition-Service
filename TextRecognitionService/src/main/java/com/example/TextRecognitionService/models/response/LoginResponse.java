package com.example.TextRecognitionService.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data @AllArgsConstructor
public class LoginResponse {
    private Map<String,String> errors;
    @JsonProperty("http_status")
    private int httpStatus;
}
