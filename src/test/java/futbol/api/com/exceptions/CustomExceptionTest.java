package futbol.api.com.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Custom Exception Tests")
class CustomExceptionTest {

    @Test
    @DisplayName("ResourceNotFoundException: single message constructor")
    void resourceNotFoundMessage() {
        var ex = new ResourceNotFoundException("Team not found");
        assertThat(ex.getMessage()).isEqualTo("Team not found");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("ResourceNotFoundException: message and cause constructor")
    void resourceNotFoundMessageAndCause() {
        var cause = new RuntimeException("db error");
        var ex = new ResourceNotFoundException("Team not found", cause);
        assertThat(ex.getMessage()).isEqualTo("Team not found");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("ResourceAlreadyExistsException: single message constructor")
    void alreadyExistsMessage() {
        var ex = new ResourceAlreadyExistsException("Team already exists");
        assertThat(ex.getMessage()).isEqualTo("Team already exists");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("ResourceAlreadyExistsException: message and cause constructor")
    void alreadyExistsMessageAndCause() {
        var cause = new RuntimeException("constraint violation");
        var ex = new ResourceAlreadyExistsException("Team already exists", cause);
        assertThat(ex.getMessage()).isEqualTo("Team already exists");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
