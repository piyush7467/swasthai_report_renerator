import { useState, useEffect, useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  Building2,
  Clock,
  CreditCard,
  ExternalLink,
  RefreshCw,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";

import { useOrganizationsQuery } from "../../hooks/useOrganizations";
import { OrganizationStatusBadge } from "../../components/OrganizationStatusBadge";
import { useOrganizationLicenseQuery } from "../hooks/useLicensing";
import { OrganizationLicenseCard } from "../components/OrganizationLicenseCard";
import { ActivateLicenseDialog } from "../components/ActivateLicenseDialog";
import { RenewLicenseDialog } from "../components/RenewLicenseDialog";
import type { OrganizationResponse } from "../../types/organizationTypes";
import { getLicenseExpiryInfo } from "../types/licensingTypes";

export function OrganizationLicensesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlOrgRefId =
    searchParams.get("organizationRefId") || searchParams.get("org") || "";

  const [activateDialogOpen, setActivateDialogOpen] = useState(false);
  const [renewDialogOpen, setRenewDialogOpen] = useState(false);

  // Fetch organizations list
  const {
    data: orgsData,
    isLoading: orgsLoading,
    refetch: refetchOrgs,
    isFetching: orgsFetching,
  } = useOrganizationsQuery({
    page: 0,
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const organizations: OrganizationResponse[] = useMemo(
    () => orgsData?.content ?? [],
    [orgsData?.content],
  );

  // Determine effective selected organization refId
  const effectiveOrgRefId = useMemo(() => {
    if (urlOrgRefId) return urlOrgRefId;
    if (organizations.length > 0) return organizations[0].refId;
    return "";
  }, [urlOrgRefId, organizations]);

  // Keep URL search param synced
  useEffect(() => {
    if (!urlOrgRefId && organizations.length > 0) {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          next.set("organizationRefId", organizations[0].refId);
          return next;
        },
        { replace: true },
      );
    }
  }, [urlOrgRefId, organizations, setSearchParams]);

  const handleSelectOrg = (refId: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("organizationRefId", refId);
      next.delete("org");
      return next;
    });
  };

  const selectedOrg = organizations.find((o) => o.refId === effectiveOrgRefId);

  // Fetch license for currently selected organization ONLY
  const {
    data: license,
    isLoading: licenseLoading,
    refetch: refetchLicense,
    isFetching: licenseFetching,
  } = useOrganizationLicenseQuery(effectiveOrgRefId);

  const isRefreshing = orgsFetching || licenseFetching;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Organization Licenses
            </h1>
            <Badge variant="outline" className="font-mono text-xs">
              Tenant Licensing
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Manage subscription licenses and authorization for individual tenant healthcare organizations.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              void refetchOrgs();
              void refetchLicense();
            }}
            disabled={isRefreshing}
            className="text-slate-600 hover:text-slate-900"
          >
            <RefreshCw
              className={`mr-1.5 h-3.5 w-3.5 ${
                isRefreshing ? "animate-spin" : ""
              }`}
            />
            Refresh
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5"
          >
            <Link to="/super-admin/licensing/plans">
              <CreditCard className="h-4 w-4" />
              Master Plan Catalog
            </Link>
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5"
          >
            <Link to="/super-admin/licensing/upgrade-requests">
              <Sparkles className="h-4 w-4 text-teal-600" />
              Upgrade Requests
            </Link>
          </Button>
        </div>
      </div>

      {/* Visual Architecture Hierarchy Card */}
      <Card className="border-indigo-100 bg-indigo-50/30 shadow-xs">
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-600">
            <div className="flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded bg-indigo-100 text-indigo-700 font-semibold">
                <CreditCard className="h-4 w-4" />
              </div>
              <span className="font-semibold text-slate-900">1. Master Plan Tier</span>
            </div>

            <div className="flex items-center gap-1 text-slate-400 font-medium">
              <ArrowRight className="h-4 w-4" />
              <span>Assigned to Tenant</span>
              <ArrowRight className="h-4 w-4" />
            </div>

            <div className="flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded bg-blue-100 text-blue-700 font-semibold">
                <Building2 className="h-4 w-4" />
              </div>
              <span className="font-semibold text-slate-900">2. Organization Tenant</span>
            </div>

            <div className="flex items-center gap-1 text-slate-400 font-medium">
              <ArrowRight className="h-4 w-4" />
              <span>License Status</span>
              <ArrowRight className="h-4 w-4" />
            </div>

            <div className="flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded bg-emerald-100 text-emerald-700 font-semibold">
                <ShieldCheck className="h-4 w-4" />
              </div>
              <span className="rounded bg-emerald-100 px-2 py-1 font-semibold text-emerald-800">
                3. LicenseGuard Authorized
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Organization Selection Toolbar */}
      <div className="flex flex-col gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex-1 max-w-md">
            <label className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1.5">
              Select Target Organization
            </label>
            <Select
              value={effectiveOrgRefId}
              onValueChange={handleSelectOrg}
              disabled={orgsLoading}
            >
              <SelectTrigger className="text-sm font-medium bg-white h-10">
                <SelectValue
                  placeholder={
                    orgsLoading ? "Loading organizations..." : "Choose organization..."
                  }
                />
              </SelectTrigger>
              <SelectContent className="max-h-72">
                {organizations.map((org) => (
                  <SelectItem key={org.refId} value={org.refId}>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900 truncate">
                        {org.name}
                      </span>
                      <span className="text-xs font-mono text-slate-400">
                        ({org.code})
                      </span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {selectedOrg && (
            <div className="flex flex-wrap items-center gap-3 pt-2 sm:pt-0">
              <div className="flex items-center gap-2 bg-slate-50 rounded-md px-3 py-2 border border-slate-100">
                <Building2 className="h-4 w-4 text-slate-400" />
                <div className="text-xs">
                  <span className="text-slate-500">Code: </span>
                  <span className="font-mono font-semibold text-slate-800">
                    {selectedOrg.code}
                  </span>
                </div>
                <div className="h-3 w-px bg-slate-200 mx-1" />
                <OrganizationStatusBadge status={selectedOrg.status} />
                {license && (() => {
                  const info = getLicenseExpiryInfo(license.expiresAt);
                  if (!info) return null;
                  return (
                    <>
                      <div className="h-3 w-px bg-slate-200 mx-1" />
                      <span
                        className={`font-semibold flex items-center gap-1 ${
                          info.isExpired
                            ? "text-rose-600"
                            : info.isExpiringSoon
                            ? "text-amber-600"
                            : "text-emerald-700"
                        }`}
                      >
                        <Clock className="h-3 w-3 inline" />
                        {info.isExpired ? info.label : `${info.daysLeft}d left`}
                      </span>
                    </>
                  );
                })()}
              </div>

              <Button
                variant="outline"
                size="sm"
                asChild
                className="text-xs text-indigo-600 hover:text-indigo-700 h-9"
              >
                <Link to={`/super-admin/organizations/${selectedOrg.refId}`}>
                  View Org Profile
                  <ExternalLink className="ml-1.5 h-3.5 w-3.5" />
                </Link>
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Selected Organization's License Card ONLY */}
      {effectiveOrgRefId ? (
        <div className="space-y-4">
          <OrganizationLicenseCard
            license={license ?? null}
            organizationRefId={effectiveOrgRefId}
            organizationName={selectedOrg?.name}
            isLoading={licenseLoading}
            onActivateClick={() => setActivateDialogOpen(true)}
            onRenewClick={() => setRenewDialogOpen(true)}
            onRefresh={() => void refetchLicense()}
          />
        </div>
      ) : orgsLoading ? (
        <div className="p-6 space-y-3">
          <Skeleton className="h-36 w-full rounded-lg" />
        </div>
      ) : (
        <Alert>
          <AlertDescription>
            No organization selected. Please choose an organization from the selector above to view its license.
          </AlertDescription>
        </Alert>
      )}

      {/* Activate License Dialog */}
      {effectiveOrgRefId && (
        <ActivateLicenseDialog
          organizationRefId={effectiveOrgRefId}
          organizationName={selectedOrg?.name}
          open={activateDialogOpen}
          onOpenChange={setActivateDialogOpen}
          onSuccess={() => {
            void refetchLicense();
          }}
        />
      )}

      {/* Renew License Dialog */}
      {effectiveOrgRefId && license && (
        <RenewLicenseDialog
          organizationRefId={effectiveOrgRefId}
          organizationName={selectedOrg?.name}
          currentLicense={license}
          open={renewDialogOpen}
          onOpenChange={setRenewDialogOpen}
          onSuccess={() => {
            void refetchLicense();
          }}
        />
      )}
    </div>
  );
}

export default OrganizationLicensesPage;
