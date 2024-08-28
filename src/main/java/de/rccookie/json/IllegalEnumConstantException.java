package de.rccookie.json;

public class IllegalEnumConstantException extends JsonDeserializationException {

    public IllegalEnumConstantException() {
        super();
    }

    public IllegalEnumConstantException(String message) {
        super(message);
    }

    public IllegalEnumConstantException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalEnumConstantException(Throwable cause) {
        super(cause);
    }

    public IllegalEnumConstantException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
