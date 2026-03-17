package com.hokhanh.ping_watch.controller;

import org.springframework.web.bind.annotation.RestController;

import com.hokhanh.ping_watch.request.LoginRequest;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest request, Principal principal) {
        String name = principal != null ? principal.getName() : "anonymous";
        log.info("Login request received for user: {}",  name);
        return ResponseEntity.ok("test login");
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<String> refreshToken(@RequestBody @Valid LoginRequest request, Principal principal) {
        String name = principal != null ? principal.getName() : "anonymous";
        log.info("refreshToken request received for user: {}", name);
        return ResponseEntity.ok("test refreshToken");
    }

    @PostMapping("/block")
    public ResponseEntity<String> block(@RequestBody @Valid LoginRequest request, Principal principal) {
        String name = principal != null ? principal.getName() : "anonymous";
        log.info("block request received for user: {}", name);
        return ResponseEntity.ok("test block");
    }
    
}
