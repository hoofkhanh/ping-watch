package com.hokhanh.ping_watch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.hokhanh.ping_watch.constant.ErrorCode;
import com.hokhanh.ping_watch.mapper.MonitoringConfigurationMapper;
import com.hokhanh.ping_watch.model.HttpMethod;
import com.hokhanh.ping_watch.model.MonitoringConfiguration;
import com.hokhanh.ping_watch.model.User;
import com.hokhanh.ping_watch.repository.MonitoringConfigurationRepository;
import com.hokhanh.ping_watch.repository.UserRepository;
import com.hokhanh.ping_watch.request.AddMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.GetAllMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.request.UpdateMonitoringConfigurationRequest;
import com.hokhanh.ping_watch.response.AddMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.DeleteMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetAllMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.GetMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StartMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.StopMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.response.UpdateMonitoringConfigurationResponse;
import com.hokhanh.ping_watch.service.impl.MonitoringConfigurationServiceImpl;
import com.hokhanh.ping_watch.service.scheduler.MonitoringRunStateService;

@ExtendWith(MockitoExtension.class)
class MonitoringConfigurationServiceImplTest {

        @InjectMocks
        private MonitoringConfigurationServiceImpl monitoringConfigurationService;

        @Mock
        private MonitoringConfigurationRepository monitoringConfigurationRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private MonitoringConfigurationMapper monitoringConfigurationMapper;

        @Mock
        private MonitoringRunStateService monitoringRunStateService;

        private UUID userId;
        private String userIdString;
        private UUID configurationId;

        private User user;
        private MonitoringConfiguration monitoringConfiguration;

        private AddMonitoringConfigurationRequest addRequest;
        private UpdateMonitoringConfigurationRequest updateRequest;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                userIdString = userId.toString();
                configurationId = UUID.randomUUID();

                user = new User(userId, "Khanh", "Ho", "khanh123", "password", "khanh@gmail.com");

                monitoringConfiguration = MonitoringConfiguration.builder()
                                .id(configurationId)
                                .name("Google Monitor")
                                .httpMethod(HttpMethod.GET)
                                .url("https://www.google.com")
                                .interval(10.0)
                                .timeout(5.0)
                                .user(user)
                                .build();

                addRequest = new AddMonitoringConfigurationRequest(
                                "Google Monitor",
                                HttpMethod.GET,
                                "https://www.google.com",
                                10.0,
                                5.0);

