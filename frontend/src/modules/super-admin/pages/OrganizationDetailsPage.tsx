import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  Building2,
  Calendar,
  Clock,
  Edit,
  ExternalLink,
  FileSignature,
  FileText,
  Globe,
  Hash,
  ImageIcon,
  ImageOff,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
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
import { ChangeOrganizationStatusDialog } from "../components/ChangeOrganizationStatusDialog";
import { EditOrganizationDialog } from "../components/EditOrganizationDialog";
import { OrganizationStatusBadge } from "../components/OrganizationStatusBadge";
import {
  useOrganizationQuery,
  useOrganizationProfileQuery,
} from "../hooks/useOrganizations";

export default function OrganizationDetailsPage() {
  const { orgRefId } = useParams<{ orgRefId: string }>();
  const navigate = useNavigate();

  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);

  const organizationQuery = useOrganizationQuery(orgRefId ?? "");
  const profileQuery = useOrganizationProfileQuery(orgRefId ?? "");

  const organization = organizationQuery.data;
  const profile = profileQuery.data;

  // Fetch Logo image blob when configured
  const logoQuery = useQuery({
    queryKey: ["organizations", orgRefId, "logo"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationLogo(orgRefId!);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(orgRefId && profile?.logoConfigured),
    staleTime: 5 * 60 * 1000,
  });

  // Fetch Signature image blob when configured
  const signatureQuery = useQuery({
    queryKey: ["organizations", orgRefId, "signature"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationSignature(orgRefId!);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(orgRefId && profile?.signatureConfigured),
    staleTime: 5 * 60 * 1000,
  });

  const logoUrl = logoQuery.data ?? null;
  const logoLoading = logoQuery.isLoading;
  const logoError = logoQuery.isError;

  const signatureUrl = signatureQuery.data ?? null;
  const signatureLoading = signatureQuery.isLoading;
  const signatureError = signatureQuery.isError;

  if (!orgRefId) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertDescription>No organization identifier provided.</AlertDescription>
        </Alert>
        <Button variant="outline" className="mt-4" onClick={() => navigate("/super-admin/organizations")}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Organizations
        </Button>
      </div>
    );
  }

  const isLoading = organizationQuery.isLoading || profileQuery.isLoading;

  const handleRefresh = () => {
    void organizationQuery.refetch();
    void profileQuery.refetch();
    void logoQuery.refetch();
    void signatureQuery.refetch();
  };

  return (
    <div className="space-y-6">
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
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
          <span className="text-sm font-semibold text-slate-700">
            {organization?.name ?? orgRefId}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={organizationQuery.isFetching || profileQuery.isFetching}
          >
            <RefreshCw
              className={`mr-1.5 h-3.5 w-3.5 ${
                organizationQuery.isFetching || profileQuery.isFetching
                  ? "animate-spin"
                  : ""
              }`}
            />
            Refresh
          </Button>

          {organization && (
            <>
              <Button
                variant="outline"
                size="sm"
                asChild
                className="text-slate-700 hover:text-slate-900"
              >
                <Link to={`/super-admin/organizations/${organization.refId}/profile`}>
                  <FileText className="mr-1.5 h-3.5 w-3.5" />
                  Profile & Branding
                </Link>
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setEditDialogOpen(true)}
              >
                <Edit className="mr-1.5 h-3.5 w-3.5" />
                Edit Organization
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setStatusDialogOpen(true)}
              >
                <SlidersHorizontal className="mr-1.5 h-3.5 w-3.5" />
                Change Status
              </Button>
            </>
          )}
        </div>
      </div>

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
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-4">
              <Skeleton className="h-8 w-64" />
              <Skeleton className="mt-2 h-4 w-48" />
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                <Skeleton className="h-20 w-full" />
                <Skeleton className="h-20 w-full" />
                <Skeleton className="h-20 w-full" />
                <Skeleton className="h-20 w-full" />
              </div>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Skeleton className="h-64 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        </div>
      )}

      {/* Main Content */}
      {!isLoading && organization && (
        <>
          {/* Organization Header Hero Card */}
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardHeader className="border-b border-slate-100 pb-4">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <Building2 className="h-6 w-6 text-slate-700" />
                    <CardTitle className="text-2xl font-bold tracking-tight text-slate-900">
                      {organization.name}
                    </CardTitle>
                    <OrganizationStatusBadge status={organization.status} />
                  </div>
                  <CardDescription className="font-mono text-xs text-slate-500">
                    Ref ID: {organization.refId} &bull; Code: {organization.code}
                  </CardDescription>
                </div>

                <Badge variant="outline" className="w-fit font-mono text-xs text-slate-600">
                  {organization.code}
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="pt-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/60 p-3">
                  <Hash className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-slate-500">System Reference ID</p>
                    <p className="mt-0.5 truncate font-mono text-xs font-semibold text-slate-900">
                      {organization.refId}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/60 p-3">
                  <ShieldCheck className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-slate-500">Operational Status</p>
                    <div className="mt-0.5">
                      <OrganizationStatusBadge status={organization.status} />
                    </div>
                  </div>
                </div>

                <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/60 p-3">
                  <Calendar className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-slate-500">Registered On</p>
                    <p className="mt-0.5 truncate text-xs font-semibold text-slate-900">
                      {new Date(organization.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/60 p-3">
                  <Clock className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-medium text-slate-500">Last Updated</p>
                    <p className="mt-0.5 truncate text-xs font-semibold text-slate-900">
                      {new Date(organization.updatedAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Images Section: Logo & Signature */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Organization Logo Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <ImageIcon className="h-4 w-4 text-blue-600" />
                    <CardTitle className="text-base font-semibold text-slate-900">
                      Organization Logo
                    </CardTitle>
                  </div>
                  {profile?.logoConfigured ? (
                    <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200">
                      Configured
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="text-slate-500">
                      Not Configured
                    </Badge>
                  )}
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Rendered in the diagnostic report header when organization header is enabled.
                </CardDescription>
              </CardHeader>

              <CardContent className="pt-2">
                {profile?.logoConfigured ? (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/70 p-6 min-h-[180px]">
                    {logoLoading && (
                      <div className="flex flex-col items-center gap-2">
                        <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                        <p className="text-xs text-slate-500">Loading logo image...</p>
                      </div>
                    )}

                    {logoError && (
                      <div className="flex flex-col items-center gap-2 text-center">
                        <ImageOff className="h-8 w-8 text-amber-500" />
                        <p className="text-xs font-medium text-amber-800">
                          Unable to render logo image
                        </p>
                        <p className="text-xs text-slate-500">
                          Logo file key is configured in database, but image content could not be retrieved.
                        </p>
                      </div>
                    )}

                    {!logoLoading && !logoError && logoUrl && (
                      <div className="flex flex-col items-center gap-3">
                        <div className="rounded-md border border-slate-200 bg-white p-3 shadow-xs">
                          <img
                            src={logoUrl}
                            alt={`${organization.name} Logo`}
                            className="max-h-36 max-w-full object-contain"
                          />
                        </div>
                        <p className="text-[11px] text-slate-500">
                          Secure preview loaded directly from backend file storage.
                        </p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/50 p-6 text-center min-h-[180px]">
                    <ImageOff className="h-8 w-8 text-slate-400" />
                    <p className="mt-2 text-xs font-semibold text-slate-700">
                      No Logo Configured
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500 max-w-xs">
                      This organization has not uploaded a clinical laboratory logo yet.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Authorized Signature Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <FileSignature className="h-4 w-4 text-emerald-600" />
                    <CardTitle className="text-base font-semibold text-slate-900">
                      Authorized Signature
                    </CardTitle>
                  </div>
                  {profile?.signatureConfigured ? (
                    <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200">
                      Configured
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="text-slate-500">
                      Not Configured
                    </Badge>
                  )}
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Affixed alongside doctor credentials when laboratory reports are finalized.
                </CardDescription>
              </CardHeader>

              <CardContent className="pt-2">
                {profile?.signatureConfigured ? (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/70 p-6 min-h-[180px]">
                    {signatureLoading && (
                      <div className="flex flex-col items-center gap-2">
                        <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                        <p className="text-xs text-slate-500">Loading signature image...</p>
                      </div>
                    )}

                    {signatureError && (
                      <div className="flex flex-col items-center gap-2 text-center">
                        <ImageOff className="h-8 w-8 text-amber-500" />
                        <p className="text-xs font-medium text-amber-800">
                          Unable to render signature image
                        </p>
                        <p className="text-xs text-slate-500">
                          Signature file key is configured in database, but image content could not be retrieved.
                        </p>
                      </div>
                    )}

                    {!signatureLoading && !signatureError && signatureUrl && (
                      <div className="flex flex-col items-center gap-3">
                        <div className="rounded-md border border-slate-200 bg-white p-3 shadow-xs">
                          <img
                            src={signatureUrl}
                            alt="Authorized Organization Signature"
                            className="max-h-28 max-w-full object-contain"
                          />
                        </div>
                        {profile?.signatureOwnerRefId && (
                          <p className="text-[11px] font-mono text-slate-600">
                            Owner: {profile.signatureOwnerRefId}
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/50 p-6 text-center min-h-[180px]">
                    <FileSignature className="h-8 w-8 text-slate-400" />
                    <p className="mt-2 text-xs font-semibold text-slate-700">
                      No Signature Configured
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500 max-w-xs">
                      An authorized organization admin must upload an initial signature specimen.
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Profile & Contact Details Section */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Address & Facility Information */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Facility Address
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Physical location printed on diagnostic headers and invoices.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3 pt-1">
                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-2">
                  <div>
                    <p className="text-xs font-medium text-slate-500">Address Line 1</p>
                    <p className="mt-0.5 text-sm font-semibold text-slate-900">
                      {profile?.addressLine1 || "Not specified"}
                    </p>
                  </div>

                  {profile?.addressLine2 && (
                    <div>
                      <p className="text-xs font-medium text-slate-500">Address Line 2</p>
                      <p className="mt-0.5 text-sm font-medium text-slate-800">
                        {profile.addressLine2}
                      </p>
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-3 pt-1 border-t border-slate-200/60">
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

            {/* Contact & Digital Channels */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Contact Channels
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Official contact information for patients and clinical inquiries.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3 pt-1">
                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-3">
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

          {/* Report Customization Text Section */}
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4 text-slate-700" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Report Texts & Disclaimers
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Printed verbatim on generated PDF reports for this tenant.
              </CardDescription>
            </CardHeader>

            <CardContent className="grid grid-cols-1 gap-4 md:grid-cols-2 pt-1">
              <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
                <p className="text-xs font-medium text-slate-500">Report Footer Text</p>
                <p className="text-xs font-medium text-slate-800 whitespace-pre-wrap">
                  {profile?.reportFooterText || "Standard default footer text"}
                </p>
              </div>

              <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3.5 space-y-1">
                <p className="text-xs font-medium text-slate-500">Report Legal Disclaimer</p>
                <p className="text-xs font-medium text-slate-800 whitespace-pre-wrap">
                  {profile?.reportDisclaimer ||
                    "This is a digitally generated medical diagnostic report."}
                </p>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {/* Action Dialogs */}
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
