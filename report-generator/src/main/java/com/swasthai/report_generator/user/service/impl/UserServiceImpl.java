package com.swasthai.report_generator.user.service.impl;

import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final OrganizationRepository organizationRepository;
        private final PasswordEncoder passwordEncoder;
        private final CurrentUserService currentUserService;

        @Override
        public UserResponse createUser(CreateUserRequest request) {

                User currentUser = currentUserService.getCurrentUser();

                String name = request.getName().trim();

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                Role requestedRole = request.getRole();

                if (userRepository.existsByEmailIgnoreCase(email)) {
                        throw new ResourceAlreadyExistsException(
                                        "User with this email already exists.");
                }

                /*
                 * SUPER_ADMIN
                 *
                 * Only the existing SUPER_ADMIN can create
                 * another platform-level user.
                 *
                 * But our business rule says there can be
                 * only ONE SUPER_ADMIN.
                 */
                if (requestedRole == Role.SUPER_ADMIN) {

                        if (currentUser.getRole() != Role.SUPER_ADMIN) {
                                throw new ForbiddenException(
                                                "Only SUPER_ADMIN can create a SUPER_ADMIN.");
                        }

                        throw new ResourceAlreadyExistsException(
                                        "Only one SUPER_ADMIN is allowed.");
                }

                /*
                 * ORG_ADMIN
                 *
                 * Only SUPER_ADMIN can create an ORG_ADMIN.
                 */
                if (requestedRole == Role.ORG_ADMIN) {

                        if (currentUser.getRole() != Role.SUPER_ADMIN) {
                                throw new ForbiddenException("Only SUPER_ADMIN can create an ORG_ADMIN.");
                         }

                        if (request.getOrganizationRefId() == null ||
                                        request.getOrganizationRefId().isBlank()) {

                                throw new IllegalArgumentException(
                                                "Organization is required for ORG_ADMIN.");
                        }

                        Organization organization = organizationRepository
                                        .findByRefId(
                                                        request.getOrganizationRefId()
                                                                        .trim())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Organization not found."));

                        validateOrganization(organization);

                        User user = buildUser(
                                        name,
                                        email,
                                        request.getPassword(),
                                        Role.ORG_ADMIN,
                                        organization);

                        return mapToResponse(
                                        userRepository.save(user));
                }

                /*
                 * LAB_STAFF
                 *
                 * SUPER_ADMIN can create staff for any organization.
                 *
                 * ORG_ADMIN can create staff only inside
                 * their own organization.
                 */
                if (requestedRole == Role.LAB_STAFF) {

                        Organization organization;

                        if (currentUser.getRole() == Role.SUPER_ADMIN) {

                                if (request.getOrganizationRefId() == null ||
                                                request.getOrganizationRefId().isBlank()) {

                                        throw new ForbiddenException(
                                                        "Organization is required for LAB_STAFF.");
                                }

                                organization = organizationRepository
                                                .findByRefId(
                                                                request.getOrganizationRefId()
                                                                                .trim())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Organization not found."));

                        } else if (currentUser.getRole() == Role.ORG_ADMIN) {

                                organization = currentUser.getOrganization();

                                if (organization == null) {
                                        throw new ForbiddenException(
                                                        "ORG_ADMIN is not associated with an organization.");
                                }

                        } else {

                                throw new ForbiddenException(
                                                "LAB_STAFF cannot create users.");
                        }

                        validateOrganization(organization);

                        User user = buildUser(
                                        name,
                                        email,
                                        request.getPassword(),
                                        Role.LAB_STAFF,
                                        organization);

                        return mapToResponse(
                                        userRepository.save(user));
                }

                throw new IllegalArgumentException(
                                "Unsupported user role.");
        }

        private User buildUser(
                        String name,
                        String email,
                        String password,
                        Role role,
                        Organization organization) {

                return User.builder()
                                .name(name)
                                .email(email)
                                .passwordHash(
                                                passwordEncoder.encode(password))
                                .role(role)
                                .organization(organization)
                                .build();
        }

        private void validateOrganization(
                        Organization organization) {

                if (organization.getStatus() != OrganizationStatus.ACTIVE) {

                        throw new IllegalStateException(
                                        "Organization is not active.");
                }
        }

        private UserResponse mapToResponse(
                        User user) {

                return UserResponse.builder()
                                .refId(user.getRefId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .status(user.getStatus())
                                .organizationRefId(
                                                user.getOrganization() != null
                                                                ? user.getOrganization().getRefId()
                                                                : null)
                                .lastLoginAt(user.getLastLoginAt())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .build();
        }
}