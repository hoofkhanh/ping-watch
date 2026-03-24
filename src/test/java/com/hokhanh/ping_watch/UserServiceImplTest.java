package com.hokhanh.ping_watch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.hokhanh.ping_watch.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
        // Mock dependencies and inject into UserServiceImpl
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

        @Mock
        private JwtService jwtService;

        private static final String OTP = "123456";
        private static final String FIRST_NAME = "Khanh";
        private static final String LAST_NAME = "Ho";
        private static final String USERNAME = "khanh123";
        private static final String EMAIL = "khanh@gmail.com";
        private static final String PASSWORD = "password";
        private static final long REFRESH_EXPIRATION_MS = 86400000L;

        private RegisterRequest cachedRegister;
        private ConfirmOtpRequest confirmOtpRequest;
        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
                cachedRegister = new RegisterRequest(
                                FIRST_NAME,
                                LAST_NAME,
                                USERNAME,
                                PASSWORD,
                                PASSWORD,
                                EMAIL,
                                OTP);

                confirmOtpRequest = new ConfirmOtpRequest(OTP, USERNAME);

                ReflectionTestUtils.setField(userService, "refreshExpiration", REFRESH_EXPIRATION_MS);

                loginRequest = new LoginRequest(USERNAME, PASSWORD);
        }

        // Register tests
        @Test
        void register_shouldSuccess_whenValidRequest() {
                RegisterRequest registerRequest = new RegisterRequest(
                                FIRST_NAME,
                                LAST_NAME,
                                USERNAME,
                                PASSWORD,
                                PASSWORD,
                                EMAIL,
                                null);
                doReturn(null).when(userRepository).findByUsername("khanh123");
                doReturn(null).when(userRepository).findByEmail("khanh@gmail.com");

                doNothing().when(redisService).set(anyString(), any(), any());
                doNothing().when(emailService).send(anyString(), anyString(), anyString());

                userService.register(registerRequest);

                verify(redisService, times(1))
                                .set(startsWith("username:"), any(), any());

                verify(emailService, times(1))
                                .send(eq("khanh@gmail.com"), anyString(), contains("Your OTP is"));
        }

        @Test
        void register_shouldThrowException_whenPasswordNotMatch() {
                RegisterRequest invalidRegisterRequest = new RegisterRequest(
                                FIRST_NAME,
                                LAST_NAME,
                                USERNAME,
                                PASSWORD,
                                "differentPassword",
                                EMAIL,
                                null);
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.register(invalidRegisterRequest));

                assertEquals(ErrorCode.PASSWORD_NOT_MATCH.name(), ex.getMessage());
        }

        @Test
        void confirmOtp_shouldSuccess_whenValidOtp() {
                User user = new User(
                                null,
                                FIRST_NAME,
                                LAST_NAME,
                                USERNAME,
                                PASSWORD,
                                EMAIL);

                User savedUser = new User(
                                UUID.randomUUID(),
                                FIRST_NAME,
                                LAST_NAME,
                                USERNAME,
                                PASSWORD,
                                EMAIL);

                RegisterResponse response = new RegisterResponse(
                                savedUser.getId(),
                                savedUser.getFirstName(),
                                savedUser.getLastName(),
                                savedUser.getUsername(),
                                savedUser.getEmail());

                when(redisService.get(eq("username:khanh123"), eq(RegisterRequest.class)))
                                .thenReturn(cachedRegister);

                when(userRepository.findByUsername("khanh123")).thenReturn(null);
                when(userRepository.findByEmail("khanh@gmail.com")).thenReturn(null);

                doNothing().when(redisService).delete("username:khanh123");

                when(userMapper.toUser(cachedRegister)).thenReturn(user);
                when(userRepository.save(any(User.class))).thenReturn(savedUser);
                when(userMapper.toRegisterResponse(savedUser)).thenReturn(response);

                RegisterResponse result = userService.confirmOtp(confirmOtpRequest);

                assertNotNull(result);

                verify(redisService).delete("username:khanh123");
                verify(userRepository).save(any(User.class));
        }

        @Test
        void confirmOtp_shouldThrow_whenOtpExpired() {
                when(redisService.get(anyString(), eq(RegisterRequest.class)))
                                .thenReturn(null);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.confirmOtp(confirmOtpRequest));

                assertEquals(ErrorCode.OTP_EXPIRED.name(), ex.getMessage());
        }

        @Test
        void confirmOtp_shouldThrow_whenOtpInvalid() {
                ConfirmOtpRequest invalidOtpRequest = new ConfirmOtpRequest("654321", USERNAME);
                when(redisService.get(anyString(), eq(RegisterRequest.class)))
                                .thenReturn(cachedRegister);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.confirmOtp(invalidOtpRequest));

                assertEquals(ErrorCode.OTP_INVALID.name(), ex.getMessage());
        }

        // Login tests
        @Test
        void login_shouldSuccess_whenValidCredentials() {
                UUID userId = UUID.randomUUID();
                User user = new User(userId, FIRST_NAME, LAST_NAME, USERNAME, PASSWORD, EMAIL);
                String accessToken = "access-token";
                String refreshToken = "refresh-token";
                LoginResponse response = new LoginResponse(userId, FIRST_NAME, LAST_NAME, USERNAME, EMAIL, accessToken,
                                refreshToken);

                when(userRepository.findByUsername(USERNAME)).thenReturn(user);
                when(jwtService.generateToken(userId, true)).thenReturn(accessToken);
                when(jwtService.generateToken(userId, false)).thenReturn(refreshToken);
                when(userMapper.toLoginResponse(user, accessToken, refreshToken)).thenReturn(response);

                LoginResponse result = userService.login(loginRequest);

                assertNotNull(result);
                assertEquals(response, result);
                verify(redisService).set(
                                eq("refreshToken:" + USERNAME),
                                eq(refreshToken),
                                eq(Duration.ofMillis(REFRESH_EXPIRATION_MS)));
                verify(userMapper).toLoginResponse(user, accessToken, refreshToken);
        }

        @Test
        void login_shouldThrow_whenUsernameNotFound() {
                when(userRepository.findByUsername(USERNAME)).thenReturn(null);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.login(loginRequest));

                assertEquals(ErrorCode.USERNAME_NOT_FOUND.name(), ex.getMessage());
        }

        @Test
        void login_shouldThrow_whenPasswordNotMatch() {
                LoginRequest loginRequest = new LoginRequest(USERNAME, "wrong-password");
                User user = new User(UUID.randomUUID(), FIRST_NAME, LAST_NAME, USERNAME, PASSWORD, EMAIL);
                when(userRepository.findByUsername(USERNAME)).thenReturn(user);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.login(loginRequest));

                assertEquals(ErrorCode.PASSWORD_NOT_MATCH.name(), ex.getMessage());
        }

        // Refresh token tests
        @Test
        void refreshToken_shouldSuccess_whenRefreshTokenExists() {
                UUID userId = UUID.randomUUID();
                String cachedRefreshToken = "cached-refresh-token";
                String providedRefreshToken = "cached-refresh-token";
                String newAccessToken = "new-access-token";

                when(redisService.get("refreshToken:" + USERNAME, String.class)).thenReturn(cachedRefreshToken);
                when(jwtService.extractUserId(cachedRefreshToken)).thenReturn(userId.toString());
                when(jwtService.generateToken(userId, true)).thenReturn(newAccessToken);

                String result = userService.refreshToken(USERNAME, providedRefreshToken);

                assertEquals(newAccessToken, result);
                verify(jwtService).extractUserId(cachedRefreshToken);
                verify(jwtService).generateToken(userId, true);
        }

        @Test
        void refreshToken_shouldThrow_whenRefreshTokenExpired() {
                when(redisService.get("refreshToken:" + USERNAME, String.class)).thenReturn(null);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.refreshToken(USERNAME, "any-refresh-token"));

                assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED.name(), ex.getMessage());
        }

        @Test
        void refreshToken_shouldThrow_whenRefreshTokenInvalid() {
                String cachedRefreshToken = "cached-refresh-token";
                String providedRefreshToken = "invalid-refresh-token";

                when(redisService.get("refreshToken:" + USERNAME, String.class)).thenReturn(cachedRefreshToken);

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> userService.refreshToken(USERNAME, providedRefreshToken));

                assertEquals(ErrorCode.REFRESH_TOKEN_INVALID.name(), ex.getMessage());
        }

}
