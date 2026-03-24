package com.hokhanh.ping_watch.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hokhanh.ping_watch.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;
        final String path = request.getRequestURI();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No JWT token found for request: {} {}", request.getMethod(), path);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userId = jwtService.extractUserId(jwt);

        if (userId == null) {
            log.info("JWT does not contain valid userId. path={}", path);
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(jwt)) {
            log.info("Expired JWT for userId={}, path={}", userId, path);
            filterChain.doFilter(request, response);
            return;
        }

        boolean isRefreshTokenType = jwtService.isRefreshTokenType(jwt);
        if (isRefreshTokenType != path.equals("/users/refresh-token")) {
            String message = isRefreshTokenType
                    ? "Refresh token used on invalid endpoint"
                    : "Access token used on refresh endpoint";

            log.info(
                    "{}. userId={}, path={}, method={}, ip={}",
                    message,
                    userId,
                    path,
                    request.getMethod(),
                    request.getRemoteAddr());

            filterChain.doFilter(request, response);
            return;
        }

        if (isRefreshTokenType) {
            request.setAttribute("refreshToken", jwt);
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.info("JWT authentication success. userId={}, path={}", userId, path);

        filterChain.doFilter(request, response);
    }
}
