package futbol.api.com.external.dto;

public class SyncInProgressException extends RuntimeException {
    public SyncInProgressException(String message) {
        super(message);
    }
}
