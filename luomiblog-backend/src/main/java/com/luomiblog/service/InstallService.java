package com.luomiblog.service;

import com.luomiblog.dto.install.*;

public interface InstallService {

    InstallStatusResponse getInstallStatus();

    EnvironmentCheckResponse checkEnvironment();

    boolean testDatabaseConnection(DatabaseConfigRequest request);

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
}
