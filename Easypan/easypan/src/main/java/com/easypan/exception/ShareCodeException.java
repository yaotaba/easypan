package com.easypan.exception;

public class ShareCodeException extends MyException {
    private static final long serialVersionUID = 1L;

    public ShareCodeException() {

    }

    public ShareCodeException(String message) {
        super(message);
    }

    public ShareCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShareCodeException(Throwable cause) {
        super(cause);
    }

}
