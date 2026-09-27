package com.luomiblog.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * install.lock 路径解析器。
 *
 * <p>锁文件位置由 {@code app.install.lock-file} 配置（可用环境变量
 * {@code INSTALL_LOCK_FILE} 覆盖），默认 {@code config/install.lock}——
 * 与 config/custom-application.yml 等既有运行时文件落点一致，
 * 避免 systemd 等部署方式下因工作目录不同导致锁文件落点不可预测。</p>
 *
 * <p>升级兼容：旧版本把锁文件落在进程工作目录（{@code install.lock}）。
 * 当配置路径不存在而旧路径存在时，首次访问自动把旧锁文件迁移到配置路径；
 * 迁移失败（只读文件系统等）则回退继续使用旧路径并告警。</p>
 */
@Slf4j
@Component
public class InstallLockLocator {

    /** 旧版本的锁文件相对路径（相对进程工作目录） */
    private static final String LEGACY_LOCK_FILE = "install.lock";

    private final String lockFile;

    public InstallLockLocator(
            @Value("${app.install.lock-file:${INSTALL_LOCK_FILE:config/install.lock}}") String lockFile) {
        this.lockFile = lockFile;
    }

    /** 当前生效的锁文件路径（含旧版锁文件一次性自动迁移） */
    public Path resolve() {
        Path configured = Paths.get(lockFile);
        if (Files.exists(configured)) {
            return configured;
        }

        Path legacy = Paths.get(LEGACY_LOCK_FILE);
        if (Files.exists(legacy)) {
            try {
                if (configured.getParent() != null) {
                    Files.createDirectories(configured.getParent());
                }
                try {
                    Files.move(legacy, configured);
                    log.info("已将旧版 install.lock 迁移到配置路径: {}", configured.toAbsolutePath());
                } catch (FileAlreadyExistsException e) {
                    // 并发迁移竞态：目标已生成，清掉旧文件保持单一落点
                    Files.deleteIfExists(legacy);
                }
                return configured;
            } catch (IOException moveErr) {
                log.warn("install.lock 迁移到 {} 失败，继续使用旧路径: {}",
                        configured.toAbsolutePath(), moveErr.getMessage());
                return legacy;
            }
        }

        return configured;
    }

    /** 锁文件是否存在（已安装） */
    public boolean exists() {
        return Files.exists(resolve());
    }

    /** 删除锁文件（如存在） */
    public void deleteIfExists() throws IOException {
        Files.deleteIfExists(resolve());
    }
}
