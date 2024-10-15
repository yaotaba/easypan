package com.easypan.exception;

public class ParamException extends MyException {
    private static final long serialVersionUID = 1L;

    public ParamException() {

    }

    public ParamException(String message) {
        super(message);
    }

    public ParamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParamException(Throwable cause) {
        super(cause);
    }

}
