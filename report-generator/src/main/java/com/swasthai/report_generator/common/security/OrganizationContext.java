package com.swasthai.report_generator.common.security;

import com.swasthai.report_generator.user.entity.User;

public final class OrganizationContext {

    private OrganizationContext() {
    }

    public static String getOrganizationRefId(User user) {

        if (user == null) {
            throw new IllegalStateException(
                    "Authenticated user is required."
            );
        }

        if (user.getOrganization() == null) {

            if (user.getRole().name().equals("SUPER_ADMIN")) {
                return null;
            }

            throw new IllegalStateException(
                    "User is not associated with an organization."
            );
        }

        return user.getOrganization().getRefId();
    }
}