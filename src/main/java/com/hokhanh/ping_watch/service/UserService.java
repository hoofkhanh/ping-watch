package com.hokhanh.ping_watch.service;


import com.hokhanh.ping_watch.request.ConfirmOtpRequest;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.RegisterResponse;

public interface UserService {
    void register(RegisterRequest request);
    RegisterResponse confirmOtp(ConfirmOtpRequest request);
}
