import { Link } from "react-router-dom";
import {
  Bell,
  Building2,
  FlaskConical,
  LogOut,
  Menu,
  Search,
  Settings,
  ShieldAlert,
  ShieldCheck,
  User,
  Users,
} from "lucide-react";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { useAuth } from "../auth/AuthContext";
import logoImg from "@/assets/logo.png";

interface AppTopbarProps {
  onMenuClick: () => void;
}

export function AppTopbar({
  onMenuClick,
}: AppTopbarProps) {
  const { user, logout } = useAuth();

  if (!user) {
    return null;
  }

  const handleLogout = async () => {
    await logout();
  };

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-100 bg-white px-4 sm:px-6">
      {/* Mobile brand header */}
      <div className="flex items-center gap-2 lg:hidden">
        <button
          type="button"
          onClick={onMenuClick}
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-50 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500"
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="flex items-center gap-1.5">
          <img src={logoImg} alt="SwasthAI Logo" className="h-6 w-6 object-contain" />
          <span className="text-sm font-bold text-slate-900 tracking-tight">
            Swasth<span className="text-teal-600">AI</span>
          </span>
        </div>
      </div>

      {/* Desktop title & tenant indicator */}
      <div className="hidden items-center gap-2.5 lg:flex">
        <img src={logoImg} alt="SwasthAI Logo" className="h-6 w-6 object-contain" />
        <p className="text-sm font-bold tracking-tight text-slate-900">
          Swasth<span className="text-teal-600">AI</span>{" "}
          <span className="font-normal text-slate-500 text-xs">| Report Generator</span>
        </p>

        {user.organizationRefId && (
          <span className="inline-flex items-center gap-1 rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-mono text-slate-600">
            <Building2 className="h-3 w-3 text-slate-400" />
            Org: {user.organizationRefId}
          </span>
        )}
      </div>

      {/* Center Search bar (as in mockup) */}
      <div className="hidden md:flex items-center flex-1 max-w-sm ml-auto mr-4">
        <div className="relative w-full">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-3.5 text-slate-400" />
          <input
            type="text"
            placeholder="Search organizations, reports, users..."
            className="w-full pl-8 pr-12 py-1.5 text-xs bg-slate-50/80 border border-slate-200/70 rounded-lg text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-teal-500 focus:bg-white transition-all"
            readOnly
          />
          <kbd className="absolute right-2 top-1/2 -translate-y-1/2 text-[9px] font-mono text-slate-400 bg-white border border-slate-200 px-1 py-0.5 rounded">
            Ctrl K
          </kbd>
        </div>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-2">
        {/* Notifications Bell */}
        <button
          type="button"
          className="relative p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-50 transition-colors cursor-pointer"
          aria-label="Notifications"
        >
          <Bell className="size-4" />
          <span className="absolute top-1.5 right-1.5 size-1.5 rounded-full bg-teal-500" />
        </button>

        {/* User menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex items-center gap-2 rounded-lg p-1 hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 cursor-pointer"
            >
              <div className="flex size-8 items-center justify-center rounded-full bg-slate-900 text-white font-semibold">
                <User className="size-4" />
              </div>

              <div className="hidden text-left sm:block">
                <p className="max-w-40 truncate text-xs font-semibold text-slate-900 leading-tight">
                  {user.name || "System Super Admin"}
                </p>
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  {user.role.replace("_", " ")}
                </p>
              </div>
            </button>
          </DropdownMenuTrigger>

          <DropdownMenuContent align="end" className="w-60">
            <DropdownMenuLabel>
              <div className="space-y-1">
                <p className="truncate text-sm font-semibold text-slate-900">
                  {user.name}
                </p>

                <p className="truncate text-xs font-normal text-slate-500">
                  {user.email}
                </p>

                <div className="pt-1">
                  <span className="inline-block rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-700">
                    {user.role}
                  </span>
                </div>

                {user.organizationRefId && (
                  <p className="truncate pt-1 text-[11px] font-mono text-slate-500">
                    Org: {user.organizationRefId}
                  </p>
                )}
              </div>
            </DropdownMenuLabel>

            {user.role === "ORG_ADMIN" && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/org-admin/settings" className="flex items-center">
                    <Settings className="mr-2 h-4 w-4 text-slate-500" />
                    Settings & Letterhead
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/org-admin/license" className="flex items-center">
                    <ShieldCheck className="mr-2 h-4 w-4 text-slate-500" />
                    License & Subscription
                  </Link>
                </DropdownMenuItem>
              </>
            )}

            {user.role === "LAB_STAFF" && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/lab-staff/tests" className="flex items-center">
                    <FlaskConical className="mr-2 h-4 w-4 text-slate-500" />
                    Assigned Tests & Ranges
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/lab-staff/patients" className="flex items-center">
                    <Users className="mr-2 h-4 w-4 text-slate-500" />
                    Patient Directory
                  </Link>
                </DropdownMenuItem>
              </>
            )}

            {user.role === "SUPER_ADMIN" && (
              <>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/super-admin/settings" className="flex items-center">
                    <Settings className="mr-2 h-4 w-4 text-slate-500" />
                    System Settings
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild className="cursor-pointer">
                  <Link to="/super-admin/reports/audit" className="flex items-center">
                    <ShieldAlert className="mr-2 h-4 w-4 text-slate-500" />
                    Audit Logs
                  </Link>
                </DropdownMenuItem>
              </>
            )}

            <DropdownMenuSeparator />

            <DropdownMenuItem
              onSelect={() => {
                void handleLogout();
              }}
              className="cursor-pointer text-red-600 focus:text-red-600"
            >
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}