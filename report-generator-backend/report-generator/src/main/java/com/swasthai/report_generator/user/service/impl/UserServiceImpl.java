package com.swasthai.report_generator.user.service.impl;

import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.common.exception.ForbiddenException;
import com.swasthai.report_generator.common.exception.ResourceAlreadyExistsException;
import com.swasthai.report_generator.common.exception.ResourceNotFoundException;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.organization.entity.OrganizationStatus;
import com.swasthai.report_generator.organization.repository.OrganizationRepository;
import com.swasthai.report_generator.user.dto.request.CreateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserRequest;
import com.swasthai.report_generator.user.dto.request.UpdateUserStatusRequest;
import com.swasthai.report_generator.user.dto.response.UserPageResponse;
import com.swasthai.report_generator.user.dto.response.UserResponse;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import com.swasthai.report_generator.user.entity.UserStatus;
import com.swasthai.report_generator.user.repository.UserRepository;
import com.swasthai.report_generator.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    // ============================================================
    // CREATE USER
    // ============================================================

    @Override
    public UserResponse createUser(
            CreateUserRequest request
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        String name =
                normalizeName(request.getName());

        String email =
                normalizeEmail(request.getEmail());

        Role requestedRole =
                request.getRole();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceAlreadyExistsException(
                    "User with this email already exists."
            );
        }

        /*
         * SUPER_ADMIN
         *
         * There must only ever be one.
         */
        if (requestedRole == Role.SUPER_ADMIN) {

            if (currentUser.getRole() != Role.SUPER_ADMIN) {
                throw new ForbiddenException(
                        "Only SUPER_ADMIN can create a SUPER_ADMIN."
                );
            }

            throw new ResourceAlreadyExistsException(
                    "Only one SUPER_ADMIN is allowed."
            );
        }

        /*
         * ORG_ADMIN
         *
         * Only SUPER_ADMIN may create ORG_ADMIN.
         */
        if (requestedRole == Role.ORG_ADMIN) {

            requireRole(
                    currentUser,
                    Role.SUPER_ADMIN,
                    "Only SUPER_ADMIN can create an ORG_ADMIN."
            );

            Organization organization =
                    getRequiredActiveOrganization(
                            request.getOrganizationRefId()
                    );

            User user =
                    buildUser(
                            name,
                            email,
                            request.getPassword(),
                            Role.ORG_ADMIN,
                            organization
                    );

            return mapToResponse(
                    userRepository.save(user)
            );
        }

        /*
         * LAB_STAFF
         *
         * SUPER_ADMIN -> any organization
         * ORG_ADMIN   -> own organization
         */
        if (requestedRole == Role.LAB_STAFF) {

            Organization organization;

            if (currentUser.getRole() == Role.SUPER_ADMIN) {

                organization =
                        getRequiredActiveOrganization(
                                request.getOrganizationRefId()
                        );

            } else if (currentUser.getRole() == Role.ORG_ADMIN) {

                organization =
                        requireCurrentUserOrganization(
                                currentUser
                        );

                validateOrganization(organization);

            } else {

                throw new ForbiddenException(
                        "LAB_STAFF cannot create users."
                );
            }

            User user =
                    buildUser(
                            name,
                            email,
                            request.getPassword(),
                            Role.LAB_STAFF,
                            organization
                    );

            return mapToResponse(
                    userRepository.save(user)
            );
        }

        throw new IllegalArgumentException(
                "Unsupported user role."
        );
    }

    // ============================================================
    // LIST USERS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse getUsers(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Role role,
            UserStatus status
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        validatePagination(page, size);

        Sort.Direction direction =
                resolveSortDirection(sortDirection);

        String sortField =
                resolveSortField(sortBy);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(direction, sortField)
                );

        Page<User> users;

        /*
         * SUPER_ADMIN can see all users.
         */
        if (currentUser.getRole() == Role.SUPER_ADMIN) {

            users =
                    findUsersForSuperAdmin(
                            pageable,
                            role,
                            status
                    );

        } else if (currentUser.getRole() == Role.ORG_ADMIN) {

            Organization organization =
                    requireCurrentUserOrganization(
                            currentUser
                    );

            if (role == Role.SUPER_ADMIN
                    || role == Role.ORG_ADMIN) {

                throw new ForbiddenException(
                        "ORG_ADMIN can only view LAB_STAFF users."
                );
            }

            if (role == null && status == null) {

                users =
                        userRepository
                                .findAllByOrganization_Id(
                                        organization.getId(),
                                        pageable
                                );

            } else if (role == null) {

                users =
                        userRepository
                                .findAllByOrganization_IdAndStatus(
                                        organization.getId(),
                                        status,
                                        pageable
                                );

            } else if (status == null) {

                users =
                        userRepository
                                .findAllByOrganization_IdAndRole(
                                        organization.getId(),
                                        role,
                                        pageable
                                );

            } else {

                /*
                 * Combined role + status filtering is deliberately
                 * handled in memory only for a small page.
                 *
                 * However, since this is an administrative list,
                 * we reject this combination rather than silently
                 * returning incomplete pagination.
                 */
                throw new IllegalArgumentException(
                        "Filtering by both role and status is not supported for ORG_ADMIN."
                );
            }

        } else {

            throw new ForbiddenException(
                    "You are not allowed to view users."
            );
        }

        List<UserResponse> content =
                users.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return UserPageResponse.builder()
                .content(content)
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .first(users.isFirst())
                .last(users.isLast())
                .build();
    }

    // ============================================================
    // GET USER
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByRefId(
            String refId
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        String normalizedRefId =
                normalizeRefId(refId);

        User target =
                userRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found."
                                )
                        );

        authorizeView(
                currentUser,
                target
        );

        return mapToResponse(target);
    }

    // ============================================================
    // UPDATE USER
    // ============================================================

    @Override
    public UserResponse updateUser(
            String refId,
            UpdateUserRequest request
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        String normalizedRefId =
                normalizeRefId(refId);

        User target =
                userRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found."
                                )
                        );

        /*
         * SUPER_ADMIN account is protected.
         */
        ensureTargetIsNotSuperAdmin(target);

        authorizeManagement(
                currentUser,
                target
        );

        String name =
                normalizeName(request.getName());

        String email =
                normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(
                email,
                target.getId()
        )) {
            throw new ResourceAlreadyExistsException(
                    "User with this email already exists."
            );
        }

        /*
         * ORG_ADMIN may only update the user's basic identity.
         *
         * They cannot change role or organization.
         */
        if (currentUser.getRole() == Role.ORG_ADMIN) {

            if (request.getRole() != null) {
                throw new ForbiddenException(
                        "ORG_ADMIN cannot change user roles."
                );
            }

            if (request.getOrganizationRefId() != null
                    && !request.getOrganizationRefId()
                    .isBlank()) {

                throw new ForbiddenException(
                        "ORG_ADMIN cannot change user organization."
                );
            }

            if (target.getRole() != Role.LAB_STAFF) {
                throw new ForbiddenException(
                        "ORG_ADMIN can only manage LAB_STAFF users."
                );
            }

            target.setName(name);
            target.setEmail(email);

        } else {

            /*
             * SUPER_ADMIN
             *
             * Can modify role and organization for
             * non-SUPER_ADMIN users.
             */
            Role requestedRole =
                    request.getRole() != null
                            ? request.getRole()
                            : target.getRole();

            if (requestedRole == Role.SUPER_ADMIN) {
                throw new ForbiddenException(
                        "The SUPER_ADMIN account cannot be assigned through this operation."
                );
            }

            Organization organization =
                    resolveOrganizationForUpdate(
                            target,
                            requestedRole,
                            request.getOrganizationRefId()
                    );

            target.setName(name);
            target.setEmail(email);
            target.setRole(requestedRole);
            target.setOrganization(organization);
        }

        return mapToResponse(
                userRepository.save(target)
        );
    }

    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @Override
    public UserResponse updateUserStatus(
            String refId,
            UpdateUserStatusRequest request
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        String normalizedRefId =
                normalizeRefId(refId);

        User target =
                userRepository
                        .findByRefId(normalizedRefId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found."
                                )
                        );

        ensureTargetIsNotSuperAdmin(target);

        authorizeManagement(
                currentUser,
                target
        );

        UserStatus requestedStatus =
                request.getStatus();

        if (target.getStatus() == requestedStatus) {
            throw new IllegalStateException(
                    "User is already in the requested status."
            );
        }

        target.setStatus(requestedStatus);

        return mapToResponse(
                userRepository.save(target)
        );
    }

    // ============================================================
    // SUPER ADMIN LIST
    // ============================================================

    private Page<User> findUsersForSuperAdmin(
            Pageable pageable,
            Role role,
            UserStatus status
    ) {

        /*
         * No filters.
         */
        if (role == null && status == null) {

            return userRepository.findAll(pageable);
        }

        /*
         * Role filter.
         */
        if (role != null && status == null) {

            return userRepository.findAllByRole(
                    role,
                    pageable
            );
        }

        /*
         * Status filter.
         */
        if (role == null) {

            return userRepository.findAllByStatus(
                    status,
                    pageable
            );
        }

        /*
         * Combined role + status filtering is best handled
         * through a specification, but we are deliberately
         * not introducing Specifications just for this phase.
         *
         * Reject instead of returning incorrect pagination.
         */
        throw new IllegalArgumentException(
                "Filtering by both role and status is not supported."
        );
    }

    // ============================================================
    // AUTHORIZATION
    // ============================================================

    private void authorizeView(
            User currentUser,
            User target
    ) {

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.ORG_ADMIN) {

            if (target.getRole() != Role.LAB_STAFF) {
                throw new ForbiddenException(
                        "ORG_ADMIN can only view LAB_STAFF users."
                );
            }

            Organization organization =
                    requireCurrentUserOrganization(
                            currentUser
                    );

            if (target.getOrganization() == null
                    || !target.getOrganization()
                    .getId()
                    .equals(organization.getId())) {

                /*
                 * Do not reveal whether a user from another
                 * organization exists.
                 */
                throw new ResourceNotFoundException(
                        "User not found."
                );
            }

            return;
        }

        throw new ForbiddenException(
                "You are not allowed to view users."
        );
    }

    private void authorizeManagement(
            User currentUser,
            User target
    ) {

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.ORG_ADMIN) {

            if (target.getRole() != Role.LAB_STAFF) {
                throw new ForbiddenException(
                        "ORG_ADMIN can only manage LAB_STAFF users."
                );
            }

            Organization organization =
                    requireCurrentUserOrganization(
                            currentUser
                    );

            if (target.getOrganization() == null
                    || !target.getOrganization()
                    .getId()
                    .equals(organization.getId())) {

                /*
                 * Tenant isolation.
                 */
                throw new ResourceNotFoundException(
                        "User not found."
                );
            }

            return;
        }

        throw new ForbiddenException(
                "You are not allowed to manage users."
        );
    }

    private void ensureTargetIsNotSuperAdmin(
            User target
    ) {

        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new ForbiddenException(
                    "The SUPER_ADMIN account cannot be modified."
            );
        }
    }

    private void requireRole(
            User currentUser,
            Role requiredRole,
            String message
    ) {

        if (currentUser.getRole() != requiredRole) {
            throw new ForbiddenException(message);
        }
    }

    // ============================================================
    // ORGANIZATION
    // ============================================================

    private Organization getRequiredActiveOrganization(
            String organizationRefId
    ) {

        if (organizationRefId == null
                || organizationRefId.isBlank()) {

            throw new IllegalArgumentException(
                    "Organization is required."
            );
        }

        Organization organization =
                organizationRepository
                        .findByRefId(
                                organizationRefId.trim()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found."
                                )
                        );

        validateOrganization(organization);

        return organization;
    }

    private Organization requireCurrentUserOrganization(
            User currentUser
    ) {

        Organization organization =
                currentUser.getOrganization();

        if (organization == null) {
            throw new ForbiddenException(
                    "User is not associated with an organization."
            );
        }

        return organization;
    }

    private void validateOrganization(
            Organization organization
    ) {

        if (organization.getStatus()
                != OrganizationStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Organization is not active."
            );
        }
    }

    private Organization resolveOrganizationForUpdate(
            User target,
            Role requestedRole,
            String organizationRefId
    ) {

        /*
         * All non-SUPER_ADMIN users must belong to
         * an active organization.
         */
        if (requestedRole == Role.ORG_ADMIN
                || requestedRole == Role.LAB_STAFF) {

            if (organizationRefId == null
                    || organizationRefId.isBlank()) {

                if (target.getOrganization() == null) {
                    throw new IllegalArgumentException(
                            "Organization is required for this user role."
                    );
                }

                validateOrganization(
                        target.getOrganization()
                );

                return target.getOrganization();
            }

            return getRequiredActiveOrganization(
                    organizationRefId
            );
        }

        throw new IllegalArgumentException(
                "Unsupported user role."
        );
    }

    // ============================================================
    // USER CONSTRUCTION
    // ============================================================

    private User buildUser(
            String name,
            String email,
            String password,
            Role role,
            Organization organization
    ) {

        return User.builder()
                .name(name)
                .email(email)
                .passwordHash(
                        passwordEncoder.encode(password)
                )
                .role(role)
                .organization(organization)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ============================================================
    // VALIDATION / NORMALIZATION
    // ============================================================

    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
                            + "."
            );
        }
    }

    private String resolveSortField(
            String sortBy
    ) {

        if (sortBy == null
                || sortBy.isBlank()) {

            return "createdAt";
        }

        return switch (sortBy.trim()) {

            case "name" -> "name";

            case "email" -> "email";

            case "role" -> "role";

            case "status" -> "status";

            case "createdAt" -> "createdAt";

            case "updatedAt" -> "updatedAt";

            case "lastLoginAt" -> "lastLoginAt";

            default -> throw new IllegalArgumentException(
                    "Invalid user sort field."
            );
        };
    }

    private Sort.Direction resolveSortDirection(
            String sortDirection
    ) {

        if (sortDirection == null
                || sortDirection.isBlank()) {

            return Sort.Direction.DESC;
        }

        try {

            return Sort.Direction.fromString(
                    sortDirection.trim()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Invalid sort direction. Use ASC or DESC."
            );
        }
    }

    private String normalizeName(
            String value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "User name is required."
            );
        }

        String normalized =
                value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "User name is required."
            );
        }

        return normalized;
    }

    private String normalizeEmail(
            String value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        String normalized =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        return normalized;
    }

    private String normalizeRefId(
            String value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "User reference ID is required."
            );
        }

        String normalized =
                value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "User reference ID is required."
            );
        }

        return normalized;
    }

    // ============================================================
    // RESPONSE
    // ============================================================

    private UserResponse mapToResponse(
            User user
    ) {

        return UserResponse.builder()
                .refId(user.getRefId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .organizationRefId(
                        user.getOrganization() != null
                                ? user.getOrganization().getRefId()
                                : null
                )
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}