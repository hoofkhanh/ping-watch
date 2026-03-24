package com.hokhanh.ping_watch.response;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        String accessToken,
        String refreshToken) {
}
