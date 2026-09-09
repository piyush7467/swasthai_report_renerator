package com.swasthai.report_generator.auth.service.impl;

import com.swasthai.report_generator.auth.service.CurrentOrganizationService;
import com.swasthai.report_generator.auth.service.CurrentUserService;
import com.swasthai.report_generator.organization.entity.Organization;
import com.swasthai.report_generator.user.entity.Role;
import com.swasthai.report_generator.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentOrganizationServiceImpl
        implements CurrentOrganizationService {

    private final CurrentUserService currentUserService;

    @Override
    public Organization getCurrentOrganization() {

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalStateException(
                    "SUPER_ADMIN is not associated with an organization."
            );
        }

        Organization organization =
                currentUser.getOrganization();

        if (organization == null) {
            throw new IllegalStateException(
                    "Authenticated user is not associated with an organization."
            );
        }

        return organization;
    }
}