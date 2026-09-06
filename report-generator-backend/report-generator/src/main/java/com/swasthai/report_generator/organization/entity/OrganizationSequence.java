package com.swasthai.report_generator.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "organization_sequences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sequence_organization",
                        columnNames = "organization_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_sequence_organization"
            )
    )
    private Organization organization;

    @Column(
            name = "patient_sequence",
            nullable = false
    )
    @Builder.Default
    private Long patientSequence = 0L;

    @Version
    @Column(nullable = false)
    private Long version;
}