import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  ArrowLeft,
  Building2,
  Check,
  Copy,
  Edit,
  FileText,
  LayoutDashboard,
  RefreshCw,
  SlidersHorizontal,
  Users,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import type { OrganizationResponse } from "../types/organizationTypes";
import { OrganizationStatusBadge } from "./OrganizationStatusBadge";
import { EditOrganizationDialog } from "./EditOrganizationDialog";
import { ChangeOrganizationStatusDialog } from "./ChangeOrganizationStatusDialog";

interface OrganizationHeaderNavProps {
  organization: OrganizationResponse | undefined;
  isLoading: boolean;
  onRefresh: () => void;
  isRefreshing?: boolean;
}

export function OrganizationHeaderNav({
  organization,
  isLoading,
  onRefresh,
  isRefreshing,
}: OrganizationHeaderNavProps) {
  const location = useLocation();
  const [copied, setCopied] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);

  const refId = organization?.refId ?? "";

  const copyRefId = () => {
    if (refId) {
      void navigator.clipboard.writeText(refId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const navTabs = [
    {
      label: "Overview",
      href: `/super-admin/organizations/${refId}`,
      icon: LayoutDashboard,
      isActive:
        location.pathname === `/super-admin/organizations/${refId}` ||
        location.pathname === `/super-admin/organizations/${refId}/`,
    },
    {
      label: "Users",
      href: `/super-admin/organizations/${refId}/users`,
      icon: Users,
      isActive: location.pathname.startsWith(
        `/super-admin/organizations/${refId}/users`,
      ),
    },
    {
      label: "Profile & Branding",
      href: `/super-admin/organizations/${refId}/profile`,
      icon: FileText,
      isActive: location.pathname.startsWith(
        `/super-admin/organizations/${refId}/profile`,
      ),
    },
  ];

  return (
    <div className="space-y-4">
      {/* Top Bar: Back & Actions */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-600 hover:text-slate-900"
          >
            <Link to="/super-admin/organizations">
              <ArrowLeft className="mr-1.5 h-4 w-4" />
              Organizations
            </Link>
          </Button>
          <span className="text-slate-300">/</span>
          {isLoading ? (
            <Skeleton className="h-5 w-40" />
          ) : (
            <span className="text-sm font-semibold text-slate-800 truncate max-w-xs">
              {organization?.name ?? refId}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onRefresh}
            disabled={isRefreshing || isLoading}
            className="text-slate-600 hover:text-slate-900"
          >
            <RefreshCw
              className={`mr-1.5 h-3.5 w-3.5 ${isRefreshing ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>

          {organization && (
            <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setEditDialogOpen(true)}
                className="text-slate-700 hover:text-slate-900"
              >
                <Edit className="mr-1.5 h-3.5 w-3.5" />
                Edit
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setStatusDialogOpen(true)}
                className="text-slate-700 hover:text-slate-900"
              >
                <SlidersHorizontal className="mr-1.5 h-3.5 w-3.5" />
                Status
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Organization Identity Header Card */}
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        {isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-7 w-64" />
            <Skeleton className="h-4 w-48" />
          </div>
        ) : organization ? (
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="space-y-1.5">
              <div className="flex flex-wrap items-center gap-2.5">
                <Building2 className="h-6 w-6 text-slate-700 shrink-0" />
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                  {organization.name}
                </h1>
                <OrganizationStatusBadge status={organization.status} />
                <Badge
                  variant="outline"
                  className="font-mono text-xs text-slate-600"
                >
                  {organization.code}
                </Badge>
              </div>

              <div className="flex items-center gap-2 text-xs font-mono text-slate-500">
                <span>Ref ID: {organization.refId}</span>
                <button
                  type="button"
                  onClick={copyRefId}
                  className="inline-flex items-center text-slate-400 hover:text-slate-700 focus:outline-none"
                  title="Copy Reference ID"
                >
                  {copied ? (
                    <Check className="h-3.5 w-3.5 text-emerald-600" />
                  ) : (
                    <Copy className="h-3.5 w-3.5" />
                  )}
                </button>
              </div>
            </div>
          </div>
        ) : null}

        {/* Sub-Navigation Tabs */}
        {organization && (
          <div className="mt-5 border-t border-slate-100 pt-3">
            <nav className="flex items-center gap-1">
              {navTabs.map((tab) => {
                const Icon = tab.icon;
                return (
                  <Link
                    key={tab.href}
                    to={tab.href}
                    className={`flex items-center gap-2 rounded-lg px-3.5 py-2 text-xs font-semibold transition-colors ${
                      tab.isActive
                        ? "bg-slate-900 text-white shadow-xs"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                    }`}
                  >
                    <Icon className="h-3.5 w-3.5 shrink-0" />
                    <span>{tab.label}</span>
                  </Link>
                );
              })}
            </nav>
          </div>
        )}
      </div>

      {/* Dialogs for Small Focused Actions */}
      {organization && (
        <>
          <EditOrganizationDialog
            organization={organization}
            open={editDialogOpen}
            onOpenChange={setEditDialogOpen}
          />
          <ChangeOrganizationStatusDialog
            organization={organization}
            open={statusDialogOpen}
            onOpenChange={setStatusDialogOpen}
          />
        </>
      )}
    </div>
  );
}
