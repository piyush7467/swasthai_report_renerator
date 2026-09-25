import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  ShieldCheck,
  Sparkles,
  RefreshCw,
  AlertCircle,
  ArrowUpRight,
} from "lucide-react";
import { useLicenseOverview } from "../hooks/useOrgLicense";
import { LicenseOverviewCard } from "../components/LicenseOverviewCard";
import { LicenseUsageSection } from "../components/LicenseUsageSection";
import { PlanDetailsCard } from "../components/PlanDetailsCard";
import { UpgradeRequestHistoryTable } from "../components/UpgradeRequestHistoryTable";
import { RequestUpgradeModal } from "../components/RequestUpgradeModal";
import { UpgradeRequestDetailsModal } from "../components/UpgradeRequestDetailsModal";
import type { PlanUpgradeRequestResponse } from "../types/licenseTypes";

export default function OrganizationLicensePage() {
  const { data: overview, isLoading, isError, error, refetch, isFetching } =
    useLicenseOverview();

  const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] =
    useState<PlanUpgradeRequestResponse | null>(null);

  if (isLoading) {
    return (
      <main className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 animate-pulse">
          <div className="space-y-2">
            <div className="h-6 bg-slate-200 rounded w-48"></div>
            <div className="h-4 bg-slate-100 rounded w-80"></div>
          </div>
          <div className="h-9 bg-slate-200 rounded w-36"></div>
        </div>

        <div className="space-y-6 animate-pulse">
          <div className="h-44 bg-slate-100 rounded-xl"></div>
          <div className="h-64 bg-slate-100 rounded-xl"></div>
          <div className="h-48 bg-slate-100 rounded-xl"></div>
        </div>
      </main>
    );
  }

  if (isError || !overview) {
    return (
      <main className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
        <Card className="p-8 text-center border-slate-200 bg-white max-w-lg mx-auto space-y-4">
          <div className="p-3 rounded-full bg-rose-50 text-rose-600 inline-block border border-rose-100">
            <AlertCircle className="h-6 w-6" />
          </div>
          <h2 className="text-lg font-bold text-slate-900">
            Unable to Load Subscription Details
          </h2>
          <p className="text-xs text-slate-500">
            {(error as { response?: { data?: { message?: string } } })?.response
              ?.data?.message ||
              "There was an error communicating with the licensing service. Please try again."}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            className="gap-2 text-xs"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Retry
          </Button>
        </Card>
      </main>
    );
  }

  return (
    <main className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              License & Subscription
            </h1>
            <div className="p-1 rounded-md bg-teal-50 text-[#0F766E] border border-teal-100">
              <ShieldCheck className="h-4 w-4" />
            </div>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Manage your organization subscription plan, resource utilization, and team capacity limits.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching}
            className="h-8 text-xs text-slate-600 hover:text-slate-900 gap-1.5 cursor-pointer"
          >
            <RefreshCw
              className={`h-3.5 w-3.5 ${isFetching ? "animate-spin text-teal-600" : ""}`}
            />
            Refresh
          </Button>

          <Button
            onClick={() => setIsUpgradeModalOpen(true)}
            size="sm"
            className="h-8 text-xs bg-[#0F766E] hover:bg-[#0d655e] text-white shadow-2xs gap-1.5 cursor-pointer"
          >
            <Sparkles className="h-3.5 w-3.5" />
            Request Upgrade
            <ArrowUpRight className="h-3 w-3 opacity-80" />
          </Button>
        </div>
      </div>

      {/* Main License Overview Card */}
      <LicenseOverviewCard
        overview={overview}
        onRequestUpgrade={() => setIsUpgradeModalOpen(true)}
      />

      {/* Resource Utilization & Capacity Limits */}
      <LicenseUsageSection
        staffUsage={overview.staffUsage}
        patientUsage={overview.patientUsage}
        reportUsage={overview.reportUsage}
      />

      {/* Plan Details & Features */}
      <PlanDetailsCard license={overview.license} />

      {/* Upgrade Request History & Tracking */}
      <UpgradeRequestHistoryTable
        onSelectRequest={(req) => setSelectedRequest(req)}
      />

      {/* Upgrade Request Modal */}
      <RequestUpgradeModal
        isOpen={isUpgradeModalOpen}
        onClose={() => setIsUpgradeModalOpen(false)}
        onSuccess={() => void refetch()}
      />

      {/* Request Details Modal */}
      <UpgradeRequestDetailsModal
        request={selectedRequest}
        isOpen={Boolean(selectedRequest)}
        onClose={() => setSelectedRequest(null)}
      />
    </main>
  );
}
