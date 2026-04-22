package com.luomiblog.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ApiResponse.error(400, "参数校验失败: " + errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException(BadCredentialsException ex) {
        return ApiResponse.error(401, "用户名或密码错误");
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleLockedException(LockedException ex) {
        return ApiResponse.error(403, "账号已被锁定，请稍后重试");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleDisabledException(DisabledException ex) {
        return ApiResponse.error(403, "账号已被禁用");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("访问被拒绝: {}", ex.getMessage());
        return ApiResponse.error(403, "权限不足，无法访问此资源");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && (
                ex.getMessage().contains("用户名或密码错误") ||
                ex.getMessage().contains("账号已被") ||
                ex.getMessage().contains("锁定") ||
                ex.getMessage().contains("密码强度") ||
                ex.getMessage().contains("已存在") ||
                ex.getMessage().contains("已注册") ||
                ex.getMessage().contains("频繁") ||
                ex.getMessage().contains("无效的token"))) {
            return ApiResponse.error(400, ex.getMessage());
        }
        log.error("运行时异常: ", ex);
        return ApiResponse.error(500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGlobalException(Exception ex) {
        log.error("未预期的异常: ", ex);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
