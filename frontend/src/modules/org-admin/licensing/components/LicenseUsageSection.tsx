import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Users,
  FileText,
  UserCheck,
  ShieldAlert,
  CheckCircle,
  HelpCircle,
  Calendar,
} from "lucide-react";
import type {
  PatientUsageSummary,
  ReportUsageSummary,
  StaffUsageSummary,
} from "../types/licenseTypes";

interface LicenseUsageSectionProps {
  staffUsage: StaffUsageSummary;
  patientUsage: PatientUsageSummary;
  reportUsage: ReportUsageSummary;
}

export function LicenseUsageSection({
  staffUsage,
  patientUsage,
  reportUsage,
}: LicenseUsageSectionProps) {
  const { activeStaff, maxLabStaff, remainingSlots, limitReached, overLimit } =
    staffUsage;

  const staffUsagePercent = Math.min(
    100,
    Math.round((activeStaff / Math.max(1, maxLabStaff)) * 100)
  );

  const monthlyReports = reportUsage.monthlyReportsCreated ?? 0;
  const maxMonthlyReports = reportUsage.maxReportsPerMonth ?? 0;
  const dailyReports = reportUsage.dailyReportsCreated ?? 0;
  const maxDailyReports = reportUsage.maxReportsPerDay ?? 0;
  const remainingMonthly = reportUsage.remainingMonthlyReports ?? -1;
  const monthlyLimitReached = !!reportUsage.monthlyLimitReached;
  const dailyLimitReached = !!reportUsage.dailyLimitReached;

  const isMonthlyUnlimited = maxMonthlyReports <= 0;
  const reportUsagePercent = isMonthlyUnlimited
    ? 0
    : Math.min(100, Math.round((monthlyReports / Math.max(1, maxMonthlyReports)) * 100));

  return (
    <div className="space-y-4">
      <div>
        <h3 className="text-base font-semibold text-slate-900">
          Resource Utilization & Capacity Limits
        </h3>
        <p className="text-xs text-slate-500">
          Real-time metrics monitored against your organization's subscription plan.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Lab Staff Capacity Card */}
        <Card className="p-5 border-slate-200 bg-white shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-lg bg-teal-50 border border-teal-100 text-[#0F766E]">
                <Users className="h-5 w-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900">
                  Lab Staff License Seats
                </h4>
                <p className="text-xs text-slate-500">
                  Active technicians and pathologists
                </p>
              </div>
            </div>

            {overLimit ? (
              <Badge
                variant="outline"
                className="bg-rose-50 text-rose-700 border-rose-200 text-xs font-semibold"
              >
                Over Limit
              </Badge>
            ) : limitReached ? (
              <Badge
                variant="outline"
                className="bg-amber-50 text-amber-700 border-amber-200 text-xs font-semibold"
              >
                Limit Reached
              </Badge>
            ) : remainingSlots <= 2 ? (
              <Badge
                variant="outline"
                className="bg-amber-50 text-amber-700 border-amber-200 text-xs font-medium"
              >
                {remainingSlots} {remainingSlots === 1 ? "seat" : "seats"} remaining
              </Badge>
            ) : (
              <Badge
                variant="outline"
                className="bg-teal-50 text-[#0F766E] border-teal-200 text-xs font-medium"
              >
                Healthy Usage
              </Badge>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex justify-between items-baseline">
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-bold text-slate-900">
                  {activeStaff} / {maxLabStaff}
                </span>
                <span className="text-xs text-slate-500">seats allocated</span>
              </div>
              <span className="text-xs font-medium text-slate-600">
                {remainingSlots} seats available
              </span>
            </div>

            {/* Progress bar */}
            <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
              <div
                className={`h-full transition-all duration-500 rounded-full ${
                  overLimit
                    ? "bg-rose-600"
                    : limitReached
                    ? "bg-amber-500"
                    : staffUsagePercent > 80
                    ? "bg-teal-700"
                    : "bg-[#0F766E]"
                }`}
                style={{ width: `${staffUsagePercent}%` }}
              />
            </div>
          </div>

          {/* Alerts */}
          {overLimit ? (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs leading-relaxed">
              <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
              <div>
                <p className="font-semibold text-rose-900">
                  Capacity Alert: Plan Downgrade Over-Limit
                </p>
                <p className="text-rose-700 mt-0.5">
                  Your organization is currently above the seat limit because of a plan downgrade. Creating or reactivating staff is blocked until seats are upgraded.
                </p>
              </div>
            </div>
          ) : limitReached ? (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs leading-relaxed">
              <ShieldAlert className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
              <div>
                <p className="font-semibold text-amber-900">
                  All Staff Seats Occupied
                </p>
                <p className="text-amber-700 mt-0.5">
                  All {maxLabStaff} staff seats are currently active. Submit an upgrade request or deactivate an unused member to onboard staff.
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2 text-xs text-slate-500 pt-1">
              <CheckCircle className="h-3.5 w-3.5 text-teal-600 shrink-0" />
              <span>
                Staff seats are active and automatically freed up when members are deactivated.
              </span>
            </div>
          )}
        </Card>

        {/* Monthly Report Generation Quota Card */}
        <Card className="p-5 border-slate-200 bg-white shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-700">
                <FileText className="h-5 w-5" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900">
                  Monthly Report Generation
                </h4>
                <p className="text-xs text-slate-500">
                  Reports drafted and finalized this month
                </p>
              </div>
            </div>

            {monthlyLimitReached ? (
              <Badge
                variant="outline"
                className="bg-rose-50 text-rose-700 border-rose-200 text-xs font-semibold"
              >
                Monthly Limit Reached
              </Badge>
            ) : dailyLimitReached ? (
              <Badge
                variant="outline"
                className="bg-amber-50 text-amber-700 border-amber-200 text-xs font-semibold"
              >
                Daily Limit Reached
              </Badge>
            ) : isMonthlyUnlimited ? (
              <Badge
                variant="outline"
                className="bg-indigo-50 text-indigo-700 border-indigo-200 text-xs font-medium"
              >
                Unlimited
              </Badge>
            ) : remainingMonthly <= 20 ? (
              <Badge
                variant="outline"
                className="bg-amber-50 text-amber-700 border-amber-200 text-xs font-medium"
              >
                {remainingMonthly} reports left
              </Badge>
            ) : (
              <Badge
                variant="outline"
                className="bg-teal-50 text-[#0F766E] border-teal-200 text-xs font-medium"
              >
                Healthy Usage
              </Badge>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex justify-between items-baseline">
              <div className="flex items-baseline gap-2">
                <span className="text-2xl font-bold text-slate-900">
                  {monthlyReports.toLocaleString()}
                  {!isMonthlyUnlimited && (
                    <span className="text-slate-400 font-normal text-lg">
                      {" "}/ {maxMonthlyReports.toLocaleString()}
                    </span>
                  )}
                </span>
                <span className="text-xs text-slate-500">
                  {isMonthlyUnlimited ? "reports generated this month" : "monthly reports used"}
                </span>
              </div>
              {!isMonthlyUnlimited && (
                <span className="text-xs font-medium text-slate-600">
                  {remainingMonthly} remaining
                </span>
              )}
            </div>

            {/* Progress bar */}
            {!isMonthlyUnlimited ? (
              <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                <div
                  className={`h-full transition-all duration-500 rounded-full ${
                    monthlyLimitReached
                      ? "bg-rose-600"
                      : reportUsagePercent > 80
                      ? "bg-amber-500"
                      : "bg-indigo-600"
                  }`}
                  style={{ width: `${reportUsagePercent}%` }}
                />
              </div>
            ) : (
              <div className="h-2.5 w-full rounded-full bg-indigo-50 overflow-hidden">
                <div className="h-full w-full bg-indigo-500/30 rounded-full" />
              </div>
            )}
          </div>

          {/* Quota Alerts & Daily Subtext */}
          {monthlyLimitReached ? (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs leading-relaxed">
              <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
              <div>
                <p className="font-semibold text-rose-900">
                  Monthly Generation Cap Exceeded
                </p>
                <p className="text-rose-700 mt-0.5">
                  Your organization has reached the monthly allowance of {maxMonthlyReports.toLocaleString()} reports. Upgrade your plan to generate more reports.
                </p>
              </div>
            </div>
          ) : dailyLimitReached ? (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs leading-relaxed">
              <ShieldAlert className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
              <div>
                <p className="font-semibold text-amber-900">
                  Daily Generation Limit Reached
                </p>
                <p className="text-amber-700 mt-0.5">
                  Today's quota of {maxDailyReports} reports is exhausted. Report generation will resume tomorrow, or upgrade for higher daily limits.
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
              <div className="flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                <span>
                  Today: <strong>{dailyReports}</strong>
                  {maxDailyReports > 0 ? ` / ${maxDailyReports} reports` : " reports"}
                </span>
              </div>
              <span className="text-[11px] text-slate-400">
                Resets on 1st of every month
              </span>
            </div>
          )}
        </Card>
      </div>

      {/* Operational Lifetime Records & Clarity Guide */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="p-4 border-slate-200 bg-white shadow-xs space-y-1 md:col-span-1">
          <div className="flex items-center gap-2 text-slate-600">
            <UserCheck className="h-4 w-4 text-slate-400" />
            <span className="text-xs font-medium uppercase tracking-wider">
              Registered Patients
            </span>
          </div>
          <div className="flex items-baseline justify-between pt-1">
            <span className="text-2xl font-bold text-slate-900">
              {patientUsage.totalPatients.toLocaleString()}
            </span>
            <span className="text-xs text-slate-500">records in system</span>
          </div>
        </Card>

        <Card className="p-4 border-slate-200 bg-white shadow-xs space-y-1 md:col-span-1">
          <div className="flex items-center gap-2 text-slate-600">
            <FileText className="h-4 w-4 text-slate-400" />
            <span className="text-xs font-medium uppercase tracking-wider">
              Lifetime Reports
            </span>
          </div>
          <div className="flex items-baseline justify-between pt-1">
            <span className="text-2xl font-bold text-slate-900">
              {reportUsage.totalReports.toLocaleString()}
            </span>
            <span className="text-xs text-teal-700 font-medium">
              {reportUsage.finalizedReports.toLocaleString()} finalized
            </span>
          </div>
        </Card>

        {/* Plan Capacity Guide Card */}
        <div className="p-4 rounded-xl border border-slate-200 bg-slate-50/80 text-xs text-slate-600 space-y-2 md:col-span-1">
          <div className="flex items-center gap-1.5 font-semibold text-slate-800">
            <HelpCircle className="h-4 w-4 text-teal-600" />
            <span>Understanding Limits & Seats</span>
          </div>
          <ul className="space-y-1 text-[11px] leading-relaxed text-slate-500">
            <li>
              • <strong>Staff Seats:</strong> Active accounts for pathologists & technicians. Deactivated accounts automatically free up seats.
            </li>
            <li>
              • <strong>Report Quotas:</strong> Total reports your organization can generate monthly and daily. Quotas reset automatically every month.
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
