package futbol.api.com.external.dto;

public class SyncAdmissionRejectedException extends RuntimeException {
    public SyncAdmissionRejectedException(String message) {
        super(message);
    }

    public SyncAdmissionRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
