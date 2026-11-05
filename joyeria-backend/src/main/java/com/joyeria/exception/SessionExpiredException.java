package com.joyeria.exception;

public class SessionExpiredException extends IllegalArgumentException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
