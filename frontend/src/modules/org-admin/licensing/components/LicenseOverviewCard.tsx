import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar, ShieldCheck, Sparkles, AlertCircle, ArrowUpRight } from "lucide-react";
import { LicenseStatusBadge } from "./LicenseStatusBadge";
import type { OrganizationLicenseOverviewResponse } from "../types/licenseTypes";

interface LicenseOverviewCardProps {
  overview: OrganizationLicenseOverviewResponse;
  onRequestUpgrade: () => void;
}

export function LicenseOverviewCard({
  overview,
  onRequestUpgrade,
}: LicenseOverviewCardProps) {
  const { license, daysRemaining, expiryStatus } = overview;

  const formatDate = (isoString?: string) => {
    if (!isoString) return "N/A";
    try {
      return new Date(isoString).toLocaleDateString("en-IN", {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
      {/* Top Banner for Urgent Expiry Notice */}
      {expiryStatus === "EXPIRED" ? (
        <div className="bg-rose-50 border-b border-rose-200 px-5 py-2.5 flex items-center justify-between text-xs text-rose-800">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
            <span>
              <strong>Subscription Expired:</strong> Diagnostic report creation is currently restricted. Please renew or upgrade your license.
            </span>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={onRequestUpgrade}
            className="h-6 text-xs border-rose-300 text-rose-900 hover:bg-rose-100"
          >
            Renew / Upgrade
          </Button>
        </div>
      ) : expiryStatus === "EXPIRING_SOON" ? (
        <div className="bg-amber-50 border-b border-amber-200 px-5 py-2.5 flex items-center justify-between text-xs text-amber-900">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-amber-600" />
            <span>
              <strong>Expiring in {daysRemaining} days:</strong> Your annual subscription expires on {formatDate(license.expiresAt)}.
            </span>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={onRequestUpgrade}
            className="h-6 text-xs border-amber-300 text-amber-900 hover:bg-amber-100"
          >
            Request Renewal
          </Button>
        </div>
      ) : null}

      <div className="p-5 sm:p-6 space-y-6">
        {/* Header Row */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start sm:items-center gap-3">
            <div className="p-2.5 rounded-xl bg-teal-50 border border-teal-100 text-[#0F766E] shadow-2xs">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h2 className="text-xl font-bold tracking-tight text-slate-900">
                  {license.planName || "Current Subscription"}
                </h2>
                <LicenseStatusBadge status={expiryStatus} />
              </div>
              <p className="text-xs text-slate-500 mt-0.5 font-mono">
                License Ref: {license.refId}
              </p>
            </div>
          </div>

          <Button
            onClick={onRequestUpgrade}
            className="bg-[#0F766E] hover:bg-[#0d655e] text-white shadow-2xs gap-1.5 cursor-pointer shrink-0"
          >
            <Sparkles className="h-4 w-4" />
            Request Plan Upgrade
            <ArrowUpRight className="h-3.5 w-3.5 ml-0.5 opacity-80" />
          </Button>
        </div>

        {/* Date and Period Metrics */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
          <div className="p-3.5 rounded-lg bg-slate-50/80 border border-slate-100">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
              Start Date
            </span>
            <div className="mt-1 flex items-center gap-2 text-slate-900 font-semibold text-sm">
              <Calendar className="h-4 w-4 text-slate-400" />
              <span>{formatDate(license.startedAt)}</span>
            </div>
          </div>

          <div className="p-3.5 rounded-lg bg-slate-50/80 border border-slate-100">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
              Expiry Date
            </span>
            <div className="mt-1 flex items-center gap-2 text-slate-900 font-semibold text-sm">
              <Calendar className="h-4 w-4 text-slate-400" />
              <span>{formatDate(license.expiresAt)}</span>
            </div>
          </div>

          <div className="p-3.5 rounded-lg bg-slate-50/80 border border-slate-100">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
              Remaining Validity
            </span>
            <div className="mt-1 flex items-baseline gap-1.5">
              <span
                className={`text-xl font-bold ${
                  daysRemaining <= 14 ? "text-amber-600" : "text-slate-900"
                }`}
              >
                {expiryStatus === "EXPIRED" ? "0" : daysRemaining}
              </span>
              <span className="text-xs text-slate-500">days left</span>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}
