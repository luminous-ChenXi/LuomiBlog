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
}
