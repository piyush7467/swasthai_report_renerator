import { LogOut } from "lucide-react";
import { NavLink } from "react-router-dom";

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

  if (!user) {
    return null;
  }

  const navigationItems =
    getNavigationForRole(user.role);

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