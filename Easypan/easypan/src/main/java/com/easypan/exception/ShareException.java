package com.easypan.exception;

public class ShareException extends MyException {
    private static final long serialVersionUID = 1L;

    public ShareException() {

    }

    public ShareException(String message) {
        super(message);
    }

    public ShareException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShareException(Throwable cause) {
        super(cause);
    }

}
