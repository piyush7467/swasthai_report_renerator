import { useAuth } from "./AuthContext";
import {
  useLicenseOverview,
  useOwnLicenseQuery,
} from "@/modules/org-admin/licensing/hooks/useOrgLicense";
import type { LicenseResponse } from "@/modules/org-admin/licensing/types/licenseTypes";

export type ExpiryStatusType =
  | "ACTIVE"
  | "EXPIRING_SOON"
  | "EXPIRED"
  | "DEACTIVATED"
  | "NO_PLAN"
  | "UNKNOWN";

export interface SubscriptionEligibility {
  isEligible: boolean;
  isLoading: boolean;
  expiryStatus: ExpiryStatusType;
  license: LicenseResponse | null;
  daysRemaining: number;
  reasonMessage: string | null;
  canManageSubscription: boolean;
  subscriptionManagementPath: string;
  isSuperAdmin: boolean;
  isOrgAdmin: boolean;
  isLabStaff: boolean;
  refetch: () => Promise<unknown>;
}

export function useSubscriptionEligibility(): SubscriptionEligibility {
  const { user, isInitializing } = useAuth();

  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const isOrgAdmin = user?.role === "ORG_ADMIN";
  const isLabStaff = user?.role === "LAB_STAFF";

  // ORG_ADMIN has access to full overview
  const overviewQuery = useLicenseOverview();
  // LAB_STAFF has access to /license/me
  const ownLicenseQuery = useOwnLicenseQuery();

  if (isInitializing || !user) {
    return {
      isEligible: false,
      isLoading: true,
      expiryStatus: "UNKNOWN",
      license: null,
      daysRemaining: 0,
      reasonMessage: null,
      canManageSubscription: false,
      subscriptionManagementPath: "/org-admin/license",
      isSuperAdmin: false,
      isOrgAdmin: false,
      isLabStaff: false,
      refetch: async () => {},
    };
  }

  // Super Admin is never blocked from operational views or platform management
  if (isSuperAdmin) {
    return {
      isEligible: true,
      isLoading: false,
      expiryStatus: "ACTIVE",
      license: null,
      daysRemaining: 999,
      reasonMessage: null,
      canManageSubscription: true,
      subscriptionManagementPath: "/super-admin/licensing/licenses",
      isSuperAdmin: true,
      isOrgAdmin: false,
      isLabStaff: false,
      refetch: async () => {},
    };
  }

  if (isOrgAdmin) {
    const { data: overview, isLoading, refetch } = overviewQuery;

    if (isLoading) {
      return {
        isEligible: false,
        isLoading: true,
        expiryStatus: "UNKNOWN",
        license: null,
        daysRemaining: 0,
        reasonMessage: null,
        canManageSubscription: true,
        subscriptionManagementPath: "/org-admin/license",
        isSuperAdmin: false,
        isOrgAdmin: true,
        isLabStaff: false,
        refetch,
      };
    }

    if (!overview || !overview.license || overview.expiryStatus === "NO_PLAN") {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "NO_PLAN",
        license: null,
        daysRemaining: 0,
        reasonMessage:
          "No plan active. Please contact Super Admin to activate a plan.",
        canManageSubscription: true,
        subscriptionManagementPath: "/org-admin/license",
        isSuperAdmin: false,
        isOrgAdmin: true,
        isLabStaff: false,
        refetch,
      };
    }

    const { license, expiryStatus, daysRemaining } = overview;

    if (expiryStatus === "EXPIRED") {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "EXPIRED",
        license,
        daysRemaining: 0,
        reasonMessage:
          "Your organization subscription has expired. Please renew or upgrade your license to continue clinical operations.",
        canManageSubscription: true,
        subscriptionManagementPath: "/org-admin/license",
        isSuperAdmin: false,
        isOrgAdmin: true,
        isLabStaff: false,
        refetch,
      };
    }

    if (expiryStatus === "DEACTIVATED") {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "DEACTIVATED",
        license,
        daysRemaining,
        reasonMessage:
          "Your organization subscription has been deactivated. Please contact platform support to reactivate your license.",
        canManageSubscription: true,
        subscriptionManagementPath: "/org-admin/license",
        isSuperAdmin: false,
        isOrgAdmin: true,
        isLabStaff: false,
        refetch,
      };
    }

    if (expiryStatus === "EXPIRING_SOON") {
      return {
        isEligible: true,
        isLoading: false,
        expiryStatus: "EXPIRING_SOON",
        license,
        daysRemaining,
        reasonMessage: `Your organization subscription expires in ${daysRemaining} day${daysRemaining === 1 ? "" : "s"}. Renew your plan to avoid operational disruption.`,
        canManageSubscription: true,
        subscriptionManagementPath: "/org-admin/license",
        isSuperAdmin: false,
        isOrgAdmin: true,
        isLabStaff: false,
        refetch,
      };
    }

    // ACTIVE
    return {
      isEligible: true,
      isLoading: false,
      expiryStatus: "ACTIVE",
      license,
      daysRemaining,
      reasonMessage: null,
      canManageSubscription: true,
      subscriptionManagementPath: "/org-admin/license",
      isSuperAdmin: false,
      isOrgAdmin: true,
      isLabStaff: false,
      refetch,
    };
  }

  // LAB_STAFF
  if (isLabStaff) {
    const { data: license, isLoading, refetch } = ownLicenseQuery;

    if (isLoading) {
      return {
        isEligible: false,
        isLoading: true,
        expiryStatus: "UNKNOWN",
        license: null,
        daysRemaining: 0,
        reasonMessage: null,
        canManageSubscription: false,
        subscriptionManagementPath: "/lab-staff/dashboard",
        isSuperAdmin: false,
        isOrgAdmin: false,
        isLabStaff: true,
        refetch,
      };
    }

    if (!license) {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "NO_PLAN",
        license: null,
        daysRemaining: 0,
        reasonMessage:
          "No plan active. Please contact Super Admin to activate a plan.",
        canManageSubscription: false,
        subscriptionManagementPath: "/lab-staff/dashboard",
        isSuperAdmin: false,
        isOrgAdmin: false,
        isLabStaff: true,
        refetch,
      };
    }

    if (license.status === "EXPIRED" || !license.currentlyUsable) {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "EXPIRED",
        license,
        daysRemaining: 0,
        reasonMessage:
          "Your laboratory subscription has expired. Please contact your organization administrator to renew the subscription.",
        canManageSubscription: false,
        subscriptionManagementPath: "/lab-staff/dashboard",
        isSuperAdmin: false,
        isOrgAdmin: false,
        isLabStaff: true,
        refetch,
      };
    }

    if (license.status === "DEACTIVATED") {
      return {
        isEligible: false,
        isLoading: false,
        expiryStatus: "DEACTIVATED",
        license,
        daysRemaining: 0,
        reasonMessage:
          "Your laboratory subscription has been deactivated. Please contact your organization administrator.",
        canManageSubscription: false,
        subscriptionManagementPath: "/lab-staff/dashboard",
        isSuperAdmin: false,
        isOrgAdmin: false,
        isLabStaff: true,
        refetch,
      };
    }

    return {
      isEligible: true,
      isLoading: false,
      expiryStatus: "ACTIVE",
      license,
      daysRemaining: 99,
      reasonMessage: null,
      canManageSubscription: false,
      subscriptionManagementPath: "/lab-staff/dashboard",
      isSuperAdmin: false,
      isOrgAdmin: false,
      isLabStaff: true,
      refetch,
    };
  }

  return {
    isEligible: false,
    isLoading: false,
    expiryStatus: "UNKNOWN",
    license: null,
    daysRemaining: 0,
    reasonMessage: "Unauthorized operational role.",
    canManageSubscription: false,
    subscriptionManagementPath: "/",
    isSuperAdmin: false,
    isOrgAdmin: false,
    isLabStaff: false,
    refetch: async () => {},
  };
}
