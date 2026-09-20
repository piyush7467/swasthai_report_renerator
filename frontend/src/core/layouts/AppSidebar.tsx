import { ChevronDown, ChevronRight, LogOut } from "lucide-react";
import { useState, useEffect } from "react";
import { NavLink, useLocation } from "react-router-dom";

import { Separator } from "@/components/ui/separator";

import { useAuth } from "../auth/AuthContext";
import {
  getDashboardPath,
  getNavigationForRole,
} from "./navigation";

interface AppSidebarProps {
  onNavigate?: () => void;
}

export function AppSidebar({
  onNavigate,
}: AppSidebarProps) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({
    Tests: true,
  });

  // Automatically expand if currently navigating within a parent's route
  useEffect(() => {
    if (location.pathname.startsWith("/super-admin/tests")) {
      setExpandedItems((prev) => ({ ...prev, Tests: true }));
    }
  }, [location.pathname]);

  if (!user) {
    return null;
  }

  const navigationItems =
    getNavigationForRole(user.role);

  const toggleExpanded = (label: string) => {
    setExpandedItems((prev) => ({
      ...prev,
      [label]: !prev[label],
    }));
  };

  const handleLogout = async () => {
    await logout();
  };

  return (
    <aside className="flex h-full w-64 flex-col border-r border-slate-200 bg-white">
      {/* Brand */}
      <div className="flex h-16 items-center px-5">
        <div>
          <div className="text-lg font-bold tracking-tight text-slate-900">
            SwasthAI
          </div>

          <div className="text-xs text-slate-500">
            Report Generator
          </div>
        </div>
      </div>

      <Separator />

      {/* Navigation */}
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {navigationItems.map((item) => {
          const Icon = item.icon;
          const hasChildren = Boolean(item.children && item.children.length > 0);
          const isExpanded = expandedItems[item.label] ?? false;
          const isParentActive =
            location.pathname === item.href ||
            (item.children &&
              item.children.some((child) =>
                child.href === item.href
                  ? location.pathname === child.href ||
                    (child.href === "/super-admin/tests" &&
                      (location.pathname.startsWith("/super-admin/tests/new") ||
                        (location.pathname.startsWith("/super-admin/tests/") &&
                          !location.pathname.startsWith("/super-admin/tests/categories") &&
                          !location.pathname.startsWith("/super-admin/tests/assignments"))))
                  : location.pathname.startsWith(child.href),
              ));

          if (hasChildren) {
            return (
              <div key={item.label} className="space-y-1">
                <button
                  type="button"
                  onClick={() => toggleExpanded(item.label)}
                  className={[
                    "flex w-full items-center justify-between rounded-lg px-3 py-2.5",
                    "text-sm font-medium transition-colors",
                    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400",
                    isParentActive
                      ? "text-slate-900 font-semibold"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                  ].join(" ")}
                >
                  <div className="flex items-center gap-3">
                    <Icon className="h-4 w-4 shrink-0" />
                    <span>{item.label}</span>
                  </div>
                  {isExpanded ? (
                    <ChevronDown className="h-4 w-4 text-slate-400" />
                  ) : (
                    <ChevronRight className="h-4 w-4 text-slate-400" />
                  )}
                </button>

                {isExpanded && item.children && (
                  <div className="ml-7 space-y-1 border-l border-slate-200 pl-2">
                    {item.children.map((child) => {
                      const isChildActive =
                        child.href === "/super-admin/tests"
                          ? location.pathname === "/super-admin/tests" ||
                            location.pathname === "/super-admin/tests/" ||
                            location.pathname.startsWith("/super-admin/tests/new") ||
                            (location.pathname.startsWith("/super-admin/tests/") &&
                              !location.pathname.startsWith("/super-admin/tests/categories") &&
                              !location.pathname.startsWith("/super-admin/tests/assignments"))
                          : location.pathname.startsWith(child.href);

                      return (
                        <NavLink
                          key={child.href}
                          to={child.href}
                          onClick={onNavigate}
                          className={[
                            "flex items-center rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors",
                            isChildActive
                              ? "bg-slate-100 font-semibold text-slate-900"
                              : "text-slate-500 hover:bg-slate-50 hover:text-slate-900",
                          ].join(" ")}
                        >
                          {child.label}
                        </NavLink>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          }

          return (
            <NavLink
              key={item.href}
              to={item.href}
              end={
                item.href ===
                getDashboardPath(user.role)
              }
              onClick={onNavigate}
              className={({ isActive }) =>
                [
                  "flex items-center gap-3 rounded-lg px-3 py-2.5",
                  "text-sm font-medium transition-colors",
                  "focus-visible:outline-none focus-visible:ring-2",
                  "focus-visible:ring-slate-400",
                  isActive
                    ? "bg-slate-100 text-slate-900"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                ].join(" ")
              }
            >
              <Icon className="h-4 w-4 shrink-0" />

              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <Separator />

      {/* User + Logout */}
      <div className="p-3">
        <div className="rounded-lg bg-slate-50 p-3">
          <p className="text-xs text-slate-500">
            Signed in as
          </p>

          <p className="mt-1 truncate text-sm font-medium text-slate-900">
            {user.name}
          </p>

          <p className="truncate text-xs text-slate-500">
            {user.email}
          </p>

          <p className="mt-1.5 inline-block rounded bg-slate-200/80 px-1.5 py-0.5 text-[11px] font-medium text-slate-700">
            {user.role.replace("_", " ")}
          </p>

          {user.organizationRefId && (
            <p
              className="mt-1.5 truncate text-[11px] font-mono text-slate-500 border-t border-slate-200/80 pt-1"
              title={user.organizationRefId}
            >
              Org: {user.organizationRefId}
            </p>
          )}
        </div>

        {/* Logout */}
        <button
          type="button"
          onClick={() => {
            void handleLogout();
          }}
          className="mt-2 flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600 transition-colors hover:bg-red-50 hover:text-red-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-300"
        >
          <LogOut className="h-4 w-4 shrink-0" />

          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}