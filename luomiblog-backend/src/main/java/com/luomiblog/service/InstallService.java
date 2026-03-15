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
}
