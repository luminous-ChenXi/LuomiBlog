package com.luomiblog.common.exception;

/**
 * 完成安装前置校验失败异常：安装流程未走到可完成状态
 * （数据库未初始化或管理员账号未创建），拒绝落 install.lock。
 *
 * <p>控制器捕获后统一返回 HTTP 409，并携带缺失步骤描述。</p>
 */
public class InstallNotReadyException extends RuntimeException {

    public InstallNotReadyException(String message) {
        super(message);
    }
}
