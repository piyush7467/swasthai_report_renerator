import type { LucideIcon } from "lucide-react";
import {
  FileText,
  FlaskConical,
  LayoutDashboard,
  Settings,
  ShieldCheck,
  Users,
} from "lucide-react";

import type { UserRole } from "../auth/authTypes";

export interface NavigationSubItem {
  label: string;
  href: string;
}

export interface NavigationItem {
  label: string;
  href: string;
  icon: LucideIcon;
  children?: NavigationSubItem[];
}

const navigationByRole: Record<
  UserRole,
  NavigationItem[]
> = {
  SUPER_ADMIN: [
    {
      label: "Dashboard",
      href: "/super-admin",
      icon: LayoutDashboard,
    },
    {
      label: "Organizations",
      href: "/super-admin/organizations",
      icon: Users,
    },
    {
      label: "Users",
      href: "/super-admin/users",
      icon: Users,
    },
    {
      label: "Reports",
      href: "/super-admin/reports",
      icon: FileText,
      children: [
        {
          label: "Overview",
          href: "/super-admin/reports",
        },
        {
          label: "Activity",
          href: "/super-admin/reports/activity",
        },
        {
          label: "Audit",
          href: "/super-admin/reports/audit",
        },
      ],
    },
    {
      label: "Tests",
      href: "/super-admin/tests",
      icon: FlaskConical,
      children: [
        {
          label: "Test Catalog",
          href: "/super-admin/tests",
        },
        {
          label: "Categories",
          href: "/super-admin/tests/categories",
        },
        {
          label: "Parameters",
          href: "/super-admin/tests/parameters",
        },
        {
          label: "Assignments",
          href: "/super-admin/tests/assignments",
        },
      ],
    },
    {
      label: "Licensing",
      href: "/super-admin/licensing/plans",
      icon: ShieldCheck,
      children: [
        {
          label: "Plans",
          href: "/super-admin/licensing/plans",
        },
        {
          label: "Organization Licenses",
          href: "/super-admin/licensing/licenses",
        },
      ],
    },
    {
      label: "Settings",
      href: "/super-admin/settings",
      icon: Settings,
    },
  ],

  ORG_ADMIN: [
    {
      label: "Dashboard",
      href: "/org-admin",
      icon: LayoutDashboard,
    },
    {
      label: "Patients",
      href: "/org-admin/patients",
      icon: Users,
    },
    {
      label: "Reports",
      href: "/org-admin/reports",
      icon: FileText,
    },
    {
      label: "Lab Staff",
      href: "/org-admin/staff",
      icon: Users,
    },
    {
      label: "Tests",
      href: "/org-admin/tests",
      icon: FlaskConical,
    },
    {
      label: "Settings",
      href: "/org-admin/settings",
      icon: Settings,
    },
  ],

  LAB_STAFF: [
    {
      label: "Dashboard",
      href: "/lab-staff",
      icon: LayoutDashboard,
    },
    {
      label: "Patients",
      href: "/lab-staff/patients",
      icon: Users,
    },
    {
      label: "Reports",
      href: "/lab-staff/reports",
      icon: FileText,
    },
    {
      label: "Tests",
      href: "/lab-staff/tests",
      icon: FlaskConical,
    },
  ],
};

export function getNavigationForRole(
  role: UserRole,
): NavigationItem[] {
  return navigationByRole[role];
}

export function getDashboardPath(
  role: UserRole,
): string {
  switch (role) {
    case "SUPER_ADMIN":
      return "/super-admin";

    case "ORG_ADMIN":
      return "/org-admin";

    case "LAB_STAFF":
      return "/lab-staff";
  }
}