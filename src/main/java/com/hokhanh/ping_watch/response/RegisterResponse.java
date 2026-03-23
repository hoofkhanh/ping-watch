package com.hokhanh.ping_watch.response;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email) {

}
