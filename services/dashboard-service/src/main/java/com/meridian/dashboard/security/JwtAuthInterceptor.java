package com.meridian.dashboard.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

import com.meridian.dashboard.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

/** Exige credenciales Bearer antes de ejecutar el dashboard. */
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator validator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || !validator.isValid(authorization.substring(BEARER_PREFIX.length()))) {
            throw new UnauthorizedException();
        }
        return true;
    }
}
