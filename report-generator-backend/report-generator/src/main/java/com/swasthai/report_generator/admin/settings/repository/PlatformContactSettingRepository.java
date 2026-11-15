package com.swasthai.report_generator.admin.settings.repository;

import com.swasthai.report_generator.admin.settings.entity.PlatformContactSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformContactSettingRepository extends JpaRepository<PlatformContactSetting, Long> {
    Optional<PlatformContactSetting> findFirstByOrderByIdAsc();
}

