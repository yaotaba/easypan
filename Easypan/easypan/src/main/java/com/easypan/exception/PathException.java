package com.easypan.exception;

public class PathException extends MyException {
    private static final long serialVersionUID = 1L;

    public PathException() {

    }

    public PathException(String message) {
        super(message);
    }

    public PathException(String message, Throwable cause) {
        super(message, cause);
    }

    public PathException(Throwable cause) {
        super(cause);
    }

}
