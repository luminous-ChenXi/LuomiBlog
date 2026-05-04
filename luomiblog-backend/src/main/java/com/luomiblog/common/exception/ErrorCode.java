package com.luomiblog.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    VALIDATION_FAILED(4001, "参数校验失败"),
    UNAUTHORIZED(401, "未认证"),
    TOKEN_EXPIRED(4011, "Token已过期"),
    TOKEN_INVALID(4012, "Token无效"),
    ACCESS_DENIED(403, "权限不足"),
    FORBIDDEN(4031, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    USER_NOT_FOUND(4100, "用户不存在"),
    USER_ALREADY_EXISTS(4101, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(4102, "邮箱已被注册"),
    PASSWORD_TOO_WEAK(4103, "密码强度不足"),
    PASSWORD_MISMATCH(4104, "两次输入的密码不一致"),
    OLD_PASSWORD_WRONG(4105, "旧密码错误"),
    ACCOUNT_BANNED(4106, "账号已被封禁"),
    ACCOUNT_INACTIVE(4107, "账号未激活"),
    ACCOUNT_LOCKED(4108, "账号已锁定"),
    LOGIN_TOO_FREQUENT(4109, "登录过于频繁"),
    EMAIL_NOT_VERIFIED(4110, "邮箱未验证"),
    REGISTRATION_DISABLED(4111, "注册功能已关闭"),

    ROLE_NOT_FOUND(4200, "角色不存在"),
    ROLE_ALREADY_EXISTS(4201, "角色已存在"),
    CANNOT_CHANGE_OWN_ROLE(4202, "不能修改自己的角色"),
    CANNOT_BAN_ADMIN(4203, "不能封禁管理员账号"),
    CANNOT_DELETE_ADMIN(4204, "不能删除管理员账号"),
    CANNOT_MODIFY_SELF_STATUS(4205, "不能修改自己的状态"),

    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
