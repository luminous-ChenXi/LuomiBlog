package com.luomiblog.service;

import com.luomiblog.dto.install.*;

public interface InstallService {

    InstallStatusResponse getInstallStatus();

    EnvironmentCheckResponse checkEnvironment();

    boolean testDatabaseConnection(DatabaseConfigRequest request);

    /**
     * 检查数据库连接并获取详细信息
     * 包括是否已有数据、MySQL版本等信息
     * @param request 数据库配置
     * @return 数据库检查结果
     */
    DatabaseCheckResponse checkDatabase(DatabaseConfigRequest request);

    void executeSqlScripts(DatabaseConfigRequest request);

    void createAdminAccount(AdminAccountRequest request);

    void saveSiteConfig(SiteConfigRequest request);

    void completeInstallation();

    /**
     * 验证重新安装权限
     * 需要输入当前数据库密码或管理员密码进行二次验证
     * @param verificationPassword 验证密码（数据库密码或管理员密码）
     * @return 验证是否通过
     */
    boolean verifyReinstallPermission(String verificationPassword);

    /**
     * 重置安装状态（验证通过后调用）
     * 删除安装锁文件，允许重新安装
     */
    void resetInstallation();

    /**
     * 执行重新安装
     * 根据用户选择的选项执行不同的安装逻辑
     * @param option 重新安装选项
     * @param request 数据库配置
     */
    void executeReinstall(ReinstallOption option, DatabaseConfigRequest request);

    /**
     * 检查是否需要显示重新安装选项
     * 当系统已有数据但 install.lock 不存在时返回 true
     * @return 是否需要显示选项
     */
    boolean needsReinstallOptions();

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
