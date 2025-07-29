package io.gaboja9.mockstock.domain.auth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gaboja9.mockstock.global.exception.ErrorCode;
import io.gaboja9.mockstock.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtResponseHandler {

    private final ObjectMapper objectMapper;

    public void handleJwtException(HttpServletResponse response, JwtAuthenticationException e)
            throws IOException {
        sendErrorResponse(response, e.getErrorCode());
    }

    public void handleUnexpectedException(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, ErrorCode.JWT_UNEXPECTED_ERROR);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getMessage(),
                errorCode.getStatus().value()
        );

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
