package com.example.sms.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockingFailure_returnsConflict() {
        ResponseEntity<Map<String, Object>> response = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("stale update"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("status", 409)
                .containsEntry("error", "Conflict")
                .containsEntry("message",
                        "This record was changed by another request. Refresh the data and try again.");
    }

    @Test
    void dataIntegrityViolation_returnsConflict() {
        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("constraint violation"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("status", 409)
                .containsEntry("error", "Conflict")
                .containsEntry("message",
                        "The request conflicts with data that was changed by another request.");
    }

    @Test
    void malformedJson_returnsStandardBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleMalformedJson(
                new HttpMessageNotReadableException("invalid JSON", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("message", "Request body contains invalid JSON.");
    }

    @Test
    void unsupportedMethod_returnsStandardMethodNotAllowed() {
        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("GET"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody())
                .containsEntry("status", 405)
                .containsEntry("message", "HTTP method GET is not supported for this endpoint.");
    }

    @Test
    void unknownEndpoint_returnsStandardNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "No endpoint found for this request.");
    }
}
