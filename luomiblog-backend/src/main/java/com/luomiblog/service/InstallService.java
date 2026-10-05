package com.luomiblog.service;

import com.luomiblog.dto.install.*;

public interface InstallService {

    InstallStatusResponse getInstallStatus();

    EnvironmentCheckResponse checkEnvironment();

    boolean testDatabaseConnection(DatabaseConfigRequest request);

    /**
     * 富信息测试连接：成功返回 MySQL 版本与字符集，失败返回具体原因分类
     * （连接拒绝 / 认证失败 / 库不存在 / 版本过低）
     */
    DatabaseTestResponse testDatabaseDetailed(DatabaseConfigRequest request);

    /**
     * 库不存在时尝试创建数据库（需要建库权限，CREATE DATABASE IF NOT EXISTS）。
     *
     * @return 创建结果信息
     */
    String createDatabase(DatabaseConfigRequest request);

    DatabaseCheckResponse checkDatabase(DatabaseConfigRequest request);

    void executeSqlScripts(DatabaseConfigRequest request);

    void createAdminAccount(AdminAccountRequest request);

    void saveSiteConfig(SiteConfigRequest request);

    /**
     * 完成安装前置校验：装库已完成（users 表存在）且管理员账号已创建（users 表存在 ADMIN）。
     *
     * @return 缺失步骤的描述；null 表示允许完成安装
     */
    String checkReadyForCompletion();

    /**
     * 当前是否允许调用 reset-install-state：
     * 未锁定（未完成/半安装）时允许；异常锁死态（install.lock 存在但 users 表无任何
     * ADMIN 账号，无法走 verify-reinstall 恢复）也允许，让正常安装能重来；
     * 正常已安装系统（锁定且存在管理员）不允许。
     */
    boolean canResetInstallState();

    void completeInstallation();

    boolean verifyReinstallPermission(String verificationPassword);

    void resetInstallation();

    /**
     * 重置安装状态（仅未完成/半安装状态可调用）：
     * 对齐 scripts/reset-install.ps1 的逻辑，删除安装锁残留、自定义配置与半安装标记。
     */
    void resetInstallState();

    void executeReinstall(ReinstallOption option, DatabaseConfigRequest request);

    boolean needsReinstallOptions();

    boolean verifyLockIntegrity();

    String getLockHash();

    /**
     * 重新安装验证的限流（按 IP，5 次/分钟）
     * @param clientIp 客户端 IP
     * @return 是否允许本次尝试
     */
    boolean tryAcquireReinstallVerifyAttempt(String clientIp);

    /**
     * 重新安装验证是否已被锁定（连续失败过多次）
     * @param clientIp 客户端 IP
     * @return 是否锁定中
     */
    boolean isReinstallVerifyLocked(String clientIp);

    /**
     * 记录一次重新安装验证失败（达到阈值后锁定一段时间）
     * @param clientIp 客户端 IP
     */
    void recordReinstallVerifyFailure(String clientIp);

    /**
     * [BUG-008 TOCTOU 修复] 开启重装窗口：
     * 写入重装标记文件（含时间戳与一次性安装票据的 SHA-256），随后重置安装状态。
     * 标记存在期间，所有安装写操作必须携带有效票据头 X-Lcx-Install-Ticket，
     * 关闭"验证通过即删锁"造成的匿名危险窗口。
     *
     * @return 一次性安装票据（仅此一次返回给已通过 ADMIN 密码验证的调用方）
     */
    String beginReinstall();

    /**
     * 重装窗口是否开启（标记文件存在即视为开启，含已过期——过期后所有写操作被拒绝，fail-closed）
     */
    boolean isReinstallWindowOpen();

    /**
     * 校验安装票据：非空、未过期、SHA-256 与标记文件常量时间比对
     */
    boolean isInstallTicketValid(String ticket);
}
