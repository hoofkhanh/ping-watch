package com.hokhanh.ping_watch.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmOtpRequest(
    @NotBlank(message = "OTP must not be blank")
    String otp,

    @NotBlank(message = "Username must not be blank")
    String username
) {

}
