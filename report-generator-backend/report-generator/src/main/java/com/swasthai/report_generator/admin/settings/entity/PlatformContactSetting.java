package com.swasthai.report_generator.admin.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "platform_contact_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformContactSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "support_email", nullable = false)
    @Builder.Default
    private String supportEmail = "support@swasthai.com";

    @Column(name = "privacy_email", nullable = false)
    @Builder.Default
    private String privacyEmail = "privacy@swasthai.com";

    @Column(name = "legal_email", nullable = false)
    @Builder.Default
    private String legalEmail = "legal@swasthai.com";

    @Column(name = "helpline_phone", nullable = false)
    @Builder.Default
    private String helplinePhone = "+91 99999 99999";

    @Column(name = "whatsapp_number", nullable = false)
    @Builder.Default
    private String whatsappNumber = "919999999999";

    @Column(name = "operating_hours", nullable = false)
    @Builder.Default
    private String operatingHours = "Monday to Saturday, 9:00 AM – 6:00 PM IST";

    @Column(name = "office_address", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String officeAddress = "SwasthAI Healthcare Technologies Private Limited";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    @PrePersist
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

