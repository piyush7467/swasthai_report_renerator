import { useState } from "react";
import {
  Building2,
  Calendar,
  Check,
  Clock,
  Copy,
  Globe,
  Mail,
  MapPin,
  Phone,
  ShieldAlert,
  Edit,
  Loader2,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import type { OrganizationResponse } from "../types/organizationTypes";
import { useOrganizationProfileQuery } from "../hooks/useOrganizations";
import { OrganizationStatusBadge } from "./OrganizationStatusBadge";

interface OrganizationDetailsDialogProps {
  organization: OrganizationResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit?: () => void;
  onChangeStatus?: () => void;
}

export function OrganizationDetailsDialog({
  organization,
  open,
  onOpenChange,
  onEdit,
  onChangeStatus,
}: OrganizationDetailsDialogProps) {
  const [copied, setCopied] = useState(false);

  const { data: profile, isLoading: profileLoading } =
    useOrganizationProfileQuery(organization?.refId ?? "", Boolean(open && organization));

  if (!organization) return null;

  const handleCopyRefId = () => {
    void navigator.clipboard.writeText(organization.refId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "—";
    try {
      return new Date(isoString).toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader className="pb-2">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-slate-900 text-white shadow-sm">
                <Building2 className="h-6 w-6" />
              </div>
              <div>
                <DialogTitle className="text-xl font-bold text-slate-900">
                  {organization.name}
                </DialogTitle>
                <div className="mt-1 flex items-center gap-2">
                  <span className="rounded bg-slate-100 px-2 py-0.5 font-mono text-xs font-semibold text-slate-700">
                    {organization.code}
                  </span>
                  <OrganizationStatusBadge status={organization.status} />
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 pt-1">
              {onEdit && organization.status !== "DISABLED" && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    onOpenChange(false);
                    onEdit();
                  }}
                >
                  <Edit className="mr-1.5 h-3.5 w-3.5" />
                  Edit
                </Button>
              )}
              {onChangeStatus && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    onOpenChange(false);
                    onChangeStatus();
                  }}
                >
                  <ShieldAlert className="mr-1.5 h-3.5 w-3.5" />
                  Status
                </Button>
              )}
            </div>
          </div>
          <DialogDescription className="sr-only">
            Detailed information for {organization.name}
          </DialogDescription>
        </DialogHeader>

        <Separator className="my-2" />

        {/* Identification & Audit Details */}
        <div className="space-y-4 text-xs">
          <div className="rounded-lg border border-slate-200 bg-slate-50/70 p-3.5">
            <div className="flex items-center justify-between">
              <span className="text-slate-500 font-medium">Tenant Reference ID:</span>
              <div className="flex items-center gap-1.5">
                <code className="rounded bg-white px-2 py-1 font-mono text-xs font-semibold text-slate-800 border border-slate-200">
                  {organization.refId}
                </code>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-slate-500 hover:text-slate-900"
                  onClick={handleCopyRefId}
                  title="Copy Reference ID"
                >
                  {copied ? (
                    <Check className="h-3.5 w-3.5 text-emerald-600" />
                  ) : (
                    <Copy className="h-3.5 w-3.5" />
                  )}
                </Button>
              </div>
            </div>

            <div className="mt-2.5 grid grid-cols-1 gap-2 sm:grid-cols-2 pt-2 border-t border-slate-200/80">
              <div className="flex items-center gap-1.5 text-slate-600">
                <Calendar className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                <span>Created: {formatDate(organization.createdAt)}</span>
              </div>
              <div className="flex items-center gap-1.5 text-slate-600">
                <Clock className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                <span>Last Updated: {formatDate(organization.updatedAt)}</span>
              </div>
            </div>
          </div>

          {/* Laboratory Profile / Branding Information */}
          <div className="space-y-3 pt-1">
            <h4 className="text-sm font-semibold text-slate-900">
              Laboratory Profile & Contact
            </h4>

            {profileLoading ? (
              <div className="flex items-center justify-center py-6 text-slate-500">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                <span>Loading laboratory profile...</span>
              </div>
            ) : profile ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {/* Contact Column */}
                <div className="rounded-lg border border-slate-200 p-3 space-y-2">
                  <p className="font-semibold text-slate-700">Contact Channels</p>
                  <div className="space-y-1.5 text-slate-600">
                    <div className="flex items-center gap-2">
                      <Mail className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{profile.email || "No email registered"}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Phone className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span>{profile.phone || "No phone registered"}</span>
                    </div>
                    {profile.alternatePhone && (
                      <div className="flex items-center gap-2 pl-5 text-slate-500">
                        <span>Alt: {profile.alternatePhone}</span>
                      </div>
                    )}
                    <div className="flex items-center gap-2">
                      <Globe className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{profile.website || "No website registered"}</span>
                    </div>
                  </div>
                </div>

                {/* Address Column */}
                <div className="rounded-lg border border-slate-200 p-3 space-y-2">
                  <p className="font-semibold text-slate-700">Physical Address</p>
                  <div className="flex items-start gap-2 text-slate-600">
                    <MapPin className="mt-0.5 h-3.5 w-3.5 text-slate-400 shrink-0" />
                    <div>
                      <p>{profile.addressLine1 || "No address entered"}</p>
                      {profile.addressLine2 && <p>{profile.addressLine2}</p>}
                      <p>
                        {[profile.city, profile.state, profile.postalCode]
                          .filter(Boolean)
                          .join(", ") || "—"}
                      </p>
                      <p>{profile.country || ""}</p>
                    </div>
                  </div>
                </div>

                {/* Branding Assets Status */}
                <div className="sm:col-span-2 rounded-lg border border-slate-200 p-3 space-y-2.5">
                  <p className="font-semibold text-slate-700">Report Branding & Certification</p>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    <div className="flex items-center justify-between rounded bg-slate-50 px-3 py-2 border border-slate-100">
                      <span className="text-slate-600">Lab Logo:</span>
                      <span
                        className={`font-semibold ${
                          profile.logoConfigured ? "text-emerald-700" : "text-slate-400"
                        }`}
                      >
                        {profile.logoConfigured ? "Configured" : "Not Set"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between rounded bg-slate-50 px-3 py-2 border border-slate-100">
                      <span className="text-slate-600">Signature:</span>
                      <span
                        className={`font-semibold ${
                          profile.signatureConfigured ? "text-emerald-700" : "text-slate-400"
                        }`}
                      >
                        {profile.signatureConfigured ? "Configured" : "Not Set"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between rounded bg-slate-50 px-3 py-2 border border-slate-100">
                      <span className="text-slate-600">Signature Owner:</span>
                      <span className="font-mono truncate max-w-28" title={profile.signatureOwnerRefId ?? "None"}>
                        {profile.signatureOwnerRefId ? "Assigned" : "Unassigned"}
                      </span>
                    </div>
                  </div>

                  {profile.reportFooterText && (
                    <div className="pt-1.5 border-t border-slate-100">
                      <p className="text-[11px] font-medium text-slate-500">Report Footer Text:</p>
                      <p className="text-xs italic text-slate-700 mt-0.5">
                        "{profile.reportFooterText}"
                      </p>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-xs text-slate-500 italic">No extended profile configured.</p>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
