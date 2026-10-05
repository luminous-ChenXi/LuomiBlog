package com.luomiblog.common.exception;

import com.luomiblog.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus httpStatus = mapCodeToHttpStatus(e.getCode());
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, e.getMessage()));
    }

    @ExceptionHandler(com.luomiblog.common.exception.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomAuthenticationException(
            com.luomiblog.common.exception.AuthenticationException e) {
        log.warn("自定义认证异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 兼容辰汐通行证/授权 SDK 使用的旧版业务异常（携带 HTTP 状态码）
     */
    @ExceptionHandler(com.luomiblog.common.BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleLegacyBusinessException(com.luomiblog.common.BusinessException e) {
        log.warn("业务异常(legacy): status={}, message={}", e.getStatusCode(), e.getMessage());
        HttpStatus httpStatus = HttpStatus.resolve(e.getStatusCode());
        if (httpStatus == null) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(e.getStatusCode(), e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "权限不足"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("凭证错误: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            // [BUG-061 修复] 类级别约束是 ObjectError 而非 FieldError，直接强转会 ClassCastException → 500
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(4001, "参数校验失败", errors));
    }

    /**
     * [BUG-061 修复] 请求体不可读（坏 JSON/编码错误）应 400 而非落兜底 500
     * （集成冒烟实测：GBK 编码中文打进来曾返回 500）
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "请求体格式错误，请检查 JSON 与编码（需 UTF-8）"));
    }

    /** [BUG-061 修复] 路径/参数类型不匹配应 400 */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数类型不正确: " + e.getName()));
    }

    /** 上传超限应 413 而非 500 */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("上传文件超限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(413, "上传文件过大"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("未处理异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }

    private HttpStatus mapCodeToHttpStatus(int code) {
        if (code >= 200 && code < 300) return HttpStatus.OK;
        if (code == 400) return HttpStatus.BAD_REQUEST;
        if (code == 401) return HttpStatus.UNAUTHORIZED;
        if (code == 403) return HttpStatus.FORBIDDEN;
        if (code == 404) return HttpStatus.NOT_FOUND;
        if (code == 409) return HttpStatus.CONFLICT;
        if (code == 429) return HttpStatus.TOO_MANY_REQUESTS;
        if (code == 503) return HttpStatus.SERVICE_UNAVAILABLE;
        if (code >= 4000 && code < 4100) return HttpStatus.BAD_REQUEST;
        if (code >= 4100 && code < 4200) return HttpStatus.UNAUTHORIZED;
        if (code >= 4200 && code < 4300) return HttpStatus.FORBIDDEN;
        if (code >= 5000) return HttpStatus.INTERNAL_SERVER_ERROR;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
