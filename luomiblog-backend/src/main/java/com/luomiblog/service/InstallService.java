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
}
