package com.hokhanh.ping_watch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hokhanh.ping_watch.constant.ErrorCode;
import com.hokhanh.ping_watch.mapper.UserMapper;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.UserRepository;
import com.hokhanh.ping_watch.request.ConfirmOtpRequest;
import com.hokhanh.ping_watch.request.RegisterRequest;
import com.hokhanh.ping_watch.response.RegisterResponse;
import com.hokhanh.ping_watch.service.EmailService;
import com.hokhanh.ping_watch.service.RedisService;
import com.hokhanh.ping_watch.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private RedisService redisService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private static final String OTP = "123456";

    @Test
    void register_shouldSuccess_whenValidRequest() {
        RegisterRequest request = new RegisterRequest(
                "Khanh",
                "Ho",
                "khanh123",
                "password",
                "password",
                "khanh@gmail.com",
                null
        );

        doReturn(null).when(userRepository).findByUsername("khanh123");
        doReturn(null).when(userRepository).findByEmail("khanh@gmail.com");

        doNothing().when(redisService).set(anyString(), any(), any());
        doNothing().when(emailService).send(anyString(), anyString(), anyString());

        userService.register(request);

        verify(redisService, times(1))
                .set(startsWith("username:"), any(), any());

        verify(emailService, times(1))
                .send(eq("khanh@gmail.com"), anyString(), contains("Your OTP is"));
    }

    @Test
    void register_shouldThrowException_whenPasswordNotMatch() {
        RegisterRequest request = new RegisterRequest(
                "Khanh",
                "Ho",
                "khanh123",
                "password1",
                "password2",
                "khanh@gmail.com",
                null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals(ErrorCode.PASSWORD_NOT_MATCH.name(), ex.getMessage());
    }

    @Test
    void confirmOtp_shouldSuccess_whenValidOtp() {
        ConfirmOtpRequest request = new ConfirmOtpRequest("khanh123", OTP);

        RegisterRequest cached = new RegisterRequest(
                "Khanh",
                "Ho",
                "khanh123",
                "password",
                "password",
                "khanh@gmail.com",
                OTP
        );

        User user = new User();
        RegisterResponse response = new RegisterResponse();

        when(redisService.get(anyString(), eq(RegisterRequest.class))).thenReturn(cached);
        doNothing().when(redisService).delete(anyString());

        when(userRepository.findByUsername("khanh123")).thenReturn(null);
        when(userRepository.findByEmail("khanh@gmail.com")).thenReturn(null);

        when(userMapper.toUser(cached)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toRegisterResponse(user)).thenReturn(response);

        RegisterResponse result = userService.confirmOtp(request);

        assertNotNull(result);

        verify(redisService).delete(anyString());
        verify(userRepository).save(user);
    }

    @Test
    void confirmOtp_shouldThrow_whenOtpExpired() {
        ConfirmOtpRequest request = new ConfirmOtpRequest("khanh123", OTP);

        when(redisService.get(anyString(), eq(RegisterRequest.class)))
                .thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.confirmOtp(request)
        );

        assertEquals(ErrorCode.OTP_EXPIRED.name(), ex.getMessage());
    }

    @Test
    void confirmOtp_shouldThrow_whenOtpInvalid() {
        ConfirmOtpRequest request = new ConfirmOtpRequest("khanh123", "wrong");

        RegisterRequest cached = new RegisterRequest(
                "Khanh",
                "Ho",
                "khanh123",
                "password",
                "password",
                "khanh@gmail.com",
                OTP
        );

        when(redisService.get(anyString(), eq(RegisterRequest.class)))
                .thenReturn(cached);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.confirmOtp(request)
        );

        assertEquals(ErrorCode.OTP_INVALID.name(), ex.getMessage());
    }
}