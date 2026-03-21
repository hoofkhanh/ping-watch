package com.hokhanh.ping_watch.controller;

import org.springframework.web.bind.annotation.RestController;

import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.request.ConfirmOtpRequest;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.RegisterResponse;
import com.hokhanh.ping_watch.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
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
    
    
}
