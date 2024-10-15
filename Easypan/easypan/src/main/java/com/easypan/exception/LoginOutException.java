package com.easypan.exception;

public class LoginOutException extends MyException {
    private static final long serialVersionUID = 1L;

    public LoginOutException() {

    }

    public LoginOutException(String message) {
        super(message);
    }

    public LoginOutException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoginOutException(Throwable cause) {
        super(cause);
    }

}
