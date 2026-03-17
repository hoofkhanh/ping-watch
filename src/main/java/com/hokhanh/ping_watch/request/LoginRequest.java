package com.hokhanh.ping_watch.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest (
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
){}
