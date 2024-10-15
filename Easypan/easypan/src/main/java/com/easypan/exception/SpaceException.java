package com.easypan.exception;

public class SpaceException extends MyException {
    private static final long serialVersionUID = 1L;

    public SpaceException() {

    }

    public SpaceException(String message) {
        super(message);
    }

    public SpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpaceException(Throwable cause) {
        super(cause);
    }

}
