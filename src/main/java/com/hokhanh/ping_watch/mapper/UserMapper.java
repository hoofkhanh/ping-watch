package com.hokhanh.ping_watch.mapper;

import org.springframework.stereotype.Component;

import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.RegisterResponse;

@Component
public class UserMapper {

    public User toUser(RegisterRequest request) {
        return User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .username(request.username())
            .password(request.password())
            .email(request.email())
            .build();
    }

    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername() ,user.getEmail());
    }
}
