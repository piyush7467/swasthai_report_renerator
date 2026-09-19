import {
  Building2,
  LogOut,
  Menu,
  UserCircle,
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
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
      {/* Mobile menu button */}
      <button
        type="button"
        onClick={onMenuClick}
        className="inline-flex h-9 w-9 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400 lg:hidden"
        aria-label="Open navigation"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Desktop title & tenant indicator */}
      <div className="hidden items-center gap-3 lg:flex">
        <p className="text-sm font-semibold text-slate-900">
          SwasthAI Report Generator
        </p>

        {user.organizationRefId && (
          <span className="inline-flex items-center gap-1 rounded-md border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-xs font-mono text-slate-600">
            <Building2 className="h-3 w-3 text-slate-400" />
            Org: {user.organizationRefId}
          </span>
        )}
      </div>

      {/* User menu */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
          >
            <UserCircle className="h-8 w-8 text-slate-500" />

            <div className="hidden text-left sm:block">
              <p className="max-w-44 truncate text-sm font-medium text-slate-900">
                {user.name}
              </p>

              <p className="text-xs text-slate-500">
                {user.role.replace("_", " ")}
              </p>
            </div>
          </button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          className="w-60"
        >
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
    </header>
  );
}