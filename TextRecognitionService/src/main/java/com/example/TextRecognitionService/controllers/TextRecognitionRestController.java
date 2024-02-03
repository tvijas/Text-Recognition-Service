package com.example.TextRecognitionService.controllers;

import com.example.TextRecognitionService.Utils.LoginValidator;
import com.example.TextRecognitionService.Utils.RegistrationValidator;
import com.example.TextRecognitionService.Utils.VerificationCodeGenerator;
import com.example.TextRecognitionService.models.TokensAndErrors;
import com.example.TextRecognitionService.models.entity.UserEntity;
import com.example.TextRecognitionService.models.request.*;
import com.example.TextRecognitionService.models.response.LoginResponse;
import com.example.TextRecognitionService.models.response.RecognizeResponse;
import com.example.TextRecognitionService.models.response.SignupResponse;
import com.example.TextRecognitionService.models.response.VerificationResponse;
import com.example.TextRecognitionService.models.template.MailTemplate;
import com.example.TextRecognitionService.properties.JwtProps;
import com.example.TextRecognitionService.repositories.UserRepository;
import com.example.TextRecognitionService.services.EmailSenderService;
import com.example.TextRecognitionService.services.OauthService;
import com.example.TextRecognitionService.services.TextRecognitionService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TextRecognitionRestController {
    private final TextRecognitionService textRecognitionService;
    private final RegistrationValidator registrationValidator;
    private final EmailSenderService emailSenderService;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final OauthService oauthService;
    private final LoginValidator loginValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtProps jwtProps;

    @PostMapping
    @RequestMapping(("/recognize"))
    public RecognizeResponse detectText(@RequestBody RecognizeRequest recognizeRequest) {
        String accessToken = recognizeRequest.getAccessToken();
        Map<String, String> errors = oauthService.verifyAccessToken(accessToken);

        if (errors != null) {
            return new RecognizeResponse(null, errors, HttpStatus.SC_BAD_REQUEST);
        } else {
            UserEntity userEntity = oauthService.checkLogin(oauthService.extractLoginFromToken(accessToken));
            userEntity.setAmountOfRequests(userEntity.getAmountOfRequests() + 1);
            oauthService.save(userEntity);
            String text = textRecognitionService.detectText(recognizeRequest.getBase64Data());
            return new RecognizeResponse(text, null, HttpStatus.SC_OK);
        }
    }

    // jwt.verify time exception
    // /refresh - загорнуть в cookie
    // всі отвєти кроми refresh олжні буть в JSON форматі
    //
    @PatchMapping
    @RequestMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestBody RefreshRequest request, HttpServletResponse response) throws JsonProcessingException {

        TokensAndErrors tokensAndErrors = oauthService.refreshRefreshAndAccessToken
                (request.getRefreshToken(), request.getAccessToken());

        if (tokensAndErrors.getTokens() != null) {
            Cookie refreshCookie = new Cookie("refresh_token", tokensAndErrors.getTokens().get("refresh_token"));
            refreshCookie.setMaxAge(jwtProps.getRefreshTokenDuration());
            response.addCookie(refreshCookie);

            Cookie accessCookie = new Cookie("access_cookie", tokensAndErrors.getTokens().get("access_token"));
            accessCookie.setMaxAge(jwtProps.getAccessTokenDuration());
            response.addCookie(accessCookie);

            return new ResponseEntity<>(null, HttpStatusCode.valueOf(HttpStatus.SC_ACCEPTED));
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            return new ResponseEntity<>(objectMapper.writeValueAsString(tokensAndErrors.getErrors()),
                    HttpStatusCode.valueOf(HttpStatus.SC_BAD_REQUEST));
        }
    }

    //доробить BCrypt
    //зробить востановлэныэ пароля
    //протестить
    //настройить отвєты Restконтроллэра
    @PostMapping
    @RequestMapping("/signup")
    public SignupResponse signupUser(@RequestBody @Validated SignupRequest request, BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        registrationValidator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            }
            return new SignupResponse(errors, HttpStatus.SC_BAD_REQUEST);
        } else {
            UserEntity userEntity = new UserEntity(request.getEmail(),
                    request.getLogin(), passwordEncoder.encode(request.getPassword()));

            MailTemplate mailTemplate = new MailTemplate();
            int vcode = verificationCodeGenerator.generateCode();
            mailTemplate.setStandardEmailTemplate(request.getEmail(), vcode);
            emailSenderService.sendEmail(mailTemplate);
            userEntity.setCode(vcode);
            userEntity.setCreationTime(Duration.ofMillis(jwtProps.getAccessTokenDuration()));
            oauthService.save(userEntity);
            return new SignupResponse(null, HttpStatus.SC_OK);
        }
    }

    @PostMapping
    @RequestMapping("/confirm")
    public VerificationResponse verificationRequest(@RequestBody VerificationRequest vr) {
        Map<String, String> errors = new HashMap<>();
        Optional<UserEntity> user = userRepository.findByEmailAndCode(vr.getEmail(), vr.getVcode());
        if (user.isPresent()) {
            UserEntity userEntity = user.get();
            if (!userEntity.getExpirationTime().isBefore(LocalDateTime.now())) {
                oauthService.confirmUser(vr.getEmail(), vr.getVcode(), LocalDateTime.now());
                return new VerificationResponse(null, HttpStatus.SC_OK);
            } else {
                errors.put("CodeExpiredException", "verification code has been expired");
                return new VerificationResponse(errors, HttpStatus.SC_BAD_REQUEST);
            }
        } else {
            errors.put("EmailAndCodeNotFoundException", "email and code were not found");
            return new VerificationResponse(errors, HttpStatus.SC_BAD_REQUEST);
        }
    }

    @PostMapping
    @RequestMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, BindingResult bindingResult, HttpServletResponse response) {
        Map<String, String> errors = new HashMap<>();
        loginValidator.validate(request, bindingResult);
        System.out.println(bindingResult);
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                String fieldName = error.getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            }
            return new ResponseEntity<>(new LoginResponse(errors,HttpStatus.SC_NOT_FOUND), HttpStatusCode.valueOf(HttpStatus.SC_NOT_FOUND));
        } else {
            String[] refreshAndAccessToken = oauthService.createRefreshAndAccessToken(request.getLogin());

            Cookie refreshCookie = new Cookie("refresh_token", refreshAndAccessToken[0]);
            refreshCookie.setMaxAge(jwtProps.getRefreshTokenDuration());
            response.addCookie(refreshCookie);

            Cookie accessCookie = new Cookie("access_cookie", refreshAndAccessToken[1]);
            accessCookie.setMaxAge(jwtProps.getAccessTokenDuration());
            response.addCookie(accessCookie);
            return new ResponseEntity<>( new LoginResponse(errors, HttpStatus.SC_ACCEPTED), HttpStatusCode.valueOf(HttpStatus.SC_ACCEPTED));
        }
    }

    @PostMapping
    @RequestMapping("/resendCode")
    public ResponseEntity<String> resendCode(@RequestBody String email) {
        UserEntity userEntity = oauthService.checkEmail(email);
        if (userEntity != null) {
            MailTemplate mailTemplate = new MailTemplate();
            int vcode = verificationCodeGenerator.generateCode();
            mailTemplate.setStandardEmailTemplate(email, vcode);
            emailSenderService.sendEmail(mailTemplate);
            userEntity.setCode(vcode);
            userEntity.setCreationTime(Duration.ofMillis(jwtProps.getAccessTokenDuration()));
            oauthService.save(userEntity);
            return new ResponseEntity<>(null, HttpStatusCode.valueOf(HttpStatus.SC_OK));
        } else {
            return new ResponseEntity<>(null, HttpStatusCode.valueOf(HttpStatus.SC_BAD_REQUEST));
        }
    }

    @PostMapping
    @RequestMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (userRepository.resetPassword(request.getPassword(), request.getEmail(), request.getCode()) == 1) {
            return new ResponseEntity<>(org.springframework.http.HttpStatus.OK);
        } else {
            return new ResponseEntity<>(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
