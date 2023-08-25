package de.rccookie.json;

public class IllegalDefaultValueException extends RuntimeException {
    public IllegalDefaultValueException(String msg) {
        super(msg);
    }

    public IllegalDefaultValueException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
