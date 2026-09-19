import { useState, useEffect, useRef } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  FileSignature,
  FileText,
  Globe,
  ImageIcon,
  ImageOff,
  Info,
  Loader2,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  Save,
  Trash2,
  Upload,
} from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";

import { organizationApi } from "../api/organizationApi";
import { DeleteImageConfirmationDialog } from "../components/DeleteImageConfirmationDialog";
import { OrganizationStatusBadge } from "../components/OrganizationStatusBadge";
import {
  useOrganizationProfileQuery,
  useOrganizationQuery,
  useUpdateOrganizationProfileMutation,
  useUploadOrganizationLogoMutation,
  useDeleteOrganizationLogoMutation,
  useUploadOrganizationSignatureMutation,
  useDeleteOrganizationSignatureMutation,
} from "../hooks/useOrganizations";

const profileFormSchema = z.object({
  addressLine1: z
    .string()
    .max(200, "Maximum 200 characters")
    .optional()
    .or(z.literal("")),
  addressLine2: z
    .string()
    .max(200, "Maximum 200 characters")
    .optional()
    .or(z.literal("")),
  city: z
    .string()
    .max(100, "Maximum 100 characters")
    .optional()
    .or(z.literal("")),
  state: z
    .string()
    .max(100, "Maximum 100 characters")
    .optional()
    .or(z.literal("")),
  postalCode: z
    .string()
    .max(20, "Maximum 20 characters")
    .optional()
    .or(z.literal("")),
  country: z
    .string()
    .max(100, "Maximum 100 characters")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .max(30, "Maximum 30 characters")
    .regex(/^[0-9+()\-\s.]*$/, "Invalid phone number format")
    .optional()
    .or(z.literal("")),
  alternatePhone: z
    .string()
    .max(30, "Maximum 30 characters")
    .regex(/^[0-9+()\-\s.]*$/, "Invalid alternate phone number format")
    .optional()
    .or(z.literal("")),
  email: z
    .string()
    .max(150, "Maximum 150 characters")
    .email("Invalid email address")
    .optional()
    .or(z.literal("")),
  website: z
    .string()
    .max(255, "Maximum 255 characters")
    .regex(/^(https?:\/\/).*$/, "Website must start with http:// or https://")
    .optional()
    .or(z.literal("")),
  reportFooterText: z
    .string()
    .max(1000, "Maximum 1000 characters")
    .optional()
    .or(z.literal("")),
  reportDisclaimer: z
    .string()
    .max(2000, "Maximum 2000 characters")
    .optional()
    .or(z.literal("")),
});

type ProfileFormValues = z.infer<typeof profileFormSchema>;

