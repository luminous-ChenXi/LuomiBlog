package com.luomiblog.repository;

import com.luomiblog.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 站点设置键值仓库
 */
@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
