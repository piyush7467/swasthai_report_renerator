package com.swasthai.report_generator.admin.settings.service.impl;

import com.swasthai.report_generator.admin.settings.dto.PlatformContactSettingDto;
import com.swasthai.report_generator.admin.settings.entity.PlatformContactSetting;
import com.swasthai.report_generator.admin.settings.repository.PlatformContactSettingRepository;
import com.swasthai.report_generator.admin.settings.service.PlatformContactSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformContactSettingServiceImpl implements PlatformContactSettingService {

    private final PlatformContactSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PlatformContactSettingDto getContactSettings() {
        PlatformContactSetting setting = repository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSetting);
        return mapToDto(setting);
    }

    @Override
    @Transactional
    public PlatformContactSettingDto updateContactSettings(PlatformContactSettingDto dto) {
        log.info("Super Admin updating platform contact settings: supportEmail={}, helpline={}",
                dto.getSupportEmail(), dto.getHelplinePhone());

        PlatformContactSetting setting = repository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSetting);

        setting.setSupportEmail(dto.getSupportEmail().trim());
        setting.setPrivacyEmail(dto.getPrivacyEmail().trim());
        setting.setLegalEmail(dto.getLegalEmail().trim());
        setting.setHelplinePhone(dto.getHelplinePhone().trim());
        setting.setWhatsappNumber(dto.getWhatsappNumber().trim());
        setting.setOperatingHours(dto.getOperatingHours().trim());
        setting.setOfficeAddress(dto.getOfficeAddress().trim());
        setting.setUpdatedAt(Instant.now());

        PlatformContactSetting saved = repository.save(setting);
        return mapToDto(saved);
    }

    private PlatformContactSetting createDefaultSetting() {
        PlatformContactSetting setting = PlatformContactSetting.builder()
                .supportEmail("support@swasthai.com")
                .privacyEmail("privacy@swasthai.com")
                .legalEmail("legal@swasthai.com")
                .helplinePhone("+91 99999 99999")
                .whatsappNumber("919999999999")
                .operatingHours("Monday to Saturday, 9:00 AM – 6:00 PM IST")
                .officeAddress("SwasthAI Healthcare Technologies Private Limited")
                .updatedAt(Instant.now())
                .build();
        return repository.save(setting);
    }

    private PlatformContactSettingDto mapToDto(PlatformContactSetting entity) {
        return PlatformContactSettingDto.builder()
                .supportEmail(entity.getSupportEmail())
                .privacyEmail(entity.getPrivacyEmail())
                .legalEmail(entity.getLegalEmail())
                .helplinePhone(entity.getHelplinePhone())
                .whatsappNumber(entity.getWhatsappNumber())
                .operatingHours(entity.getOperatingHours())
                .officeAddress(entity.getOfficeAddress())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

