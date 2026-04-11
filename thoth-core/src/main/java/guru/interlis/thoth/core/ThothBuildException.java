package guru.interlis.thoth.core;

/**
 * Common exception for Thoth build operations.
 * Provides structured error information for both blog and biblios products.
 */
public class ThothBuildException extends RuntimeException {

    private final ErrorSeverity severity;
    private final String component;

    public ThothBuildException(String message) {
        super(message);
        this.severity = ErrorSeverity.ERROR;
        this.component = "general";
    }

    public ThothBuildException(String message, Throwable cause) {
        super(message, cause);
        this.severity = ErrorSeverity.ERROR;
        this.component = "general";
    }

    public ThothBuildException(String message, ErrorSeverity severity, String component) {
        super(message);
        this.severity = severity;
        this.component = component;
    }

    public ErrorSeverity severity() {
        return severity;
    }

    public String component() {
        return component;
    }

    public enum ErrorSeverity {
        WARNING,
        ERROR,
        FATAL
    }
}
