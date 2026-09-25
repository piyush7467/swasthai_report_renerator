import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Users, ShieldAlert, Sparkles, ArrowUpRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { LabStaffSummaryResponse } from "../types/labStaffTypes";

interface LabStaffSummaryCardProps {
  summary: LabStaffSummaryResponse | undefined;
  isLoading: boolean;
}

export function LabStaffSummaryCard({
  summary,
  isLoading,
}: LabStaffSummaryCardProps) {
  const navigate = useNavigate();
  const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);

  if (isLoading || !summary) {
    return (
      <Card className="p-5 border-slate-200 bg-white shadow-xs animate-pulse">
        <div className="h-4 bg-slate-200 rounded w-1/3 mb-4"></div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="h-12 bg-slate-100 rounded"></div>
          <div className="h-12 bg-slate-100 rounded"></div>
          <div className="h-12 bg-slate-100 rounded"></div>
          <div className="h-12 bg-slate-100 rounded"></div>
        </div>
      </Card>
    );
  }

  const { activeStaff, maxLabStaff, remainingSlots, limitReached, planName } =
    summary;
  const isOverLimit = activeStaff > maxLabStaff;
  const usagePercentage = Math.min(
    100,
    Math.round((activeStaff / Math.max(1, maxLabStaff)) * 100)
  );

  return (
    <>
      <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
        <div className="p-5 sm:p-6 space-y-4">
          {/* Top Row: Plan badge & license status */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-md bg-teal-50 text-[#0F766E] border border-teal-100">
                <Users className="h-4 w-4" />
              </div>
              <div>
                <span className="text-sm font-semibold text-slate-800">
                  Staff License Allocation
                </span>
                {planName && (
                  <Badge
                    variant="outline"
                    className="ml-2 bg-slate-50 text-slate-600 border-slate-200 text-xs font-medium"
                  >
                    <Sparkles className="h-3 w-3 mr-1 text-teal-600" />
                    {planName}
                  </Badge>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2">
              {limitReached && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsUpgradeModalOpen(true)}
                  className="h-7 text-xs font-medium text-teal-700 border-teal-200 hover:bg-teal-50 gap-1 cursor-pointer"
                >
                  <Sparkles className="h-3.5 w-3.5" />
                  Upgrade Plan
                </Button>
              )}

              {isOverLimit ? (
                <Badge
                  variant="outline"
                  className="bg-rose-50 text-rose-700 border-rose-200 text-xs font-semibold px-2.5 py-0.5"
                >
                  Over Limit ({activeStaff}/{maxLabStaff})
                </Badge>
              ) : limitReached ? (
                <Badge
                  variant="outline"
                  className="bg-amber-50 text-amber-700 border-amber-200 text-xs font-semibold px-2.5 py-0.5"
                >
                  Limit Reached (0 Slots Left)
                </Badge>
              ) : (
                <Badge
                  variant="outline"
                  className="bg-teal-50 text-[#0F766E] border-teal-200 text-xs font-medium px-2.5 py-0.5"
                >
                  {remainingSlots} {remainingSlots === 1 ? "Seat" : "Seats"} Available
                </Badge>
              )}
            </div>
          </div>

          {/* Metric Cards Row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
            <div className="p-3 rounded-lg bg-slate-50/70 border border-slate-100">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
                Active Staff
              </span>
              <div className="mt-1 flex items-baseline gap-1.5">
                <span className="text-2xl font-bold text-slate-900">
                  {activeStaff} / {maxLabStaff}
                </span>
                <span className="text-xs text-slate-500">active</span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-50/70 border border-slate-100">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
                Available Seats
              </span>
              <div className="mt-1 flex items-baseline gap-1.5">
                <span
                  className={`text-2xl font-bold ${
                    remainingSlots === 0 ? "text-amber-600" : "text-[#0F766E]"
                  }`}
                >
                  {remainingSlots}
                </span>
                <span className="text-xs text-slate-500">remaining</span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-50/70 border border-slate-100">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
                Subscription Plan
              </span>
              <div className="mt-1 flex items-baseline gap-1.5">
                <span className="text-lg font-bold text-slate-900 truncate">
                  {planName || "Standard"}
                </span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-50/70 border border-slate-100">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block">
                Total Roster
              </span>
              <div className="mt-1 flex items-baseline gap-1.5">
                <span className="text-2xl font-bold text-slate-700">
                  {summary.totalStaff}
                </span>
                <span className="text-xs text-slate-500">all time</span>
              </div>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="space-y-1.5 pt-1">
            <div className="flex justify-between text-xs text-slate-500 font-medium">
              <span>License Seat Utilization</span>
              <span>
                {activeStaff} of {maxLabStaff} active seats ({usagePercentage}%)
              </span>
            </div>
            <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
              <div
                className={`h-full transition-all duration-500 rounded-full ${
                  isOverLimit
                    ? "bg-rose-600"
                    : limitReached
                    ? "bg-amber-500"
                    : usagePercentage > 80
                    ? "bg-teal-700"
                    : "bg-[#0F766E]"
                }`}
                style={{ width: `${usagePercentage}%` }}
              />
            </div>
          </div>

          {/* Warning if limit exceeded (e.g. plan downgrade) */}
          {isOverLimit ? (
            <div className="flex items-start justify-between gap-3 p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs">
              <div className="flex items-start gap-2.5">
                <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                <div className="space-y-1">
                  <p className="font-semibold text-rose-900">
                    Plan Seat Limit Exceeded
                  </p>
                  <p className="text-rose-700 leading-relaxed">
                    Your current plan allows {maxLabStaff} active lab staff, but {activeStaff} are currently active.
                  </p>
                  <p className="text-rose-600 font-medium">
                    New staff creation and reactivation are temporarily unavailable.
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsUpgradeModalOpen(true)}
                className="shrink-0 text-xs border-rose-300 text-rose-800 hover:bg-rose-100 gap-1"
              >
                Upgrade Plan
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Button>
            </div>
          ) : limitReached ? (
            <div className="flex items-start justify-between gap-3 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs">
              <div className="flex items-start gap-2.5">
                <ShieldAlert className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                <div>
                  <p className="font-semibold text-amber-900">
                    Staff Account Allocation Limit Reached
                  </p>
                  <p className="mt-0.5 text-amber-700">
                    Your organization has reached the maximum of {maxLabStaff} active lab staff accounts under your {planName || "current"} subscription plan. To add another staff member, either deactivate an inactive account or upgrade your plan.
                  </p>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsUpgradeModalOpen(true)}
                className="shrink-0 text-xs border-amber-300 text-amber-900 hover:bg-amber-100 gap-1"
              >
                Upgrade Plan
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Button>
            </div>
          ) : null}
        </div>
      </Card>

      {/* Upgrade Plan Info Modal */}
      <Dialog open={isUpgradeModalOpen} onOpenChange={setIsUpgradeModalOpen}>
        <DialogContent className="sm:max-w-[450px]">
          <DialogHeader>
            <div className="flex items-center gap-2 text-teal-800">
              <div className="p-2 rounded-lg bg-teal-50 border border-teal-100">
                <Sparkles className="h-5 w-5 text-[#0F766E]" />
              </div>
              <DialogTitle className="text-lg font-bold text-slate-900">
                Upgrade Staff License
              </DialogTitle>
            </div>
            <DialogDescription className="text-xs text-slate-500 pt-1">
              Increase your active staff seats to add more laboratory technicians and pathologists.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 py-2 text-xs">
            <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 divide-y divide-slate-100">
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500">Current Plan:</span>
                <span className="font-semibold text-slate-800">{planName || "Standard"}</span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500">Maximum Active Staff:</span>
                <span className="font-semibold text-slate-800">{maxLabStaff} accounts</span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500">Currently Active:</span>
                <span className="font-semibold text-slate-800">{activeStaff} accounts</span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-teal-50/70 border border-teal-100 text-slate-700 leading-relaxed">
              <p className="font-semibold text-teal-900 mb-1">How to upgrade:</p>
              To upgrade your organization to a higher-tier plan (e.g. Professional with 10 seats or Enterprise with 50 seats), contact your SwasthAI platform Super Administrator or email <span className="font-medium text-teal-900">admin@swasthai.com</span>.
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsUpgradeModalOpen(false)}
            >
              Close
            </Button>
            <Button
              type="button"
              onClick={() => {
                setIsUpgradeModalOpen(false);
                navigate("/org-admin/license");
              }}
              className="bg-[#0F766E] hover:bg-[#0d655e] text-white gap-1 cursor-pointer"
            >
              Go to License & Subscription
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
