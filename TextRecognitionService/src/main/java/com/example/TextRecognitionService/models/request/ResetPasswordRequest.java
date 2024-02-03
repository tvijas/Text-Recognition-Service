package com.example.TextRecognitionService.models.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private int code;
    private String password;
}
