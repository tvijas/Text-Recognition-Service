package com.example.TextRecognitionService.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
@Data @AllArgsConstructor
public class TokensAndErrors {
    private Map<String,String> tokens;
    private Map<String,String> errors;
}
