import { Card } from "@/components/ui/card";
import { Check, Sparkles, Shield, Users, FileText } from "lucide-react";
import type { LicenseResponse } from "../types/licenseTypes";

interface PlanDetailsCardProps {
  license: LicenseResponse;
}

export function PlanDetailsCard({ license }: PlanDetailsCardProps) {
  const isPro = license.planCode === "PROFESSIONAL";
  const isEnterprise = license.planCode === "ENTERPRISE";

  return (
    <Card className="p-5 sm:p-6 border-slate-200 bg-white shadow-xs space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-base font-semibold text-slate-900">
            Subscription Plan Specifications
          </h3>
          <p className="text-xs text-slate-500">
            Capabilities and provisions enabled under the {license.planName} tier.
          </p>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-50 border border-slate-200 text-xs font-medium text-slate-700">
          <Sparkles className="h-3.5 w-3.5 text-teal-600" />
          <span>Annual Billing</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
        <div className="p-4 rounded-lg bg-slate-50/70 border border-slate-100 flex items-start gap-3">
          <div className="p-2 rounded-md bg-white border border-slate-200 text-teal-700 shadow-2xs">
            <Users className="h-4 w-4" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-900 block">
              Staff Seat Allocation
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              Up to <strong className="text-slate-700">{license.maxLabStaff} active users</strong> (technicians, pathologists, reviewers).
            </p>
          </div>
        </div>

        <div className="p-4 rounded-lg bg-slate-50/70 border border-slate-100 flex items-start gap-3">
          <div className="p-2 rounded-md bg-white border border-slate-200 text-indigo-700 shadow-2xs">
            <FileText className="h-4 w-4" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-900 block">
              Report Quota Limits
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              {license.maxReportsPerMonth && license.maxReportsPerMonth > 0 ? (
                <>
                  Up to <strong className="text-slate-700">{license.maxReportsPerMonth.toLocaleString()} reports / mo</strong> ({license.maxReportsPerDay ?? 25}/day).
                </>
              ) : (
                <strong className="text-slate-700">Unlimited diagnostic reports</strong>
              )}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-lg bg-slate-50/70 border border-slate-100 flex items-start gap-3">
          <div className="p-2 rounded-md bg-white border border-slate-200 text-teal-700 shadow-2xs">
            <Shield className="h-4 w-4" />
          </div>
          <div>
            <span className="text-xs font-semibold text-slate-900 block">
              Medical Security & Audit
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              Full cryptographic verification, tamper-evident audit logs & 10-day safety retention.
            </p>
          </div>
        </div>
      </div>

      {/* Feature list bullet points */}
      <div className="pt-2 border-t border-slate-100">
        <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block mb-2">
          Included in {license.planName}
        </span>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-700">
          <div className="flex items-center gap-2">
            <Check className="h-3.5 w-3.5 text-teal-600 shrink-0" />
            <span>Dedicated laboratory organization workspace</span>
          </div>
          <div className="flex items-center gap-2">
            <Check className="h-3.5 w-3.5 text-teal-600 shrink-0" />
            <span>Unlimited patient registrations and search</span>
          </div>
          <div className="flex items-center gap-2">
            <Check className="h-3.5 w-3.5 text-teal-600 shrink-0" />
            <span>Custom test parameter directory and reference ranges</span>
          </div>
          <div className="flex items-center gap-2">
            <Check className="h-3.5 w-3.5 text-teal-600 shrink-0" />
            <span>
              {isEnterprise
                ? "Priority 24/7 dedicated platform support & SLA"
                : isPro
                ? "Priority email & phone technical assistance"
                : "Standard platform email support"}
            </span>
          </div>
        </div>
      </div>
    </Card>
  );
}
