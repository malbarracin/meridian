package com.meridian.dashboard.security;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.meridian.dashboard.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Autenticación Bearer del dashboard")
class JwtAuthInterceptorTest {

    private final JwtAuthInterceptor interceptor =
            new JwtAuthInterceptor(new JwtValidator(new JwtConfig(JwtValidatorTest.SECRET)));

    @Test
    @DisplayName("Rechaza header ausente, esquema incorrecto y tokens inválidos o expirados")
    void rejectsInvalidCredentials() {
        assertUnauthorized(new MockHttpServletRequest());
        for (String header : new String[]{"Basic abc", "Bearer ", "Bearer invalid",
                "Bearer " + JwtValidatorTest.token(JwtValidatorTest.SECRET, Instant.now().minusSeconds(300))}) {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", header);
            assertUnauthorized(request);
        }
    }

    private void assertUnauthorized(MockHttpServletRequest request) {
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), this))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    @DisplayName("Continúa con un token firmado vigente")
    void acceptsValidToken() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer "
                + JwtValidatorTest.token(JwtValidatorTest.SECRET, Instant.now().plusSeconds(300)));
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), this)).isTrue();
    }
}
