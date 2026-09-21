import { useState } from "react";
import {
  Calendar,
  CheckCircle2,
  Clock,
  KeyRound,
  PauseCircle,
  PlayCircle,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Hourglass,
} from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import type { LicenseResponse } from "../types/licensingTypes";
import { getLicenseExpiryInfo } from "../types/licensingTypes";
import { LicenseStatusBadge } from "./LicenseStatusBadge";
import { ConfirmDeactivateLicenseDialog } from "./ConfirmDeactivateLicenseDialog";
import { ConfirmReactivateLicenseDialog } from "./ConfirmReactivateLicenseDialog";
import {
  useDeactivateLicenseMutation,
  useReactivateLicenseMutation,
} from "../hooks/useLicensing";

interface OrganizationLicenseCardProps {
  license: LicenseResponse | null;
  organizationRefId: string;
  organizationName?: string;
  isLoading?: boolean;
  onActivateClick?: () => void;
  onRenewClick?: () => void;
  onRefresh?: () => void;
}

export function OrganizationLicenseCard({
  license,
  organizationRefId,
  organizationName,
  isLoading = false,
  onActivateClick,
  onRenewClick,
  onRefresh,
}: OrganizationLicenseCardProps) {
  const [deactivateDialogOpen, setDeactivateDialogOpen] = useState(false);
  const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);

  const deactivateMutation = useDeactivateLicenseMutation();
  const reactivateMutation = useReactivateLicenseMutation();

  if (isLoading) {
    return (
      <Card className="border-slate-200 bg-white">
        <CardHeader>
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-4 w-72" />
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-10 w-32" />
        </CardContent>
      </Card>
    );
  }

  // Case 1: No license found
  if (!license) {
    return (
      <Card className="border-slate-200 bg-white shadow-xs">
        <CardHeader className="pb-4">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
              <ShieldAlert className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-base font-semibold text-slate-900">
                Subscription License
              </CardTitle>
              <CardDescription className="text-xs">
                Tenant licensing and system authorization status
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/50 p-6 text-center space-y-3">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
              <KeyRound className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <h4 className="text-sm font-semibold text-slate-800">
                No License Assigned
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
                This organization currently does not possess a subscription license. Report generation and clinical functions are restricted by the server-side LicenseGuard.
              </p>
            </div>
            {onActivateClick && (
              <div className="pt-2">
                <Button
                  onClick={onActivateClick}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5 shadow-xs"
                >
                  <ShieldCheck className="h-4 w-4" />
                  Activate License
                </Button>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  // Case 2: License exists (ACTIVE, DEACTIVATED, or EXPIRED)
  const isUsable = license.currentlyUsable;
  const isDeactivated = license.status === "DEACTIVATED";
  const isExpired = license.status === "EXPIRED" || (!isUsable && !isDeactivated);
  const expiryInfo = getLicenseExpiryInfo(license.expiresAt, license.startedAt, license.status);

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return "—";
    return new Date(dateStr).toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return "—";
    return new Date(dateStr).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const handleDeactivate = async () => {
    await deactivateMutation.mutateAsync(organizationRefId);
    if (onRefresh) onRefresh();
  };

  const handleReactivate = async () => {
    await reactivateMutation.mutateAsync(organizationRefId);
    if (onRefresh) onRefresh();
  };

  return (
    <>
      <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
        <CardHeader className="border-b border-slate-100 bg-slate-50/50 pb-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2.5">
              <div
                className={`flex h-10 w-10 items-center justify-center rounded-lg ${
                  isUsable
                    ? "bg-emerald-100/70 text-emerald-700"
                    : isDeactivated
                    ? "bg-amber-100/70 text-amber-700"
                    : "bg-rose-100/70 text-rose-700"
                }`}
              >
                {isUsable ? (
                  <ShieldCheck className="h-5 w-5" />
                ) : isDeactivated ? (
                  <PauseCircle className="h-5 w-5" />
                ) : (
                  <ShieldAlert className="h-5 w-5" />
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <CardTitle className="text-base font-semibold text-slate-900">
                    {license.planName}
                  </CardTitle>
                  <Badge variant="outline" className="font-mono text-[11px] bg-white text-slate-700">
                    {license.planCode}
                  </Badge>
                </div>
                <CardDescription className="text-xs">
                  Organization: {organizationName || organizationRefId}
                </CardDescription>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <LicenseStatusBadge
                status={license.status}
                currentlyUsable={license.currentlyUsable}
                showUsability
                expiresAt={license.expiresAt}
                showDaysLeft
              />

              {onRefresh && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={onRefresh}
                  className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900"
                  title="Refresh License"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                </Button>
              )}

              {/* Action 1: Deactivate License (when ACTIVE) */}
              {license.status === "ACTIVE" && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setDeactivateDialogOpen(true)}
                  disabled={deactivateMutation.isPending}
                  className="border-amber-200 text-amber-700 hover:bg-amber-50 hover:text-amber-800 gap-1.5 shadow-xs"
                >
                  <PauseCircle className="h-3.5 w-3.5" />
                  Deactivate
                </Button>
              )}

              {/* Action 2: Reactivate License (when DEACTIVATED) */}
              {license.status === "DEACTIVATED" && (
                <Button
                  size="sm"
                  onClick={() => setReactivateDialogOpen(true)}
                  disabled={reactivateMutation.isPending}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5 shadow-xs"
                >
                  <PlayCircle className="h-3.5 w-3.5" />
                  Reactivate
                </Button>
              )}

              {/* Action 3: Renew License */}
              {onRenewClick && (
                <Button
                  variant={isExpired || expiryInfo?.isExpiringSoon ? "default" : "outline"}
                  size="sm"
                  onClick={onRenewClick}
                  className={
                    isExpired
                      ? "bg-rose-600 hover:bg-rose-700 text-white gap-1.5 shadow-xs"
                      : expiryInfo?.isExpiringSoon
                      ? "bg-amber-600 hover:bg-amber-700 text-white gap-1.5 shadow-xs"
                      : "text-slate-700 hover:text-slate-900 gap-1.5"
                  }
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                  Renew License
                </Button>
              )}
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-6 space-y-6">
          {/* Banner A: If DEACTIVATED (Paused) */}
          {isDeactivated && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-start gap-3">
                <PauseCircle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
                <div className="text-xs text-amber-900 leading-relaxed">
                  <span className="font-semibold block sm:inline">License Deactivated (Suspended):</span>{" "}
                  Reporting operations are temporarily blocked for this organization. The remaining{" "}
                  <strong>{expiryInfo?.daysLeft ?? 0} days</strong> are safely frozen and will resume from the day you reactivate this license.
                </div>
              </div>
              <Button
                size="sm"
                onClick={() => setReactivateDialogOpen(true)}
                disabled={reactivateMutation.isPending}
                className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs h-8 shrink-0 gap-1.5 self-start sm:self-auto"
              >
                <PlayCircle className="h-3.5 w-3.5" />
                Reactivate License
              </Button>
            </div>
          )}

          {/* Banner B: If EXPIRED */}
          {isExpired && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 p-3.5 flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <ShieldAlert className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                <div className="text-xs text-rose-900 leading-relaxed">
                  <span className="font-semibold">Subscription Expired:</span> This organization license is no longer currently usable ({expiryInfo?.label || "expired"}). Diagnostic report generation is temporarily blocked by the backend LicenseGuard until renewed.
                </div>
              </div>
              {onRenewClick && (
                <Button
                  size="sm"
                  onClick={onRenewClick}
                  className="bg-rose-600 hover:bg-rose-700 text-white text-xs h-7 shrink-0"
                >
                  Renew Now
                </Button>
              )}
            </div>
          )}

          {/* Banner C: If usable but expiring soon (<= 30 days) */}
          {isUsable && expiryInfo?.isExpiringSoon && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3.5 flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <Clock className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                <div className="text-xs text-amber-900 leading-relaxed">
                  <span className="font-semibold">Plan Expiring Soon:</span> Only {expiryInfo.daysLeft} {expiryInfo.daysLeft === 1 ? "day" : "days"} remaining on this subscription ({formatDate(license.expiresAt)}). Renew before expiration to ensure uninterrupted diagnostic service.
                </div>
              </div>
              {onRenewClick && (
                <Button
                  size="sm"
                  onClick={onRenewClick}
                  className="bg-amber-600 hover:bg-amber-700 text-white text-xs h-7 shrink-0"
                >
                  Renew License
                </Button>
              )}
            </div>
          )}

          {/* License Details Grid */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
              <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">
                License Status
              </span>
              <div className="flex items-center gap-1.5 pt-0.5">
                <span
                  className={`h-2 w-2 rounded-full ${
                    license.status === "ACTIVE"
                      ? "bg-emerald-500"
                      : license.status === "DEACTIVATED"
                      ? "bg-amber-500"
                      : "bg-rose-500"
                  }`}
                />
                <span className="text-sm font-semibold text-slate-900">
                  {license.status}
                </span>
              </div>
            </div>

            <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
              <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">
                Currently Usable
              </span>
              <div className="flex items-center gap-1.5 pt-0.5">
                {license.currentlyUsable ? (
                  <>
                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                    <span className="text-sm font-semibold text-emerald-700">
                      Yes (Authorized)
                    </span>
                  </>
                ) : (
                  <>
                    <ShieldAlert className="h-4 w-4 text-rose-600" />
                    <span className="text-sm font-semibold text-rose-700">
                      No (Blocked)
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
              <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">
                Started At
              </span>
              <div className="flex items-center gap-1.5 pt-0.5 text-slate-900">
                <Calendar className="h-4 w-4 text-slate-400" />
                <span className="text-sm font-semibold">
                  {formatDate(license.startedAt)}
                </span>
              </div>
            </div>

            <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider block">
                  Expires At
                </span>
                {expiryInfo && (
                  <span
                    className={`text-[11px] font-semibold ${
                      license.status === "DEACTIVATED"
                        ? "text-amber-600"
                        : expiryInfo.isExpired
                        ? "text-rose-600"
                        : expiryInfo.isExpiringSoon
                        ? "text-amber-600"
                        : "text-emerald-700"
                    }`}
                  >
                    {expiryInfo.label}
                  </span>
                )}
              </div>
              <div className="flex items-center gap-1.5 pt-0.5 text-slate-900">
                <Calendar className="h-4 w-4 text-slate-400" />
                <span className="text-sm font-semibold">
                  {formatDate(license.expiresAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Plan Validity & Time Remaining Progress Widget */}
          {expiryInfo && (
            <div
              className={`rounded-xl border p-4 space-y-3 ${
                license.status === "DEACTIVATED"
                  ? "border-amber-300 bg-amber-50/40"
                  : expiryInfo.isExpired
                  ? "border-rose-200 bg-rose-50/30"
                  : expiryInfo.isExpiringSoon
                  ? "border-amber-200 bg-amber-50/30"
                  : "border-slate-200 bg-slate-50/50"
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <div
                    className={`flex h-7 w-7 items-center justify-center rounded-md ${
                      license.status === "DEACTIVATED"
                        ? "bg-amber-100 text-amber-700"
                        : expiryInfo.isExpired
                        ? "bg-rose-100 text-rose-700"
                        : expiryInfo.isExpiringSoon
                        ? "bg-amber-100 text-amber-700"
                        : "bg-emerald-100 text-emerald-700"
                    }`}
                  >
                    <Hourglass className="h-4 w-4" />
                  </div>
                  <div>
                    <span className="text-xs font-semibold text-slate-900 block">
                      {license.status === "DEACTIVATED"
                        ? "License Paused (Remaining Validity Frozen)"
                        : "Plan Duration & Remaining Days"}
                    </span>
                    <span className="text-[11px] text-slate-500">
                      {license.status === "DEACTIVATED"
                        ? "Reactivating will resume the subscription with the remaining frozen days starting from that date."
                        : expiryInfo.isExpired
                        ? "License has lapsed. Reports will be rejected until renewed."
                        : expiryInfo.isExpiringSoon
                        ? "Expiring soon. Extend this subscription for an additional 365 days."
                        : "Authoritative license window managed by backend LicenseGuard."}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-start sm:self-auto">
                  <Badge
                    variant="outline"
                    className={`font-semibold text-xs px-2.5 py-1 ${
                      license.status === "DEACTIVATED"
                        ? "border-amber-300 bg-amber-100 text-amber-800"
                        : expiryInfo.isExpired
                        ? "border-rose-300 bg-rose-100 text-rose-800"
                        : expiryInfo.isUrgent
                        ? "border-rose-400 bg-rose-100 text-rose-800 animate-pulse"
                        : expiryInfo.isExpiringSoon
                        ? "border-amber-300 bg-amber-100 text-amber-800"
                        : "border-emerald-300 bg-emerald-100 text-emerald-800"
                    }`}
                  >
                    <Clock className="mr-1.5 h-3.5 w-3.5 inline" />
                    {license.status === "DEACTIVATED"
                      ? `${expiryInfo.daysLeft} days frozen`
                      : expiryInfo.isExpired
                      ? expiryInfo.label
                      : `${expiryInfo.daysLeft} ${expiryInfo.daysLeft === 1 ? "day" : "days"} left`}
                  </Badge>
                </div>
              </div>

              {/* Visual Lifespan Bar */}
              {typeof expiryInfo.percentElapsed === "number" && (
                <div className="space-y-1.5 pt-1">
                  <div className="w-full bg-slate-200/80 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full transition-all duration-500 ${
                        license.status === "DEACTIVATED"
                          ? "bg-amber-500"
                          : expiryInfo.isExpired
                          ? "bg-rose-500"
                          : expiryInfo.isExpiringSoon
                          ? "bg-amber-500"
                          : "bg-emerald-500"
                      }`}
                      style={{ width: `${expiryInfo.percentElapsed}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono">
                    <span>Started: {formatDate(license.startedAt)}</span>
                    <span className="font-sans font-medium text-slate-700">
                      {expiryInfo.isExpired
                        ? "100% elapsed"
                        : license.status === "DEACTIVATED"
                        ? `${expiryInfo.percentElapsed}% used (paused)`
                        : `${expiryInfo.percentElapsed}% elapsed`}
                    </span>
                    <span>Expires: {formatDate(license.expiresAt)}</span>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Audit & Identifier Metadata */}
          <div className="border-t border-slate-100 pt-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between text-xs text-slate-500 font-mono">
            <div>License Ref: {license.refId}</div>
            <div className="flex items-center gap-4">
              <span>Created: {formatDateTime(license.createdAt)}</span>
              <span>Updated: {formatDateTime(license.updatedAt)}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Confirmation Dialog for Deactivating License */}
      <ConfirmDeactivateLicenseDialog
        license={license}
        organizationName={organizationName}
        open={deactivateDialogOpen}
        onOpenChange={setDeactivateDialogOpen}
        onConfirm={handleDeactivate}
        isLoading={deactivateMutation.isPending}
      />

      {/* Confirmation Dialog for Reactivating License */}
      <ConfirmReactivateLicenseDialog
        license={license}
        organizationName={organizationName}
        open={reactivateDialogOpen}
        onOpenChange={setReactivateDialogOpen}
        onConfirm={handleReactivate}
        isLoading={reactivateMutation.isPending}
      />
    </>
  );
}

export default OrganizationLicenseCard;
