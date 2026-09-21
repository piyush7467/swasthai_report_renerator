import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  Building2,
  ExternalLink,
  FileText,
  Globe,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Users,
} from "lucide-react";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { organizationApi } from "../api/organizationApi";
import { OrganizationStatusBadge } from "../components/OrganizationStatusBadge";
import { OrganizationHeaderNav } from "../components/OrganizationHeaderNav";
import {
  useOrganizationQuery,
  useOrganizationProfileQuery,
} from "../hooks/useOrganizations";
import { useOrganizationLicenseQuery } from "../licensing/hooks/useLicensing";
import { LicenseStatusBadge } from "../licensing/components/LicenseStatusBadge";
import { getLicenseExpiryInfo } from "../licensing/types/licensingTypes";

export default function OrganizationDetailsPage() {
  const params = useParams<{ refId?: string; orgRefId?: string }>();
  const refId = params.refId || params.orgRefId || "";
  const navigate = useNavigate();

  const organizationQuery = useOrganizationQuery(refId);
  const profileQuery = useOrganizationProfileQuery(refId);
  const licenseQuery = useOrganizationLicenseQuery(refId);

  const organization = organizationQuery.data;
  const profile = profileQuery.data;
  const license = licenseQuery.data;

  // Fetch Logo image blob when configured
  const logoQuery = useQuery({
    queryKey: ["organizations", refId, "logo"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationLogo(refId);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(refId && profile?.logoConfigured),
    staleTime: 5 * 60 * 1000,
  });

  // Fetch Signature image blob when configured
  const signatureQuery = useQuery({
    queryKey: ["organizations", refId, "signature"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationSignature(refId);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(refId && profile?.signatureConfigured),
    staleTime: 5 * 60 * 1000,
  });

  const logoUrl = logoQuery.data ?? null;
  const signatureUrl = signatureQuery.data ?? null;

  if (!refId) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertDescription>No organization identifier provided.</AlertDescription>
        </Alert>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => navigate("/super-admin/organizations")}
        >
          Back to Organizations
        </Button>
      </div>
    );
  }

  const isLoading = organizationQuery.isLoading || profileQuery.isLoading;

  const handleRefresh = () => {
    void organizationQuery.refetch();
    void profileQuery.refetch();
    void licenseQuery.refetch();
    void logoQuery.refetch();
    void signatureQuery.refetch();
  };

  return (
    <div className="space-y-6">
      {/* Context & Navigation Header with Tabs */}
      <OrganizationHeaderNav
        organization={organization}
        isLoading={organizationQuery.isLoading}
        onRefresh={handleRefresh}
        isRefreshing={
          organizationQuery.isFetching ||
          profileQuery.isFetching ||
          licenseQuery.isFetching ||
          logoQuery.isFetching ||
          signatureQuery.isFetching
        }
      />

      {/* Error Display */}
      {organizationQuery.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {organizationQuery.error instanceof Error
              ? organizationQuery.error.message
              : "Failed to load organization details from backend."}
          </AlertDescription>
        </Alert>
      )}

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Skeleton className="h-48 w-full rounded-xl" />
          <Skeleton className="h-48 w-full rounded-xl" />
          <Skeleton className="h-64 w-full rounded-xl" />
          <Skeleton className="h-64 w-full rounded-xl" />
        </div>
      )}

      {/* Overview Cards */}
      {!isLoading && organization && (
        <div className="space-y-6">
          {/* Top Row: System Identifiers, Users & Subscription License */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* System Information Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    System Identifiers
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Core registration and status metadata.
                </CardDescription>
              </CardHeader>

              <CardContent className="pt-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="rounded-lg border border-slate-100 bg-slate-50/70 p-3">
                    <p className="text-xs font-medium text-slate-500">Tenant Code</p>
                    <p className="mt-1 font-mono text-sm font-semibold text-slate-900">
                      {organization.code}
                    </p>
                  </div>

                  <div className="rounded-lg border border-slate-100 bg-slate-50/70 p-3">
                    <p className="text-xs font-medium text-slate-500">Operational Status</p>
                    <div className="mt-1">
                      <OrganizationStatusBadge status={organization.status} />
                    </div>
                  </div>

                  <div className="rounded-lg border border-slate-100 bg-slate-50/70 p-3">
                    <p className="text-xs font-medium text-slate-500">Registered On</p>
                    <p className="mt-1 text-xs font-semibold text-slate-800">
                      {new Date(organization.createdAt).toLocaleDateString(undefined, {
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      })}
                    </p>
                  </div>

                  <div className="rounded-lg border border-slate-100 bg-slate-50/70 p-3">
                    <p className="text-xs font-medium text-slate-500">Last Updated</p>
                    <p className="mt-1 text-xs font-semibold text-slate-800">
                      {new Date(organization.updatedAt).toLocaleDateString(undefined, {
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      })}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Organization Users Quick Link Card */}
            <Card className="border-slate-200 bg-white shadow-xs flex flex-col justify-between">
              <div>
                <CardHeader className="pb-3 border-b border-slate-100">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-slate-700" />
                      <CardTitle className="text-base font-semibold text-slate-900">
                        Organization Users
                      </CardTitle>
                    </div>
                    <Badge variant="outline" className="text-xs text-slate-600">
                      Primary
                    </Badge>
                  </div>
                  <CardDescription className="text-xs text-slate-500">
                    Facility administrators and lab staff.
                  </CardDescription>
                </CardHeader>

                <CardContent className="pt-4 space-y-2">
                  <p className="text-xs text-slate-600 leading-relaxed">
                    Create laboratory managers (Org Admin) or operators (Lab Staff) directly within this organization&apos;s isolated scope.
                  </p>
                  <div className="rounded-lg border border-blue-100 bg-blue-50/60 p-2.5 text-xs text-blue-900">
                    <p className="font-medium">Direct Personnel Assignment</p>
                    <p className="text-[11px] text-blue-700 mt-0.5">
                      Users created here are mapped to {organization.name}.
                    </p>
                  </div>
                </CardContent>
              </div>

              <div className="p-4 pt-0">
                <Button
                  asChild
                  className="w-full bg-slate-900 text-white hover:bg-slate-800 text-xs"
                >
                  <Link to={`/super-admin/organizations/${organization.refId}/users`}>
                    View Users
                    <ArrowRight className="ml-1.5 h-3.5 w-3.5" />
                  </Link>
                </Button>
              </div>
            </Card>

            {/* Subscription License Overview Card */}
            <Card className="border-slate-200 bg-white shadow-xs flex flex-col justify-between">
              <div>
                <CardHeader className="pb-3 border-b border-slate-100">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="h-4 w-4 text-emerald-600" />
                      <CardTitle className="text-base font-semibold text-slate-900">
                        Subscription License
                      </CardTitle>
                    </div>
                    {license ? (
                      <LicenseStatusBadge
                        status={license.status}
                        currentlyUsable={license.currentlyUsable}
                        showUsability
                        expiresAt={license.expiresAt}
                        showDaysLeft
                      />
                    ) : (
                      <Badge variant="outline" className="text-xs text-amber-600 border-amber-200 bg-amber-50">
                        Unlicensed
                      </Badge>
                    )}
                  </div>
                  <CardDescription className="text-xs text-slate-500">
                    Authorization for clinical reporting operations.
                  </CardDescription>
                </CardHeader>

                <CardContent className="pt-4 space-y-2.5">
                  {license ? (
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-500 font-medium">Assigned Plan:</span>
                        <span className="font-semibold text-slate-900">
                          {license.planName} ({license.planCode})
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-500 font-medium">Expires On:</span>
                        <span className="font-medium text-slate-800">
                          {new Date(license.expiresAt).toLocaleDateString(undefined, {
                            year: "numeric",
                            month: "short",
                            day: "numeric",
                          })}
                        </span>
                      </div>
                      {(() => {
                        const info = getLicenseExpiryInfo(license.expiresAt);
                        if (!info) return null;
                        return (
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-slate-500 font-medium">Time Remaining:</span>
                            <span
                              className={`font-semibold ${
                                info.isExpired
                                  ? "text-rose-600"
                                  : info.isExpiringSoon
                                  ? "text-amber-600"
                                  : "text-emerald-700"
                              }`}
                            >
                              {info.label}
                            </span>
                          </div>
                        );
                      })()}
                    </div>
                  ) : (
                    <div className="rounded-lg border border-amber-100 bg-amber-50/60 p-2.5 text-xs text-amber-900">
                      <p className="font-medium">No Active License</p>
                      <p className="text-[11px] text-amber-700 mt-0.5">
                        Activate a subscription plan to allow report creation.
                      </p>
                    </div>
                  )}
                </CardContent>
              </div>

              <div className="p-4 pt-0">
                <Button
                  asChild
                  variant="outline"
                  className="w-full text-slate-800 hover:text-slate-900 text-xs"
                >
                  <Link to={`/super-admin/organizations/${organization.refId}/license`}>
                    Manage License
                    <ArrowRight className="ml-1.5 h-3.5 w-3.5" />
                  </Link>
                </Button>
              </div>
            </Card>
          </div>

          {/* Middle Row: Contact & Address Information */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Facility Address Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Facility Address
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Location printed on diagnostic report headers.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3 pt-4">
                <div className="rounded-lg border border-slate-100 bg-slate-50/60 p-3.5 space-y-2">
                  <div>
                    <p className="text-xs font-medium text-slate-500">Street Address</p>
                    <p className="mt-0.5 text-sm font-semibold text-slate-900">
                      {profile?.addressLine1 || "Not specified"}
                    </p>
                    {profile?.addressLine2 && (
                      <p className="text-xs text-slate-600 mt-0.5">
                        {profile.addressLine2}
                      </p>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-200/60">
                    <div>
                      <p className="text-xs font-medium text-slate-500">City</p>
                      <p className="mt-0.5 text-xs font-semibold text-slate-800">
                        {profile?.city || "—"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-slate-500">State / Region</p>
                      <p className="mt-0.5 text-xs font-semibold text-slate-800">
                        {profile?.state || "—"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-slate-500">Postal Code</p>
                      <p className="mt-0.5 text-xs font-mono font-semibold text-slate-800">
                        {profile?.postalCode || "—"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-slate-500">Country</p>
                      <p className="mt-0.5 text-xs font-semibold text-slate-800">
                        {profile?.country || "—"}
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Contact Channels Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Official Contact
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Communications and online channels for patients and clinicians.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3 pt-4">
                <div className="rounded-lg border border-slate-100 bg-slate-50/60 p-3.5 space-y-3">
                  <div className="flex items-start gap-2.5">
                    <Mail className="mt-0.5 h-4 w-4 text-slate-400 shrink-0" />
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-medium text-slate-500">Email Address</p>
                      {profile?.email ? (
                        <a
                          href={`mailto:${profile.email}`}
                          className="mt-0.5 block truncate text-sm font-semibold text-blue-600 hover:underline"
                        >
                          {profile.email}
                        </a>
                      ) : (
                        <p className="mt-0.5 text-sm text-slate-500">Not specified</p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5 border-t border-slate-200/60 pt-2.5">
                    <Phone className="mt-0.5 h-4 w-4 text-slate-400 shrink-0" />
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-medium text-slate-500">Phone</p>
                      <p className="mt-0.5 text-sm font-semibold text-slate-900">
                        {profile?.phone || "Not specified"}
                      </p>
                      {profile?.alternatePhone && (
                        <p className="mt-0.5 text-xs text-slate-500">
                          Alt: {profile.alternatePhone}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5 border-t border-slate-200/60 pt-2.5">
                    <Globe className="mt-0.5 h-4 w-4 text-slate-400 shrink-0" />
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-medium text-slate-500">Website</p>
                      {profile?.website ? (
                        <a
                          href={profile.website}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="mt-0.5 inline-flex items-center gap-1 text-sm font-semibold text-blue-600 hover:underline"
                        >
                          {profile.website}
                          <ExternalLink className="h-3 w-3" />
                        </a>
                      ) : (
                        <p className="mt-0.5 text-sm text-slate-500">Not specified</p>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Bottom Row: Branding Snapshot */}
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardHeader className="pb-3 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <FileText className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Branding & Diagnostic Report Assets
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Configured laboratory logo, signature specimen, and report texts.
                </CardDescription>
              </div>

              <Button
                variant="outline"
                size="sm"
                asChild
                className="text-xs text-slate-700 hover:text-slate-900"
              >
                <Link to={`/super-admin/organizations/${organization.refId}/profile`}>
                  Manage Profile & Branding
                  <ArrowRight className="ml-1.5 h-3.5 w-3.5" />
                </Link>
              </Button>
            </CardHeader>

            <CardContent className="pt-4">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {/* Logo Snapshot */}
                <div className="rounded-lg border border-slate-100 bg-slate-50/60 p-4">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-200/60">
                    <span className="text-xs font-semibold text-slate-700">Laboratory Logo</span>
                    {profile?.logoConfigured ? (
                      <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 text-[10px]">
                        Configured
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="text-slate-500 text-[10px]">
                        Not Uploaded
                      </Badge>
                    )}
                  </div>
                  <div className="mt-3 flex items-center justify-center min-h-[100px]">
                    {profile?.logoConfigured && logoUrl ? (
                      <img
                        src={logoUrl}
                        alt="Logo Preview"
                        className="max-h-20 max-w-full object-contain"
                      />
                    ) : (
                      <p className="text-xs text-slate-400">No logo image specimen</p>
                    )}
                  </div>
                </div>

                {/* Signature Snapshot */}
                <div className="rounded-lg border border-slate-100 bg-slate-50/60 p-4">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-200/60">
                    <span className="text-xs font-semibold text-slate-700">Authorized Signature</span>
                    {profile?.signatureConfigured ? (
                      <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 text-[10px]">
                        Configured
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="text-slate-500 text-[10px]">
                        Not Uploaded
                      </Badge>
                    )}
                  </div>
                  <div className="mt-3 flex items-center justify-center min-h-[100px]">
                    {profile?.signatureConfigured && signatureUrl ? (
                      <img
                        src={signatureUrl}
                        alt="Signature Preview"
                        className="max-h-16 max-w-full object-contain"
                      />
                    ) : (
                      <p className="text-xs text-slate-400">No signature specimen</p>
                    )}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
