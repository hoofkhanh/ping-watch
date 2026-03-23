package com.hokhanh.ping_watch.service.impl;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hokhanh.ping_watch.constant.ErrorCode;
import com.hokhanh.ping_watch.mapper.UserMapper;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.UserRepository;
import com.hokhanh.ping_watch.request.ConfirmOtpRequest;
import com.hokhanh.ping_watch.request.LoginRequest;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.LoginResponse;
import com.hokhanh.ping_watch.response.RegisterResponse;
import com.hokhanh.ping_watch.service.EmailService;
import com.hokhanh.ping_watch.service.JwtService;
import com.hokhanh.ping_watch.service.RedisService;
import com.hokhanh.ping_watch.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final RedisService redisService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Value("${jwt.expiration.refresh}")
    private long refreshExpiration;

    private static final int OTP_EXPIRATION_MINUTUE = 2;

    @Override
    public void register(RegisterRequest request) {
        log.info("Processing registration for user: {}", request.username());

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new IllegalArgumentException(ErrorCode.PASSWORD_NOT_MATCH.name());
        }

        validateUserNotExists(request.username(), request.email());

        String otp = generateOtp();

        RegisterRequest requestWithOtp = new RegisterRequest(
                request.firstName(),
                request.lastName(),
                request.username(),
                request.password(),
                request.passwordConfirmation(),
                request.email(),
                otp);

        redisService.set("username:" + request.username(), requestWithOtp, Duration.ofMinutes(2));

        emailService.send(
                request.email(),
                String.format("Your OTP for registration"),
                String.format("Your OTP is valid for %d minutes and your OTP is %s", OTP_EXPIRATION_MINUTUE, otp));
    }

    @Override
    public RegisterResponse confirmOtp(ConfirmOtpRequest request) {
        log.info("Processing confirmOtp using OTP for user: {}", request.username());

        RegisterRequest cachedUserInfo = redisService.get("username:" + request.username(), RegisterRequest.class);
        if (cachedUserInfo == null) {
            throw new IllegalArgumentException(ErrorCode.OTP_EXPIRED.name().toString());
        }

        if (!request.otp().equals(cachedUserInfo.otp())) {
            throw new IllegalArgumentException(ErrorCode.OTP_INVALID.name());
        }

        validateUserNotExists(cachedUserInfo.username(), cachedUserInfo.email());

        redisService.delete("username:" + request.username());

        User user = userRepository.save(userMapper.toUser(cachedUserInfo));
        return userMapper.toRegisterResponse(user);
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    private void validateUserNotExists(String username, String email) {
        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException(ErrorCode.USERNAME_ALREADY_EXISTS.name());
        }

        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException(ErrorCode.EMAIL_ALREADY_EXISTS.name());
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Processing login for user: {}", request.username());
        User user = userRepository.findByUsername(request.username());
        if (user == null) {
            throw new IllegalArgumentException(ErrorCode.USERNAME_NOT_FOUND.name());
        }

        if (!user.getPassword().equals(request.password())) {
            throw new IllegalArgumentException(ErrorCode.PASSWORD_NOT_MATCH.name());
        }

        String accessToken = jwtService.generateToken(user.getId(), true);
        String refreshToken = jwtService.generateToken(user.getId(), false);
        redisService.set("refreshToken:" + request.username(), refreshToken, Duration.ofMillis(refreshExpiration));
        return userMapper.toLoginResponse(user, accessToken);
    }

    @Override
    public String refreshToken(String username) {
        String cachedRefreshToken = redisService.get("refreshToken:" + username, String.class);
        if (cachedRefreshToken == null) {
            throw new IllegalArgumentException(ErrorCode.REFRESH_TOKEN_EXPIRED.name());
        }

        UUID userId = UUID.fromString(jwtService.extractUserId(cachedRefreshToken));
        String newAccessToken = jwtService.generateToken(userId, true);
        return newAccessToken;
    }

}
