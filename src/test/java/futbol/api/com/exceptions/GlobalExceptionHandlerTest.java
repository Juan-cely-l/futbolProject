package futbol.api.com.exceptions;

import futbol.api.com.exceptions.ExternalApiException;
import futbol.api.com.exceptions.ResourceAlreadyExistsException;
import futbol.api.com.exceptions.ResourceNotFoundException;
import futbol.api.com.external.dto.SyncInProgressException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @SuppressWarnings("unchecked")
    private void assertStandardErrorResponse(ResponseEntity<?> response,
                                             HttpStatus expectedStatus,
                                             String expectedError,
                                             String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body)
                .containsEntry("status", expectedStatus.value())
                .containsEntry("error", expectedError)
                .containsEntry("message", expectedMessage);
        assertThat(body.get("timestamp")).isInstanceOf(LocalDateTime.class);
    }

    @SuppressWarnings("unchecked")
    private void assertStandardErrorResponseNoMessageLeak(ResponseEntity<?> response,
                                                          HttpStatus expectedStatus,
                                                          String expectedError,
                                                          String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body)
                .containsEntry("status", expectedStatus.value())
                .containsEntry("error", expectedError)
                .containsEntry("message", expectedMessage);
        assertThat(body.get("timestamp")).isInstanceOf(LocalDateTime.class);
    }

    // ----------------------------------------------------------------
    // 404 — ResourceNotFoundException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("ResourceNotFoundException → 404")
    class NotFoundTests {

        @Test
        @DisplayName("should return 404 with exception message")
        void handleNotFound_returns404() {
            String message = "Player with ID 123 not found";
            ResourceNotFoundException exception = new ResourceNotFoundException(message);

            ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

            assertStandardErrorResponse(response, HttpStatus.NOT_FOUND, "Not Found", message);
        }

        @Test
        @DisplayName("should include timestamp in response")
        void handleNotFound_includesTimestamp() {
            ResourceNotFoundException exception = new ResourceNotFoundException("Not found");
            ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

            assertThat(response.getBody()).containsKey("timestamp");
            assertThat(response.getBody().get("timestamp")).isInstanceOf(LocalDateTime.class);
        }
    }

    // ----------------------------------------------------------------
    // 409 — ResourceAlreadyExistsException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("ResourceAlreadyExistsException → 409")
    class AlreadyExistsTests {

        @Test
        @DisplayName("should return 409 with exception message")
        void handleConflict_returns409() {
            String message = "Player 'John' already exists in team 'FC Barcelona'";
            ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException(message);

            ResponseEntity<Map<String, Object>> response = handler.handleConflict(exception);

            assertStandardErrorResponse(response, HttpStatus.CONFLICT, "Conflict", message);
        }
    }

    // ----------------------------------------------------------------
    // 409 — SyncInProgressException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("SyncInProgressException → 409")
    class SyncInProgressTests {

        @Test
        @DisplayName("should return 409 with exception message")
        void handleSyncInProgress_returns409() {
            String message = "A synchronization is already in progress";
            SyncInProgressException exception = new SyncInProgressException(message);

            ResponseEntity<Map<String, Object>> response = handler.handleSyncInProgress(exception);

            assertStandardErrorResponse(response, HttpStatus.CONFLICT, "Conflict", message);
        }
    }

    // ----------------------------------------------------------------
    // 400 — MethodArgumentNotValidException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("MethodArgumentNotValidException → 400 with fieldErrors")
    class ValidationTests {

        @Mock
        private MethodArgumentNotValidException exception;

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("should return 400 with field errors")
        void handleValidation_returns400withFieldErrors() {
            FieldError nameError = new FieldError("playerRequest", "name", "Name must not be blank");
            FieldError ageError = new FieldError("playerRequest", "age", "Age must be between 16 and 45");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(nameError, ageError));
            when(exception.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody())
                    .containsEntry("status", 400)
                    .containsEntry("error", "Validation Failed");
            assertThat(response.getBody().get("timestamp")).isInstanceOf(LocalDateTime.class);
            assertThat(response.getBody()).containsKey("fieldErrors");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> fieldErrors =
                    (List<Map<String, String>>) response.getBody().get("fieldErrors");
            assertThat(fieldErrors).hasSize(2);
            assertThat(fieldErrors.get(0))
                    .containsEntry("field", "name")
                    .containsEntry("message", "Name must not be blank");
            assertThat(fieldErrors.get(1))
                    .containsEntry("field", "age")
                    .containsEntry("message", "Age must be between 16 and 45");
        }

        @Test
        @DisplayName("should use default message when getDefaultMessage is null")
        void handleValidation_nullDefaultMessage_usesFallback() {
            FieldError errorWithNullMessage = new FieldError("playerRequest", "position", null);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(errorWithNullMessage));
            when(exception.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> fieldErrors =
                    (List<Map<String, String>>) response.getBody().get("fieldErrors");
            assertThat(fieldErrors.get(0))
                    .containsEntry("field", "position")
                    .containsEntry("message", "Invalid value");
        }

        @Test
        @DisplayName("should handle empty field errors list")
        void handleValidation_emptyFieldErrors_returnsEmptyList() {
            when(bindingResult.getFieldErrors()).thenReturn(List.of());
            when(exception.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> fieldErrors =
                    (List<Map<String, String>>) response.getBody().get("fieldErrors");
            assertThat(fieldErrors).isEmpty();
        }
    }

    // ----------------------------------------------------------------
    // 400 — HttpMessageNotReadableException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("HttpMessageNotReadableException → 400")
    class MalformedJsonTests {

        @Test
        @DisplayName("should return 400 with hardcoded message")
        void handleMalformedJson_returns400() {
            HttpInputMessage inputMessage = mock(HttpInputMessage.class);
            HttpMessageNotReadableException exception =
                    new HttpMessageNotReadableException("Some internal parsing error", inputMessage);

            ResponseEntity<Map<String, Object>> response = handler.handleMalformedJson(exception);

            assertStandardErrorResponse(response, HttpStatus.BAD_REQUEST, "Bad Request",
                    "Malformed request body. Check JSON syntax and field types.");
        }
    }

    // ----------------------------------------------------------------
    // 400 — MissingServletRequestParameterException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("MissingServletRequestParameterException → 400")
    class MissingParamTests {

        @Test
        @DisplayName("should return 400 with parameter name in message")
        void handleMissingParam_returns400() {
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("teamName", "String");

            ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(exception);

            assertStandardErrorResponse(response, HttpStatus.BAD_REQUEST, "Bad Request",
                    "Required parameter 'teamName' is missing.");
        }
    }

    // ----------------------------------------------------------------
    // 405 — HttpRequestMethodNotSupportedException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("HttpRequestMethodNotSupportedException → 405")
    class MethodNotAllowedTests {

        @Test
        @DisplayName("should return 405 with exception message")
        void handleMethodNotAllowed_returns405() {
            HttpRequestMethodNotSupportedException exception =
                    new HttpRequestMethodNotSupportedException("PATCH");

            ResponseEntity<Map<String, Object>> response = handler.handleMethodNotAllowed(exception);

            assertStandardErrorResponse(response, HttpStatus.METHOD_NOT_ALLOWED,
                    "Method Not Allowed", exception.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // 403 — AccessDeniedException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("AccessDeniedException → 403")
    class AccessDeniedTests {

        @Test
        @DisplayName("should return 403 with hardcoded message")
        void handleAccessDenied_returns403() {
            AccessDeniedException exception = new AccessDeniedException("Real detail message not leaked");

            ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(exception);

            assertStandardErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not have permission to perform this action.");
        }
    }

    // ----------------------------------------------------------------
    // 409 — DataIntegrityViolationException
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("DataIntegrityViolationException → 409")
    class DataIntegrityTests {

        @Test
        @DisplayName("should return 409 with hardcoded message")
        void handleDataIntegrity_returns409() {
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("Real SQL constraint violation");

            ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(exception);

            assertStandardErrorResponse(response, HttpStatus.CONFLICT, "Conflict",
                    "Operation would violate a data constraint.");
        }
    }

    // ----------------------------------------------------------------
    // ExternalApiException — status-dependent
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("ExternalApiException → status-dependent")
    class ExternalApiTests {

        @Test
        @DisplayName("statusCode 429 should map to 429 Too Many Requests")
        void handleExternalApi_429_returns429() {
            ExternalApiException exception = new ExternalApiException(429, "Rate limit exceeded");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody())
                    .containsEntry("status", 429)
                    .containsEntry("error", "Too Many Requests")
                    .containsEntry("message", "Rate limit exceeded");
            assertThat(response.getBody().get("timestamp")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("statusCode >= 500 should map to 502 Bad Gateway")
        void handleExternalApi_500plus_returns502() {
            ExternalApiException exception = new ExternalApiException(502, "External server error");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody())
                    .containsEntry("status", 502)
                    .containsEntry("error", "Bad Gateway");
        }

        @Test
        @DisplayName("statusCode 500 should map to 502 Bad Gateway")
        void handleExternalApi_500_returns502() {
            ExternalApiException exception = new ExternalApiException(500, "Internal server error on external API");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody())
                    .containsEntry("status", 502)
                    .containsEntry("error", "Bad Gateway");
        }

        @Test
        @DisplayName("statusCode in 400-499 (excluding 429) should pass through")
        void handleExternalApi_4xx_returnsSameStatus() {
            ExternalApiException exception = new ExternalApiException(422, "Unprocessable content");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(response.getBody())
                    .containsEntry("status", 422)
                    .containsEntry("error", "Unprocessable Content");
        }

        @Test
        @DisplayName("statusCode in 400-499 includes 400")
        void handleExternalApi_400_returns400() {
            ExternalApiException exception = new ExternalApiException(400, "Bad request");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody())
                    .containsEntry("status", 400)
                    .containsEntry("error", "Bad Request");
        }

        @Test
        @DisplayName("statusCode outside known ranges should map to 503 Service Unavailable")
        void handleExternalApi_other_returns503() {
            ExternalApiException exception = new ExternalApiException(300, "Multiple choices");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody())
                    .containsEntry("status", 503)
                    .containsEntry("error", "Service Unavailable");
        }

        @Test
        @DisplayName("statusCode 200 (success) should map to 503 Service Unavailable")
        void handleExternalApi_200_returns503() {
            ExternalApiException exception = new ExternalApiException(200, "Unexpected success");

            ResponseEntity<Map<String, Object>> response = handler.handleExternalApi(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody())
                    .containsEntry("status", 503)
                    .containsEntry("error", "Service Unavailable");
        }
    }

    // ----------------------------------------------------------------
    // 500 — Generic Exception
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("Generic Exception → 500 without message leak")
    class GeneralExceptionTests {

        @Test
        @DisplayName("should return 500 with generic message, not the real exception message")
        void handleGeneral_doesNotLeakMessage() {
            Exception exception = new RuntimeException("This is a sensitive internal error: DB connection timeout");

            ResponseEntity<Map<String, Object>> response = handler.handleGeneral(exception);

            assertStandardErrorResponseNoMessageLeak(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error", "An unexpected error occurred");
        }

        @Test
        @DisplayName("should always return same generic message regardless of exception type")
        void handleGeneral_messageIsAlwaysGeneric() {
            Exception nullPointer = new NullPointerException("null reference in database query");
            Exception illegalArg = new IllegalArgumentException("invalid argument passed");

            ResponseEntity<Map<String, Object>> response1 = handler.handleGeneral(nullPointer);
            ResponseEntity<Map<String, Object>> response2 = handler.handleGeneral(illegalArg);

            assertThat(response1.getBody())
                    .containsEntry("message", "An unexpected error occurred");
            assertThat(response2.getBody())
                    .containsEntry("message", "An unexpected error occurred");
        }
    }

    // ----------------------------------------------------------------
    // Edge cases across handlers
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("ResourceNotFoundException with empty string message")
        void handleNotFound_emptyMessage() {
            ResourceNotFoundException exception = new ResourceNotFoundException("");

            ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

            assertStandardErrorResponse(response, HttpStatus.NOT_FOUND, "Not Found", "");
        }

        @Test
        @DisplayName("all response bodies contain timestamp")
        void allResponsesContainTimestamp() {
            ResourceNotFoundException ex1 = new ResourceNotFoundException("msg");
            ResponseEntity<Map<String, Object>> r1 = handler.handleNotFound(ex1);
            assertThat(r1.getBody()).containsKey("timestamp");

            ResourceAlreadyExistsException ex2 = new ResourceAlreadyExistsException("msg");
            ResponseEntity<Map<String, Object>> r2 = handler.handleConflict(ex2);
            assertThat(r2.getBody()).containsKey("timestamp");

            SyncInProgressException ex3 = new SyncInProgressException("msg");
            ResponseEntity<Map<String, Object>> r3 = handler.handleSyncInProgress(ex3);
            assertThat(r3.getBody()).containsKey("timestamp");

            ExternalApiException ex4 = new ExternalApiException(429, "msg");
            ResponseEntity<Map<String, Object>> r4 = handler.handleExternalApi(ex4);
            assertThat(r4.getBody()).containsKey("timestamp");

            Exception ex5 = new Exception("msg");
            ResponseEntity<Map<String, Object>> r5 = handler.handleGeneral(ex5);
            assertThat(r5.getBody()).containsKey("timestamp");
        }
    }
}
