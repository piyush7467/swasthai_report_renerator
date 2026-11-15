package com.swasthai.report_generator.admin.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformContactSettingDto {

    @NotBlank(message = "Support email cannot be blank")
    @Email(message = "Support email must be a valid email format")
    private String supportEmail;

    @NotBlank(message = "Privacy email cannot be blank")
    @Email(message = "Privacy email must be a valid email format")
    private String privacyEmail;

    @NotBlank(message = "Legal email cannot be blank")
    @Email(message = "Legal email must be a valid email format")
    private String legalEmail;

    @NotBlank(message = "Helpline phone cannot be blank")
    private String helplinePhone;

    @NotBlank(message = "WhatsApp number cannot be blank")
    private String whatsappNumber;

    @NotBlank(message = "Operating hours cannot be blank")
    private String operatingHours;

    @NotBlank(message = "Office address cannot be blank")
    private String officeAddress;

    private Instant updatedAt;
}

