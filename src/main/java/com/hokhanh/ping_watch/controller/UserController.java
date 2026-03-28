package com.hokhanh.ping_watch.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import com.hokhanh.ping_watch.request.ConfirmOtpRequest;
import com.hokhanh.ping_watch.request.LoginRequest;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.LoginResponse;
import com.hokhanh.ping_watch.response.RegisterResponse;
import com.hokhanh.ping_watch.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@ConditionalOnExpression("'${app.role:all}' == 'api' || '${app.role:all}' == 'all'")
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        log.info("Received registration request: {}", request);

        userService.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm-otp")
    public ResponseEntity<RegisterResponse> confirmOtp(@RequestBody @Valid ConfirmOtpRequest request) {
        log.info("Received confirmOtp request: {}", request);

        RegisterResponse response = userService.confirmOtp(request);

        URI location = URI.create("/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("Received login request: {}", request);
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<String> refreshToken(
            @RequestParam @NotBlank(message = "Username is required") String username,
            HttpServletRequest request) {
        log.info("Received refresh token request");
        String refreshToken = (String) request.getAttribute("refreshToken");
        String response = userService.refreshToken(username, refreshToken);
        return ResponseEntity.ok(response);
    }

}
