package com.luomiblog.common;

/**
 * 业务异常
 * 携带 HTTP 状态码（默认 400），由 GlobalExceptionHandler 统一转换为响应
 */
public class BusinessException extends RuntimeException {

    private final int statusCode;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
