package com.example.TextRecognitionService.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @Email(message = "Isn't email")
    @NotBlank(message = "Email field can't be empty")
    @Size(max = 40, message = "Email is too long")
    private final String email;

    @NotBlank(message = "Login field shouldn't be empty")
    @Size(min = 6, max = 15, message = "Login size should be from 6 to 15 characters")
    private final String login;

    @NotBlank(message = "Password field shouldn't be empty")
    @Size(min = 9, max = 20, message = "Password size should be from 6 to 15 characters")
    @Pattern(regexp = ("[a-zA-Z0-9]"), message = "Password should contain a-z, A-Z, 0-9")
    private final String password;
}
