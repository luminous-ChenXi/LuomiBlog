package com.luomiblog.common.exception;

public class AuthenticationException extends BusinessException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public AuthenticationException(int code, String message) {
        super(code, message);
    }
}
