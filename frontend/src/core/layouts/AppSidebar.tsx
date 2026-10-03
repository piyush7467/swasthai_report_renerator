import { ChevronDown, ChevronRight, LogOut, User } from "lucide-react";
import { useState, useEffect } from "react";
import { NavLink, useLocation } from "react-router-dom";
import logoImg from "@/assets/logo.png";

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
    Reports: true,
    Tests: true,
    Licensing: true,
  });

  useEffect(() => {
    if (location.pathname.startsWith("/super-admin/reports")) {
      setExpandedItems((prev) => (prev.Reports ? prev : { ...prev, Reports: true }));
    } else if (location.pathname.startsWith("/super-admin/tests")) {
      setExpandedItems((prev) => (prev.Tests ? prev : { ...prev, Tests: true }));
    } else if (location.pathname.startsWith("/super-admin/licensing")) {
      setExpandedItems((prev) => (prev.Licensing ? prev : { ...prev, Licensing: true }));
    }
  }, [location.pathname]);

  if (!user) {
    return null;
  }

  const navigationItems = getNavigationForRole(user.role);

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
    <aside className="flex h-full w-60 flex-col border-r border-slate-100 bg-white select-none">
      {/* Brand Header */}
      <div className="flex items-center gap-3 px-6 py-5">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-50 border border-teal-100/70 p-1 shadow-2xs">
          <img src={logoImg} alt="SwasthAI Logo" className="h-full w-full object-contain" />
        </div>
        <div>
          <div className="text-base font-bold tracking-tight text-slate-900 leading-none">
            Swasth<span className="text-teal-600">AI</span>
          </div>
          <div className="text-[11px] text-slate-400 font-medium mt-1">
            Report Generator
          </div>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
        {navigationItems.map((item) => {
          const Icon = item.icon;
          const hasChildren = Boolean(item.children && item.children.length > 0);
          const isExpanded = expandedItems[item.label] ?? false;
          const isParentActive =
            location.pathname === item.href ||
            Boolean(
              item.children?.some((child) =>
                child.href === item.href
                  ? location.pathname === child.href ||
                    (child.href === "/super-admin/reports" &&
                      location.pathname.startsWith("/super-admin/reports")) ||
                    (child.href === "/super-admin/tests" &&
                      (location.pathname.startsWith("/super-admin/tests/new") ||
                        (location.pathname.startsWith("/super-admin/tests/") &&
                          !location.pathname.startsWith("/super-admin/tests/categories") &&
                          !location.pathname.startsWith("/super-admin/tests/assignments") &&
                          !location.pathname.startsWith("/super-admin/tests/parameters"))))
                  : child.href === "/super-admin/licensing/plans"
                  ? location.pathname.startsWith("/super-admin/licensing/plans")
                  : location.pathname.startsWith(child.href)
              )
            );

          if (hasChildren) {
            return (
              <div key={item.label} className="space-y-0.5">
                <button
                  type="button"
                  onClick={() => toggleExpanded(item.label)}
                  className={[
                    "flex w-full items-center justify-between rounded-xl px-3.5 py-2.5",
                    "text-sm font-medium transition-colors cursor-pointer",
                    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500",
                    isParentActive
                      ? "text-slate-900 font-semibold"
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                  ].join(" ")}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`h-4 w-4 shrink-0 ${isParentActive ? "text-teal-600" : "text-slate-400"}`} />
                    <span>{item.label}</span>
                  </div>
                  {isExpanded ? (
                    <ChevronDown className="h-3.5 w-3.5 text-slate-400" />
                  ) : (
                    <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                  )}
                </button>

                {isExpanded && item.children && (
                  <div className="ml-6 space-y-0.5 border-l border-slate-100 pl-3.5 py-1">
                    {item.children.map((child) => {
                      const isChildActive =
                        child.href === "/super-admin/tests"
                          ? location.pathname === "/super-admin/tests" ||
                            location.pathname === "/super-admin/tests/" ||
                            location.pathname.startsWith("/super-admin/tests/new") ||
                            (location.pathname.startsWith("/super-admin/tests/") &&
                              !location.pathname.startsWith("/super-admin/tests/categories") &&
                              !location.pathname.startsWith("/super-admin/tests/assignments") &&
                              !location.pathname.startsWith("/super-admin/tests/parameters"))
                          : child.href === "/super-admin/licensing/plans"
                          ? location.pathname.startsWith("/super-admin/licensing/plans")
                          : location.pathname.startsWith(child.href);

                      return (
                        <NavLink
                          key={child.href}
                          to={child.href}
                          onClick={onNavigate}
                          className={[
                            "flex items-center rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors",
                            isChildActive
                              ? "bg-teal-50/70 font-semibold text-teal-800"
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
              end={item.href === getDashboardPath(user.role)}
              onClick={onNavigate}
              className={({ isActive }) =>
                [
                  "flex items-center gap-3 rounded-xl px-3.5 py-2.5",
                  "text-sm font-medium transition-colors",
                  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500",
                  isActive
                    ? "bg-[#e6f4f1] text-[#0d766e] font-semibold"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                ].join(" ")
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={`h-4 w-4 shrink-0 ${
                      isActive ? "text-[#0d766e]" : "text-slate-400"
                    }`}
                  />
                  <span>{item.label}</span>
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* User Info & Logout at bottom */}
      <div className="border-t border-slate-100 p-4">
        <div className="flex items-center gap-3 px-1 py-1">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-900 text-white font-semibold">
            <User className="h-4 w-4" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold text-slate-900 leading-tight">
              {user.name || "System Super Admin"}
            </p>
            <p className="truncate text-[11px] text-slate-400">
              {user.email || "admin@swasthai.com"}
            </p>
            <span className="inline-block mt-0.5 rounded bg-slate-100 px-1.5 py-0.5 text-[9px] font-bold text-slate-500 tracking-wider uppercase">
              {user.role.replace("_", " ")}
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => {
            void handleLogout();
          }}
          className="mt-3 flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-xs font-medium text-slate-500 transition-colors hover:bg-red-50 hover:text-red-600 cursor-pointer"
        >
          <LogOut className="h-3.5 w-3.5" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}