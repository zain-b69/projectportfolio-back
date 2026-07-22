package ma.onee.dsi.projectportfolio.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ma.onee.dsi.projectportfolio.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthenticationReturnsUnauthorizedForBadCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");

        ResponseEntity<ApiErrorResponse> response = handler.handleAuthentication(
                new BadCredentialsException("Bad credentials"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().getMessage()).isEqualTo("Identifiants invalides");
        assertThat(response.getBody().getPath()).isEqualTo("/auth/login");
    }
}
