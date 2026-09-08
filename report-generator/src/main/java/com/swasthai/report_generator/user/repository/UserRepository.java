package com.swasthai.report_generator.user.repository;

import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /*
     * Find user using public reference ID.
     */
    Optional<User> findByRefId(String refId);

    /*
     * Login lookup.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /*
     * Check whether email already exists.
     */
    boolean existsByEmailIgnoreCase(String email);

    /*
     * Check whether public reference ID already exists.
     */
    boolean existsByRefId(String refId);

    /*
     * Count users having a particular role.
     *
     * Used for enforcing the single SUPER_ADMIN rule
     * at the application level.
     */
    long countByRole(Role role);

    /*
     * Count users of a particular role inside an organization.
     *
     * Useful for subscription/user-limit enforcement.
     */
    long countByOrganization_IdAndRole(
            UUID organizationId,
            Role role
    );

    /*
     * Tenant-safe lookup.
     *
     * Finds a user only if the user belongs to the
     * specified organization.
     */
    Optional<User> findByRefIdAndOrganization_Id(
            String refId,
            UUID organizationId
    );

    /*
     * Used while updating a user's email.
     *
     * Checks another user with the same email,
     * excluding the current user.
     */
    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            UUID id
    );

    /*
     * Tenant-safe existence check using internal IDs.
     */
    boolean existsByIdAndOrganization_Id(
            UUID userId,
            UUID organizationId
    );

    /*
     * Tenant-safe email lookup.
     */
    Optional<User> findByEmailIgnoreCaseAndOrganization_Id(
            String email,
            UUID organizationId
    );
}