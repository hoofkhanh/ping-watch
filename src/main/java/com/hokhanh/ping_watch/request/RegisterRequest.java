package com.hokhanh.ping_watch.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank(message = "firstName is required")
    String firstName,

    @NotBlank(message = "lastName is required")
    String lastName,

    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password,

    @NotBlank(message = "Password confirmation is required")
    String passwordConfirmation,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    // used for storing OTP in Redis, not required in the request body
    String otp
) {

}
