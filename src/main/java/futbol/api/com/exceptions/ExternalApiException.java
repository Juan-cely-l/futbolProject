package futbol.api.com.exceptions;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {
    private final Integer statusCode;

    public ExternalApiException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
