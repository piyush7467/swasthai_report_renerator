package com.swasthai.report_generator.user.repository;

import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByRefId(String refId);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRefId(String refId);

    long countByRole(Role role);

    long countByOrganization_IdAndRole(
            UUID organizationId,
            Role role
    );

    Optional<User> findByRefIdAndOrganization_Id(
            String refId,
            UUID organizationId
    );

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            UUID id
    );

    boolean existsByIdAndOrganization_Id(
            UUID userId,
            UUID organizationId
    );

    Optional<User> findByEmailIgnoreCaseAndOrganization_Id(
            String email,
            UUID organizationId
    );

    Page<User> findAllByOrganization_Id(
            UUID organizationId,
            Pageable pageable
    );

    Page<User> findAllByOrganization_IdAndStatus(
            UUID organizationId,
            UserStatus status,
            Pageable pageable
    );

    Page<User> findAllByOrganization_IdAndRole(
            UUID organizationId,
            Role role,
            Pageable pageable
    );

    Page<User> findAllByStatus(
            UserStatus status,
            Pageable pageable
    );

    Page<User> findAllByRole(
            Role role,
            Pageable pageable
    );
}