export default function OrganizationProfilePage() {
  const { refId, orgRefId } = useParams<{ refId?: string; orgRefId?: string }>();
  const organizationRefId = refId || orgRefId || "";
  const navigate = useNavigate();

  const logoInputRef = useRef<HTMLInputElement>(null);
  const signatureInputRef = useRef<HTMLInputElement>(null);

  const [deleteLogoOpen, setDeleteLogoOpen] = useState(false);
  const [deleteSignatureOpen, setDeleteSignatureOpen] = useState(false);
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);
  const [actionErrorMessage, setActionErrorMessage] = useState<string | null>(null);

  const organizationQuery = useOrganizationQuery(organizationRefId);
  const profileQuery = useOrganizationProfileQuery(organizationRefId);

  const organization = organizationQuery.data;
  const profile = profileQuery.data;

  const updateProfileMutation = useUpdateOrganizationProfileMutation();
  const uploadLogoMutation = useUploadOrganizationLogoMutation();
  const deleteLogoMutation = useDeleteOrganizationLogoMutation();
  const uploadSignatureMutation = useUploadOrganizationSignatureMutation();
  const deleteSignatureMutation = useDeleteOrganizationSignatureMutation();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileFormSchema),
    defaultValues: {
      addressLine1: "",
      addressLine2: "",
      city: "",
      state: "",
      postalCode: "",
      country: "",
      phone: "",
      alternatePhone: "",
      email: "",
      website: "",
      reportFooterText: "",
      reportDisclaimer: "",
    },
  });

  // Watch character counts
  const reportFooterWatch = watch("reportFooterText") || "";
  const reportDisclaimerWatch = watch("reportDisclaimer") || "";

  // Synchronize form values with authoritative backend profile
  useEffect(() => {
    if (profile) {
      reset({
        addressLine1: profile.addressLine1 || "",
        addressLine2: profile.addressLine2 || "",
        city: profile.city || "",
        state: profile.state || "",
        postalCode: profile.postalCode || "",
        country: profile.country || "",
        phone: profile.phone || "",
        alternatePhone: profile.alternatePhone || "",
        email: profile.email || "",
        website: profile.website || "",
        reportFooterText: profile.reportFooterText || "",
        reportDisclaimer: profile.reportDisclaimer || "",
      });
    }
  }, [profile, reset]);

  // Authenticated logo blob streaming
  const logoQuery = useQuery({
    queryKey: ["organizations", organizationRefId, "logo"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationLogo(organizationRefId);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(organizationRefId && profile?.logoConfigured),
    staleTime: 5 * 60 * 1000,
  });

  // Authenticated signature blob streaming
  const signatureQuery = useQuery({
    queryKey: ["organizations", organizationRefId, "signature"],
    queryFn: async () => {
      const blob = await organizationApi.getOrganizationSignature(organizationRefId);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(organizationRefId && profile?.signatureConfigured),
    staleTime: 5 * 60 * 1000,
  });

  const logoUrl = logoQuery.data ?? null;
  const signatureUrl = signatureQuery.data ?? null;

  if (!organizationRefId) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertDescription>No organization reference ID provided.</AlertDescription>
        </Alert>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => navigate("/super-admin/organizations")}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Organizations
        </Button>
      </div>
    );
  }

  const isLoading = organizationQuery.isLoading || profileQuery.isLoading;
  const isSuspendedOrDisabled =
    organization && organization.status !== "ACTIVE";

  const handleRefreshAll = () => {
    setActionErrorMessage(null);
    setSaveSuccessMessage(null);
    void organizationQuery.refetch();
    void profileQuery.refetch();
    if (profile?.logoConfigured) void logoQuery.refetch();
    if (profile?.signatureConfigured) void signatureQuery.refetch();
  };

  // Form submit handler
  const onSubmit = async (values: ProfileFormValues) => {
    setActionErrorMessage(null);
    setSaveSuccessMessage(null);

    try {
      await updateProfileMutation.mutateAsync({
        refId: organizationRefId,
        data: {
          addressLine1: values.addressLine1?.trim() || null,
          addressLine2: values.addressLine2?.trim() || null,
          city: values.city?.trim() || null,
          state: values.state?.trim() || null,
          postalCode: values.postalCode?.trim() || null,
          country: values.country?.trim() || null,
          phone: values.phone?.trim() || null,
          alternatePhone: values.alternatePhone?.trim() || null,
          email: values.email?.trim() || null,
          website: values.website?.trim() || null,
          reportFooterText: values.reportFooterText?.trim() || null,
          reportDisclaimer: values.reportDisclaimer?.trim() || null,
        },
      });

      setSaveSuccessMessage("Organization profile and report branding updated successfully.");
    } catch (err: unknown) {
      console.error("Failed to update profile:", err);
      const message =
        err instanceof Error
          ? err.message
          : "Failed to update organization profile. Check input values and try again.";
      setActionErrorMessage(message);
    }
  };

  // Logo file selection and upload
  const handleLogoSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
      setActionErrorMessage("Invalid logo format. Only PNG, JPEG, or WebP images are allowed.");
      event.target.value = "";
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setActionErrorMessage("Logo image exceeds maximum allowed size of 5 MB.");
      event.target.value = "";
      return;
    }

    setActionErrorMessage(null);
    setSaveSuccessMessage(null);

    try {
      await uploadLogoMutation.mutateAsync({
        refId: organizationRefId,
        file,
      });
      void logoQuery.refetch();
      setSaveSuccessMessage("Organization logo uploaded successfully.");
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Failed to upload organization logo.";
      setActionErrorMessage(message);
    } finally {
      if (logoInputRef.current) {
        logoInputRef.current.value = "";
      }
    }
  };

  // Logo deletion confirmed
  const handleConfirmDeleteLogo = async () => {
    setActionErrorMessage(null);
    setSaveSuccessMessage(null);

    try {
      await deleteLogoMutation.mutateAsync(organizationRefId);
      setDeleteLogoOpen(false);
      setSaveSuccessMessage("Organization logo deleted successfully.");
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Failed to delete organization logo.";
      setActionErrorMessage(message);
    }
  };

  // Signature file selection and upload
  const handleSignatureSelect = async (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
      setActionErrorMessage("Invalid signature format. Only PNG, JPEG, or WebP images are allowed.");
      event.target.value = "";
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setActionErrorMessage("Signature image exceeds maximum allowed size of 5 MB.");
      event.target.value = "";
      return;
    }

    setActionErrorMessage(null);
    setSaveSuccessMessage(null);

    try {
      await uploadSignatureMutation.mutateAsync({
        refId: organizationRefId,
        file,
      });
      void signatureQuery.refetch();
      setSaveSuccessMessage("Authorized signature specimen updated successfully.");
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Failed to update authorized signature.";
      setActionErrorMessage(message);
    } finally {
      if (signatureInputRef.current) {
        signatureInputRef.current.value = "";
      }
    }
  };

  // Signature deletion confirmed
  const handleConfirmDeleteSignature = async () => {
    setActionErrorMessage(null);
    setSaveSuccessMessage(null);

    try {
      await deleteSignatureMutation.mutateAsync(organizationRefId);
      setDeleteSignatureOpen(false);
      setSaveSuccessMessage("Authorized signature specimen removed successfully.");
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Failed to delete authorized signature.";
      setActionErrorMessage(message);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Breadcrumbs */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-2 sm:gap-3">
          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-600 hover:text-slate-900"
          >
            <Link to={`/super-admin/organizations/${organizationRefId}`}>
              <ArrowLeft className="mr-1.5 h-4 w-4" />
              Organization Details
            </Link>
          </Button>
          <span className="text-slate-300">/</span>
          <span className="text-sm font-semibold text-slate-700">
            {organization?.name ?? organizationRefId}
          </span>
          <span className="text-slate-300">/</span>
          <span className="text-sm font-semibold text-slate-900">
            Profile & Branding
          </span>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefreshAll}
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
        </div>
      </div>

      {/* Inactive Organization Warning */}
      {isSuspendedOrDisabled && (
        <Alert variant="destructive" className="bg-rose-50 border-rose-200">
          <AlertCircle className="h-4 w-4 text-rose-600" />
          <AlertTitle className="text-rose-900">
            Organization is {organization.status}
          </AlertTitle>
          <AlertDescription className="text-xs text-rose-800">
            Profile modifications and branding uploads are restricted by the backend while this organization is suspended or disabled. Please reactivate the tenant to apply updates.
          </AlertDescription>
        </Alert>
      )}

      {/* Global Success / Error Feedback */}
      {saveSuccessMessage && (
        <Alert className="bg-emerald-50 border-emerald-200 text-emerald-900">
          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          <AlertDescription className="text-sm font-medium">
            {saveSuccessMessage}
          </AlertDescription>
        </Alert>
      )}

      {actionErrorMessage && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription className="text-sm font-medium">
            {actionErrorMessage}
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
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Skeleton className="h-48 w-full" />
                <Skeleton className="h-48 w-full" />
              </div>
            </CardContent>
          </Card>
          <Skeleton className="h-96 w-full" />
        </div>
      )}

      {/* Main Content */}
      {!isLoading && organization && (
        <div className="space-y-6">
          {/* Organization Mini Summary */}
          <Card className="border-slate-200 bg-white shadow-xs">
            <CardContent className="pt-5 pb-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 text-blue-700">
                    <Building2 className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-slate-900">
                      {organization.name}
                    </h2>
                    <p className="font-mono text-xs text-slate-500">
                      Ref: {organization.refId} &bull; Code: {organization.code}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <OrganizationStatusBadge status={organization.status} />
                  <Badge variant="outline" className="font-mono text-xs">
                    Version: {profile?.version ?? 1}
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Branding Section: Logo & Signature specimen management */}
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Logo Management Card */}
            <Card className="border-slate-200 bg-white shadow-xs flex flex-col justify-between">
              <div>
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
                    Displayed on printed diagnostic reports when the organization header is enabled.
                  </CardDescription>
                </CardHeader>

                <CardContent className="space-y-4 pt-1">
                  {/* Preview Area */}
                  <div className="flex min-h-[160px] flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/70 p-4">
                    {logoQuery.isLoading ? (
                      <div className="flex flex-col items-center gap-2">
                        <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                        <p className="text-xs text-slate-500">Loading logo preview...</p>
                      </div>
                    ) : profile?.logoConfigured && logoUrl ? (
                      <div className="flex flex-col items-center gap-2">
                        <div className="rounded-md border border-slate-200 bg-white p-2.5 shadow-xs">
                          <img
                            src={logoUrl}
                            alt={`${organization.name} Logo`}
                            className="max-h-28 max-w-full object-contain"
                          />
                        </div>
                        <p className="text-[11px] text-slate-500">
                          Active laboratory logo
                        </p>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center gap-1 text-center">
                        <ImageOff className="h-8 w-8 text-slate-400" />
                        <p className="mt-1 text-xs font-semibold text-slate-700">
                          No Logo Uploaded
                        </p>
                        <p className="text-[11px] text-slate-500 max-w-xs">
                          PNG, JPEG, or WebP up to 5 MB.
                        </p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </div>

              {/* Upload / Delete Actions */}
              <div className="border-t border-slate-100 px-6 py-3 bg-slate-50/40 rounded-b-xl flex items-center justify-between gap-3">
                <input
                  ref={logoInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="hidden"
                  onChange={handleLogoSelect}
                  disabled={uploadLogoMutation.isPending || isSuspendedOrDisabled}
                />

                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => logoInputRef.current?.click()}
                  disabled={uploadLogoMutation.isPending || isSuspendedOrDisabled}
                  className="text-xs"
                >
                  {uploadLogoMutation.isPending ? (
                    <>
                      <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                      Uploading...
                    </>
                  ) : (
                    <>
                      <Upload className="mr-1.5 h-3.5 w-3.5" />
                      {profile?.logoConfigured ? "Replace Logo" : "Upload Logo"}
                    </>
                  )}
                </Button>

                {profile?.logoConfigured && (
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    onClick={() => setDeleteLogoOpen(true)}
                    disabled={deleteLogoMutation.isPending || isSuspendedOrDisabled}
                    className="text-xs text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                  >
                    <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                    Delete Logo
                  </Button>
                )}
              </div>
            </Card>

            {/* Signature Management Card */}
            <Card className="border-slate-200 bg-white shadow-xs flex flex-col justify-between">
              <div>
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
                    Affixed on finalized laboratory reports alongside authorized signatory doctor credentials.
                  </CardDescription>
                </CardHeader>

                <CardContent className="space-y-3 pt-1">
                  {/* Signature Owner Info or Restriction Alert */}
                  {!profile?.signatureOwnerRefId ? (
                    <Alert className="bg-amber-50/80 border-amber-200 text-amber-900 py-2.5">
                      <Info className="h-4 w-4 text-amber-600" />
                      <AlertDescription className="text-xs leading-relaxed">
                        <strong className="font-semibold">Backend Invariant Rule:</strong> An initial authorized signature must first be uploaded by an active organization administrator (<code className="font-mono text-amber-800">ORG_ADMIN</code>). Once established, a SUPER_ADMIN may update or replace the specimen.
                      </AlertDescription>
                    </Alert>
                  ) : (
                    <div className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50/60 p-2.5 text-xs">
                      <span className="font-medium text-slate-500">Authorized Signatory Owner:</span>
                      <code className="font-mono font-semibold text-slate-800 bg-white px-2 py-0.5 rounded border border-slate-200">
                        {profile.signatureOwnerRefId}
                      </code>
                    </div>
                  )}

                  {/* Preview Area */}
                  <div className="flex min-h-[120px] flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/70 p-4">
                    {signatureQuery.isLoading ? (
                      <div className="flex flex-col items-center gap-2">
                        <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                        <p className="text-xs text-slate-500">Loading signature preview...</p>
                      </div>
                    ) : profile?.signatureConfigured && signatureUrl ? (
                      <div className="flex flex-col items-center gap-2">
                        <div className="rounded-md border border-slate-200 bg-white p-2.5 shadow-xs">
                          <img
                            src={signatureUrl}
                            alt="Authorized Signature Specimen"
                            className="max-h-24 max-w-full object-contain"
                          />
                        </div>
                        <p className="text-[11px] text-slate-500">
                          Active authorized signature specimen
                        </p>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center gap-1 text-center">
                        <ImageOff className="h-7 w-7 text-slate-400" />
                        <p className="mt-1 text-xs font-semibold text-slate-700">
                          No Signature Specimen
                        </p>
                        <p className="text-[11px] text-slate-500 max-w-xs">
                          {profile?.signatureOwnerRefId
                            ? "Ready for replacement upload by SUPER_ADMIN."
                            : "Awaiting initial upload by an ORG_ADMIN."}
                        </p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </div>

              {/* Upload / Delete Actions */}
              <div className="border-t border-slate-100 px-6 py-3 bg-slate-50/40 rounded-b-xl flex items-center justify-between gap-3">
                <input
                  ref={signatureInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="hidden"
                  onChange={handleSignatureSelect}
                  disabled={
                    uploadSignatureMutation.isPending ||
                    !profile?.signatureOwnerRefId ||
                    isSuspendedOrDisabled
                  }
                />

                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => signatureInputRef.current?.click()}
                  disabled={
                    uploadSignatureMutation.isPending ||
                    !profile?.signatureOwnerRefId ||
                    isSuspendedOrDisabled
                  }
                  title={
                    !profile?.signatureOwnerRefId
                      ? "Requires initial upload by ORG_ADMIN"
                      : "Upload new signature specimen"
                  }
                  className="text-xs"
                >
                  {uploadSignatureMutation.isPending ? (
                    <>
                      <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                      Uploading...
                    </>
                  ) : (
                    <>
                      <Upload className="mr-1.5 h-3.5 w-3.5" />
                      {profile?.signatureConfigured
                        ? "Replace Signature"
                        : "Upload Signature"}
                    </>
                  )}
                </Button>

                {profile?.signatureConfigured && (
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    onClick={() => setDeleteSignatureOpen(true)}
                    disabled={
                      deleteSignatureMutation.isPending || isSuspendedOrDisabled
                    }
                    className="text-xs text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                  >
                    <Trash2 className="mr-1.5 h-3.5 w-3.5" />
                    Delete Signature
                  </Button>
                )}
              </div>
            </Card>
          </div>

          {/* Profile Form: Address, Contact & Report Texts */}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Facility Address Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Facility Address Information
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Primary clinical laboratory address printed on headers, verified reports, and invoices.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-4 pt-1">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="addressLine1" className="text-xs font-medium text-slate-700">
                      Address Line 1
                    </Label>
                    <Input
                      id="addressLine1"
                      placeholder="Street address, building, suite"
                      {...register("addressLine1")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.addressLine1 && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.addressLine1.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="addressLine2" className="text-xs font-medium text-slate-700">
                      Address Line 2 (Optional)
                    </Label>
                    <Input
                      id="addressLine2"
                      placeholder="Apartment, unit, floor, landmark"
                      {...register("addressLine2")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.addressLine2 && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.addressLine2.message}
                      </p>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="city" className="text-xs font-medium text-slate-700">
                      City
                    </Label>
                    <Input
                      id="city"
                      placeholder="City"
                      {...register("city")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.city && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.city.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="state" className="text-xs font-medium text-slate-700">
                      State / Province
                    </Label>
                    <Input
                      id="state"
                      placeholder="State"
                      {...register("state")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.state && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.state.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="postalCode" className="text-xs font-medium text-slate-700">
                      Postal / ZIP Code
                    </Label>
                    <Input
                      id="postalCode"
                      placeholder="Postal Code"
                      {...register("postalCode")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.postalCode && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.postalCode.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="country" className="text-xs font-medium text-slate-700">
                      Country
                    </Label>
                    <Input
                      id="country"
                      placeholder="Country"
                      {...register("country")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.country && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.country.message}
                      </p>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Contact Channels Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Contact Channels
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Official contact parameters for patients, physicians, and platform correspondence.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-4 pt-1">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="phone" className="text-xs font-medium text-slate-700 flex items-center gap-1.5">
                      <Phone className="h-3.5 w-3.5 text-slate-400" />
                      Primary Phone
                    </Label>
                    <Input
                      id="phone"
                      placeholder="+91-9876543210"
                      {...register("phone")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.phone && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.phone.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="alternatePhone" className="text-xs font-medium text-slate-700 flex items-center gap-1.5">
                      <Phone className="h-3.5 w-3.5 text-slate-400" />
                      Alternate Phone
                    </Label>
                    <Input
                      id="alternatePhone"
                      placeholder="Alternate phone or hotline"
                      {...register("alternatePhone")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.alternatePhone && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.alternatePhone.message}
                      </p>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="email" className="text-xs font-medium text-slate-700 flex items-center gap-1.5">
                      <Mail className="h-3.5 w-3.5 text-slate-400" />
                      Official Email Address
                    </Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="contact@diagnostics.com"
                      {...register("email")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.email && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.email.message}
                      </p>
                    )}
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="website" className="text-xs font-medium text-slate-700 flex items-center gap-1.5">
                      <Globe className="h-3.5 w-3.5 text-slate-400" />
                      Website URL
                    </Label>
                    <Input
                      id="website"
                      placeholder="https://www.diagnostics.com"
                      {...register("website")}
                      disabled={isSuspendedOrDisabled}
                    />
                    {errors.website && (
                      <p className="text-xs text-rose-600 font-medium">
                        {errors.website.message}
                      </p>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Report Customization Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <FileText className="h-4 w-4 text-slate-700" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Report Footer & Legal Disclaimer
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Customizable notes rendered at the bottom of generated clinical test PDFs.
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-4 pt-1">
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="reportFooterText" className="text-xs font-medium text-slate-700">
                      Report Footer Text
                    </Label>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {reportFooterWatch.length} / 1000
                    </span>
                  </div>
                  <Textarea
                    id="reportFooterText"
                    rows={3}
                    placeholder="E.g., Thank you for choosing our laboratory. For queries, call our 24/7 hotline."
                    {...register("reportFooterText")}
                    disabled={isSuspendedOrDisabled}
                  />
                  {errors.reportFooterText && (
                    <p className="text-xs text-rose-600 font-medium">
                      {errors.reportFooterText.message}
                    </p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="reportDisclaimer" className="text-xs font-medium text-slate-700">
                      Report Legal Disclaimer
                    </Label>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {reportDisclaimerWatch.length} / 2000
                    </span>
                  </div>
                  <Textarea
                    id="reportDisclaimer"
                    rows={4}
                    placeholder="E.g., This report is based on findings from specimen tested. Clinical correlation is recommended."
                    {...register("reportDisclaimer")}
                    disabled={isSuspendedOrDisabled}
                  />
                  {errors.reportDisclaimer && (
                    <p className="text-xs text-rose-600 font-medium">
                      {errors.reportDisclaimer.message}
                    </p>
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Save & Reset Actions Bar */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => reset()}
                disabled={!isDirty || isSubmitting || isSuspendedOrDisabled}
              >
                Reset Changes
              </Button>

              <Button
                type="submit"
                disabled={!isDirty || isSubmitting || isSuspendedOrDisabled}
                className="bg-slate-900 hover:bg-slate-800 text-white min-w-[140px]"
              >
                {isSubmitting || updateProfileMutation.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Saving Profile...
                  </>
                ) : (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    Save Profile
                  </>
                )}
              </Button>
            </div>
          </form>

          {/* Delete Logo Confirmation Dialog */}
          <DeleteImageConfirmationDialog
            open={deleteLogoOpen}
            onOpenChange={setDeleteLogoOpen}
            type="logo"
            organizationName={organization.name}
            onConfirm={handleConfirmDeleteLogo}
            isDeleting={deleteLogoMutation.isPending}
          />

          {/* Delete Signature Confirmation Dialog */}
          <DeleteImageConfirmationDialog
            open={deleteSignatureOpen}
            onOpenChange={setDeleteSignatureOpen}
            type="signature"
            organizationName={organization.name}
            onConfirm={handleConfirmDeleteSignature}
            isDeleting={deleteSignatureMutation.isPending}
          />
        </div>
      )}
    </div>
  );
}
