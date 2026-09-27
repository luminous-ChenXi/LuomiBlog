package com.luomiblog.service;

import com.luomiblog.dto.install.*;

public interface InstallService {

    InstallStatusResponse getInstallStatus();

    EnvironmentCheckResponse checkEnvironment();

    boolean testDatabaseConnection(DatabaseConfigRequest request);

    DatabaseCheckResponse checkDatabase(DatabaseConfigRequest request);

    void executeSqlScripts(DatabaseConfigRequest request);

    void createAdminAccount(AdminAccountRequest request);

    void saveSiteConfig(SiteConfigRequest request);

    void completeInstallation();

    boolean verifyReinstallPermission(String verificationPassword);

    void resetInstallation();

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