                updateRequest = new UpdateMonitoringConfigurationRequest(
                                "Google Monitor Updated",
                                HttpMethod.GET,
                                "https://www.google.com/health",
                                15.0,
                                6.0);
        }

        @Test
        void add_shouldSuccess_whenValidRequest() {
                AddMonitoringConfigurationResponse expected = new AddMonitoringConfigurationResponse(
                                configurationId,
                                "Google Monitor",
                                HttpMethod.GET,
                                "https://www.google.com",
                                10.0,
                                5.0);

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(monitoringConfigurationMapper.toEntity(addRequest, user)).thenReturn(monitoringConfiguration);
                when(monitoringConfigurationRepository.save(monitoringConfiguration))
                                .thenReturn(monitoringConfiguration);
                when(monitoringConfigurationMapper.toAddResponse(monitoringConfiguration)).thenReturn(expected);

                AddMonitoringConfigurationResponse result = monitoringConfigurationService.add(addRequest,
                                userIdString);

                assertNotNull(result);
                assertEquals(expected, result);
                verify(userRepository).findById(userId);
                verify(monitoringConfigurationRepository).save(monitoringConfiguration);
        }

        @Test
        void add_shouldThrow_whenInvalidUserId() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.add(addRequest, "invalid-uuid"));

                assertEquals(ErrorCode.INVALID_USER_ID.name(), ex.getMessage());
        }

        @Test
        void add_shouldThrow_whenUserNotFound() {
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.add(addRequest, userIdString));

                assertEquals(ErrorCode.USER_NOT_FOUND.name(), ex.getMessage());
        }

        @Test
        void update_shouldSuccess_whenConfigurationBelongsToUser() {
                UpdateMonitoringConfigurationResponse expected = new UpdateMonitoringConfigurationResponse(
                                configurationId,
                                "Google Monitor Updated",
                                HttpMethod.GET,
                                "https://www.google.com/health",
                                15.0,
                                6.0);

                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.of(monitoringConfiguration));
                when(monitoringConfigurationRepository.save(monitoringConfiguration))
                                .thenReturn(monitoringConfiguration);
                when(monitoringConfigurationMapper.toUpdateResponse(monitoringConfiguration)).thenReturn(expected);

                UpdateMonitoringConfigurationResponse result = monitoringConfigurationService.update(
                                configurationId,
                                updateRequest,
                                userIdString);

                assertNotNull(result);
                assertEquals(expected, result);
                verify(monitoringConfigurationMapper).updateEntity(monitoringConfiguration, updateRequest);
                verify(monitoringConfigurationRepository).save(monitoringConfiguration);
        }

        @Test
        void update_shouldThrow_whenConfigurationNotFound() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.update(configurationId, updateRequest,
                                                userIdString));

                assertEquals(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name(), ex.getMessage());
        }

        @Test
        void delete_shouldSuccess_whenConfigurationBelongsToUser() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.of(monitoringConfiguration));

                DeleteMonitoringConfigurationResponse result = monitoringConfigurationService.delete(configurationId,
                                userIdString);

                assertNotNull(result);
                assertEquals(configurationId, result.id());
                verify(monitoringConfigurationRepository).delete(monitoringConfiguration);
                verify(monitoringRunStateService).stop(configurationId);
        }

        @Test
        void delete_shouldThrow_whenConfigurationNotFound() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.delete(configurationId, userIdString));

                assertEquals(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name(), ex.getMessage());
                verify(monitoringConfigurationRepository, never()).delete(any());
                verify(monitoringRunStateService, never()).stop(any());
        }

        @Test
        void getAll_shouldSuccess_whenValidUserId() {
                GetAllMonitoringConfigurationRequest request = new GetAllMonitoringConfigurationRequest(1, 2);

                List<MonitoringConfiguration> configurations = List.of(monitoringConfiguration);
                Page<MonitoringConfiguration> page = new PageImpl<>(configurations, Pageable.ofSize(2).withPage(1), 5);

                GetAllMonitoringConfigurationResponse expected = new GetAllMonitoringConfigurationResponse(List.of(), 1,
                                2, 3, 5);

                when(monitoringConfigurationRepository.findAllByUser_Id(eq(userId), any(Pageable.class)))
                                .thenReturn(page);
                when(monitoringConfigurationMapper.toGetAllResponse(configurations, 1, 2, 3, 5)).thenReturn(expected);

                GetAllMonitoringConfigurationResponse result = monitoringConfigurationService.getAll(request,
                                userIdString);

                assertNotNull(result);
                assertEquals(expected, result);
                verify(monitoringConfigurationRepository).findAllByUser_Id(eq(userId), any(Pageable.class));
        }

        @Test
        void getAll_shouldUseDefaultPaging_whenPageAndSizeNull() {
                GetAllMonitoringConfigurationRequest request = new GetAllMonitoringConfigurationRequest(null, null);

                List<MonitoringConfiguration> configurations = List.of();
                Page<MonitoringConfiguration> page = new PageImpl<>(configurations, Pageable.ofSize(10).withPage(0), 0);

                GetAllMonitoringConfigurationResponse expected = new GetAllMonitoringConfigurationResponse(List.of(), 0,
                                10, 0, 0);

                when(monitoringConfigurationRepository.findAllByUser_Id(eq(userId), any(Pageable.class)))
                                .thenReturn(page);
                when(monitoringConfigurationMapper.toGetAllResponse(configurations, 0, 10, 0, 0)).thenReturn(expected);

                GetAllMonitoringConfigurationResponse result = monitoringConfigurationService.getAll(request,
                                userIdString);

                assertEquals(expected, result);
        }

        @Test
        void getById_shouldSuccess_whenConfigurationBelongsToUser() {
                GetMonitoringConfigurationResponse expected = new GetMonitoringConfigurationResponse(
                                configurationId,
                                "Google Monitor",
                                HttpMethod.GET,
                                "https://www.google.com",
                                10.0,
                                5.0);

                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.of(monitoringConfiguration));
                when(monitoringConfigurationMapper.toGetByIdResponse(monitoringConfiguration)).thenReturn(expected);

                GetMonitoringConfigurationResponse result = monitoringConfigurationService.getById(configurationId,
                                userIdString);

                assertNotNull(result);
                assertEquals(expected, result);
        }

        @Test
        void getById_shouldThrow_whenConfigurationNotFound() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.getById(configurationId, userIdString));

                assertEquals(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name(), ex.getMessage());
        }

        @Test
        void start_shouldSuccess_whenConfigurationBelongsToUser() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.of(monitoringConfiguration));

                StartMonitoringConfigurationResponse result = monitoringConfigurationService.start(configurationId,
                                userIdString);

                assertNotNull(result);
                assertEquals(configurationId, result.id());
                assertEquals("STARTED", result.status());
                verify(monitoringRunStateService, times(1)).start(configurationId);
        }

        @Test
        void stop_shouldSuccess_whenConfigurationBelongsToUser() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.of(monitoringConfiguration));

                StopMonitoringConfigurationResponse result = monitoringConfigurationService.stop(configurationId,
                                userIdString);

                assertNotNull(result);
                assertEquals(configurationId, result.id());
                assertEquals("STOPPED", result.status());
                verify(monitoringRunStateService, times(1)).stop(configurationId);
        }

        @Test
        void start_shouldThrow_whenConfigurationNotFound() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.start(configurationId, userIdString));

                assertEquals(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name(), ex.getMessage());
        }

        @Test
        void stop_shouldThrow_whenConfigurationNotFound() {
                when(monitoringConfigurationRepository.findByIdAndUser_Id(configurationId, userId))
                                .thenReturn(Optional.empty());

                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringConfigurationService.stop(configurationId, userIdString));

                assertEquals(ErrorCode.MONITORING_CONFIGURATION_NOT_FOUND.name(), ex.getMessage());
        }
}
