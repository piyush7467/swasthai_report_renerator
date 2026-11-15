package com.swasthai.report_generator.admin.settings.service;

import com.swasthai.report_generator.admin.settings.dto.PlatformContactSettingDto;

public interface PlatformContactSettingService {
    PlatformContactSettingDto getContactSettings();
    PlatformContactSettingDto updateContactSettings(PlatformContactSettingDto dto);
}

