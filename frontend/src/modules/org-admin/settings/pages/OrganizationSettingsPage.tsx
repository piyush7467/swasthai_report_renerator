import { useState, useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  Clock,
  FileSignature,
  FileText,
  Globe,
  ImageIcon,
  ImageOff,
  Loader2,
  Mail,
  MapPin,
  PenTool,
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

import { DeleteImageConfirmationDialog } from "@/modules/super-admin/components/DeleteImageConfirmationDialog";
import { SignatureDrawingModal } from "../components/SignatureDrawingModal";
import {
  useMyOrganizationProfileQuery,
  useMyLogoBlobQuery,
  useMySignatureBlobQuery,
  useUpdateMyOrganizationProfileMutation,
  useUploadMyLogoMutation,
  useDeleteMyLogoMutation,
  useUploadMySignatureMutation,
  useDeleteMySignatureMutation,
} from "../hooks/useOrgProfile";
import type { UpdateOrganizationProfileRequest } from "@/modules/super-admin/types/organizationTypes";

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
    .email("Invalid email address")
    .max(150, "Maximum 150 characters")
    .optional()
    .or(z.literal("")),
  website: z
    .string()
    .regex(/^(https?:\/\/).+/, "Website must start with http:// or https://")
    .max(255, "Maximum 255 characters")
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

export default function OrganizationSettingsPage() {
  const [activeTab, setActiveTab] = useState("general");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Logo & Signature delete dialog state
  const [deleteDialogType, setDeleteDialogType] = useState<"logo" | "signature" | null>(null);
  const [isDrawingModalOpen, setIsDrawingModalOpen] = useState(false);

  // Hidden file inputs
  const logoInputRef = useRef<HTMLInputElement>(null);
  const signatureInputRef = useRef<HTMLInputElement>(null);

  // Queries
  const {
    data: profile,
    isLoading: isProfileLoading,
    isError: isProfileError,
    error: profileError,
    refetch: refetchProfile,
  } = useMyOrganizationProfileQuery();

  const { data: logoBlob, isLoading: isLogoBlobLoading } = useMyLogoBlobQuery(
    Boolean(profile?.logoConfigured),
  );
  const { data: signatureBlob, isLoading: isSignatureBlobLoading } = useMySignatureBlobQuery(
    Boolean(profile?.signatureConfigured),
  );

  // Object URLs for preview
  const [logoUrl, setLogoUrl] = useState<string | null>(null);
  const [signatureUrl, setSignatureUrl] = useState<string | null>(null);

  useEffect(() => {
    if (logoBlob) {
      const url = URL.createObjectURL(logoBlob);
      setLogoUrl(url);
      return () => URL.revokeObjectURL(url);
    } else {
      setLogoUrl(null);
    }
  }, [logoBlob]);

  useEffect(() => {
    if (signatureBlob) {
      const url = URL.createObjectURL(signatureBlob);
      setSignatureUrl(url);
      return () => URL.revokeObjectURL(url);
    } else {
      setSignatureUrl(null);
    }
  }, [signatureBlob]);

  // Mutations
  const updateProfileMutation = useUpdateMyOrganizationProfileMutation();
  const uploadLogoMutation = useUploadMyLogoMutation();
  const deleteLogoMutation = useDeleteMyLogoMutation();
  const uploadSignatureMutation = useUploadMySignatureMutation();
  const deleteSignatureMutation = useDeleteMySignatureMutation();

  // Form setup
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isDirty },
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

  // Populate form with current values
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

  // Watched values for live preview
  const watchedAddress1 = watch("addressLine1") || profile?.addressLine1 || "";
  const watchedAddress2 = watch("addressLine2") || profile?.addressLine2 || "";
  const watchedCity = watch("city") || profile?.city || "";
  const watchedState = watch("state") || profile?.state || "";
  const watchedPostal = watch("postalCode") || profile?.postalCode || "";
  const watchedPhone = watch("phone") || profile?.phone || "";
  const watchedEmail = watch("email") || profile?.email || "";
  const watchedWebsite = watch("website") || profile?.website || "";
  const watchedFooter = watch("reportFooterText") || profile?.reportFooterText || "";
  const watchedDisclaimer = watch("reportDisclaimer") || profile?.reportDisclaimer || "";

  // Auto-dismiss success message
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  const onSubmit = async (values: ProfileFormValues) => {
    setSuccessMessage(null);
    setErrorMessage(null);

    const payload: UpdateOrganizationProfileRequest = {
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
    };

    try {
      await updateProfileMutation.mutateAsync(payload);
      setSuccessMessage("Organization settings and letterhead updated successfully.");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update profile settings.";
      setErrorMessage(msg);
    }
  };

  const handleLogoFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setErrorMessage("Please select a valid image file (PNG, JPEG, WebP).");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setErrorMessage("Logo file size must be less than 2MB.");
      return;
    }

    try {
      setErrorMessage(null);
      await uploadLogoMutation.mutateAsync(file);
      setSuccessMessage("Laboratory logo uploaded successfully.");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to upload laboratory logo.";
      setErrorMessage(msg);
    } finally {
      if (logoInputRef.current) logoInputRef.current.value = "";
    }
  };

  const handleSignatureFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setErrorMessage("Please select a valid image file for digital signature.");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setErrorMessage("Signature file size must be less than 2MB.");
      return;
    }

    try {
      setErrorMessage(null);
      await uploadSignatureMutation.mutateAsync(file);
      setSuccessMessage("Pathologist signature uploaded successfully.");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to upload signature.";
      setErrorMessage(msg);
    } finally {
      if (signatureInputRef.current) signatureInputRef.current.value = "";
    }
  };

  const handleSaveDrawnSignature = async (file: File) => {
    try {
      setErrorMessage(null);
      await uploadSignatureMutation.mutateAsync(file);
      setSuccessMessage("Digital signature saved successfully.");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to save digital signature.";
      setErrorMessage(msg);
      throw err;
    }
  };

  const handleConfirmDelete = async () => {
    if (!deleteDialogType) return;
    try {
      setErrorMessage(null);
      if (deleteDialogType === "logo") {
        await deleteLogoMutation.mutateAsync();
        setSuccessMessage("Laboratory logo removed successfully.");
      } else {
        await deleteSignatureMutation.mutateAsync();
        setSuccessMessage("Pathologist signature removed successfully.");
      }
      setDeleteDialogType(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to remove image.";
      setErrorMessage(msg);
    }
  };

  if (isProfileLoading) {
    return (
      <div className="space-y-6 max-w-6xl mx-auto p-4 sm:p-6 animate-pulse">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-20 w-full" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-64 col-span-2" />
          <Skeleton className="h-64" />
        </div>
      </div>
    );
  }

  if (isProfileError) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <Alert variant="destructive">
          <AlertCircle className="h-5 w-5" />
          <AlertTitle className="text-base font-semibold">Error Loading Organization Settings</AlertTitle>
          <AlertDescription className="text-xs mt-1">
            {profileError?.message || "Unable to fetch organization profile. Please try again."}
          </AlertDescription>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetchProfile()}
            className="mt-4 text-xs bg-white text-rose-900 border-rose-300"
          >
            <RefreshCw className="h-3.5 w-3.5 mr-1.5" />
            Retry
          </Button>
        </Alert>
      </div>
    );
  }

  const isSaving = updateProfileMutation.isPending;
  const isLogoUploading = uploadLogoMutation.isPending;
  const isSignatureUploading = uploadSignatureMutation.isPending;

  return (
    <div className="max-w-6xl mx-auto p-4 sm:p-6 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="flex size-10 items-center justify-center rounded-xl bg-teal-50 text-[#0F766E] border border-teal-100">
              <Building2 className="size-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                Organization Settings & Letterhead
              </h1>
              <p className="text-xs text-slate-500 mt-0.5">
                Manage your laboratory identity, official logo, pathologist digital signature, and report letterhead.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {profile?.organizationRefId && (
            <Badge variant="outline" className="font-mono text-xs text-slate-600 bg-slate-50">
              Ref: {profile.organizationRefId}
            </Badge>
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => refetchProfile()}
            className="text-xs h-9 border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer"
          >
            <RefreshCw className="size-3.5 mr-1.5 text-slate-500" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Global Notifications */}
      {successMessage && (
        <Alert className="border-teal-200 bg-teal-50 text-teal-900 animate-in fade-in duration-200">
          <CheckCircle2 className="size-4 text-teal-600" />
          <AlertTitle className="text-xs font-semibold">Success</AlertTitle>
          <AlertDescription className="text-xs text-teal-800">{successMessage}</AlertDescription>
        </Alert>
      )}

      {errorMessage && (
        <Alert variant="destructive" className="border-rose-200 bg-rose-50 text-rose-900 animate-in fade-in duration-200">
          <AlertCircle className="size-4 text-rose-600" />
          <AlertTitle className="text-xs font-semibold">Action Notice</AlertTitle>
          <AlertDescription className="text-xs text-rose-800">{errorMessage}</AlertDescription>
        </Alert>
      )}

      {/* Main Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <div className="w-full overflow-x-auto pb-1 [-webkit-overflow-scrolling:touch]">
          <TabsList className="inline-flex w-full min-w-max sm:w-auto bg-slate-100 p-1 border border-slate-200">
            <TabsTrigger value="general" className="text-xs font-medium shrink-0 data-[state=active]:bg-white data-[state=active]:text-[#0F766E] data-[state=active]:font-semibold">
              <Building2 className="size-3.5 mr-1.5" />
              General & Address
            </TabsTrigger>
            <TabsTrigger value="branding" className="text-xs font-medium shrink-0 data-[state=active]:bg-white data-[state=active]:text-[#0F766E] data-[state=active]:font-semibold">
              <ImageIcon className="size-3.5 mr-1.5" />
              Logo & Signature
            </TabsTrigger>
            <TabsTrigger value="disclaimers" className="text-xs font-medium shrink-0 data-[state=active]:bg-white data-[state=active]:text-[#0F766E] data-[state=active]:font-semibold">
              <FileText className="size-3.5 mr-1.5" />
              Report Footers
            </TabsTrigger>
            <TabsTrigger value="preview" className="text-xs font-medium shrink-0 data-[state=active]:bg-white data-[state=active]:text-[#0F766E] data-[state=active]:font-semibold">
              <FileSignature className="size-3.5 mr-1.5" />
              Letterhead Live Preview
            </TabsTrigger>
          </TabsList>
        </div>

        {/* TAB 1: General Information & Physical Address */}
        <TabsContent value="general" className="space-y-6 mt-0">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-4 border-b border-slate-100">
                <CardTitle className="text-base font-semibold text-slate-900">
                  Laboratory Identity & Contacts
                </CardTitle>
                <CardDescription className="text-xs text-slate-500">
                  Official contact details that appear on printed reports and patient verification pages.
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-5 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label className="text-xs font-semibold text-slate-700">Organization Name</Label>
                    <Input
                      value={profile?.organizationName || ""}
                      disabled
                      className="mt-1.5 h-9 text-xs bg-slate-50 border-slate-200 text-slate-600 font-semibold"
                    />
                    <p className="text-[11px] text-slate-400 mt-1">Tenant name configured in platform licensing.</p>
                  </div>

                  <div>
                    <Label htmlFor="email" className="text-xs font-semibold text-slate-700">Official Lab Email</Label>
                    <div className="relative mt-1.5">
                      <Mail className="size-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <Input
                        id="email"
                        placeholder="reports@laboratory.com"
                        className={`pl-9 h-9 text-xs border-slate-200 ${errors.email ? "border-rose-300 focus-visible:ring-rose-200" : ""}`}
                        {...register("email")}
                      />
                    </div>
                    {errors.email && <p className="text-[11px] text-rose-600 mt-1">{errors.email.message}</p>}
                  </div>

                  <div>
                    <Label htmlFor="phone" className="text-xs font-semibold text-slate-700">Primary Phone</Label>
                    <div className="relative mt-1.5">
                      <Phone className="size-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <Input
                        id="phone"
                        placeholder="+91 98765 43210"
                        className={`pl-9 h-9 text-xs border-slate-200 ${errors.phone ? "border-rose-300 focus-visible:ring-rose-200" : ""}`}
                        {...register("phone")}
                      />
                    </div>
                    {errors.phone && <p className="text-[11px] text-rose-600 mt-1">{errors.phone.message}</p>}
                  </div>

                  <div>
                    <Label htmlFor="alternatePhone" className="text-xs font-semibold text-slate-700">Alternate / Emergency Contact</Label>
                    <div className="relative mt-1.5">
                      <Phone className="size-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <Input
                        id="alternatePhone"
                        placeholder="+91 11 2345 6789"
                        className={`pl-9 h-9 text-xs border-slate-200 ${errors.alternatePhone ? "border-rose-300 focus-visible:ring-rose-200" : ""}`}
                        {...register("alternatePhone")}
                      />
                    </div>
                    {errors.alternatePhone && <p className="text-[11px] text-rose-600 mt-1">{errors.alternatePhone.message}</p>}
                  </div>

                  <div className="md:col-span-2">
                    <Label htmlFor="website" className="text-xs font-semibold text-slate-700">Website URL</Label>
                    <div className="relative mt-1.5">
                      <Globe className="size-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <Input
                        id="website"
                        placeholder="https://www.laboratory.com"
                        className={`pl-9 h-9 text-xs border-slate-200 ${errors.website ? "border-rose-300 focus-visible:ring-rose-200" : ""}`}
                        {...register("website")}
                      />
                    </div>
                    {errors.website && <p className="text-[11px] text-rose-600 mt-1">{errors.website.message}</p>}
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-4 border-b border-slate-100">
                <CardTitle className="text-base font-semibold text-slate-900">
                  Registered Physical Address
                </CardTitle>
                <CardDescription className="text-xs text-slate-500">
                  Physical address included on the letterhead header of all issued diagnostic reports.
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-5 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="md:col-span-2">
                    <Label htmlFor="addressLine1" className="text-xs font-semibold text-slate-700">Address Line 1</Label>
                    <div className="relative mt-1.5">
                      <MapPin className="size-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <Input
                        id="addressLine1"
                        placeholder="Plot No. 12, Diagnostic Towers, Sector 4"
                        className="pl-9 h-9 text-xs border-slate-200"
                        {...register("addressLine1")}
                      />
                    </div>
                  </div>

                  <div className="md:col-span-2">
                    <Label htmlFor="addressLine2" className="text-xs font-semibold text-slate-700">Address Line 2 (Optional)</Label>
                    <Input
                      id="addressLine2"
                      placeholder="Near Civil Hospital, Landmark Gate 2"
                      className="mt-1.5 h-9 text-xs border-slate-200"
                      {...register("addressLine2")}
                    />
                  </div>

                  <div>
                    <Label htmlFor="city" className="text-xs font-semibold text-slate-700">City / District</Label>
                    <Input
                      id="city"
                      placeholder="New Delhi"
                      className="mt-1.5 h-9 text-xs border-slate-200"
                      {...register("city")}
                    />
                  </div>

                  <div>
                    <Label htmlFor="state" className="text-xs font-semibold text-slate-700">State / Province</Label>
                    <Input
                      id="state"
                      placeholder="Delhi"
                      className="mt-1.5 h-9 text-xs border-slate-200"
                      {...register("state")}
                    />
                  </div>

                  <div>
                    <Label htmlFor="postalCode" className="text-xs font-semibold text-slate-700">Postal / PIN Code</Label>
                    <Input
                      id="postalCode"
                      placeholder="110001"
                      className="mt-1.5 h-9 text-xs border-slate-200"
                      {...register("postalCode")}
                    />
                  </div>

                  <div>
                    <Label htmlFor="country" className="text-xs font-semibold text-slate-700">Country</Label>
                    <Input
                      id="country"
                      placeholder="India"
                      className="mt-1.5 h-9 text-xs border-slate-200"
                      {...register("country")}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                type="submit"
                disabled={isSaving || !isDirty}
                className="w-full sm:w-auto bg-[#0F766E] hover:bg-[#115E59] text-white text-xs h-9 px-5 rounded-lg shadow-xs cursor-pointer font-semibold"
              >
                {isSaving ? (
                  <>
                    <Loader2 className="size-3.5 mr-1.5 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="size-3.5 mr-1.5" />
                    Save Information
                  </>
                )}
              </Button>
            </div>
          </form>
        </TabsContent>

        {/* TAB 2: Laboratory Logo & Doctor Signature */}
        <TabsContent value="branding" className="space-y-6 mt-0">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Laboratory Logo Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                    <ImageIcon className="size-4 text-teal-600" />
                    Laboratory Logo
                  </CardTitle>
                  {profile?.logoConfigured ? (
                    <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px]">
                      Configured
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="text-slate-400 text-[10px]">
                      Not Set
                    </Badge>
                  )}
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Prints on the top-left of every diagnostic report header. Max 2MB (PNG, JPEG).
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-4 space-y-4">
                <div className="flex flex-col items-center justify-center p-6 border-2 border-dashed border-slate-200 rounded-xl bg-slate-50/50 min-h-[180px]">
                  {isLogoBlobLoading ? (
                    <div className="flex flex-col items-center">
                      <Loader2 className="size-6 animate-spin text-teal-600" />
                      <span className="text-xs text-slate-500 mt-2">Loading logo...</span>
                    </div>
                  ) : logoUrl ? (
                    <div className="relative group max-h-32 flex items-center justify-center">
                      <img
                        src={logoUrl}
                        alt="Laboratory Logo"
                        className="max-h-28 max-w-full object-contain drop-shadow-xs"
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col items-center text-center text-slate-400">
                      <ImageOff className="size-10 mb-2 stroke-[1.5]" />
                      <span className="text-xs font-medium text-slate-600">No logo uploaded</span>
                      <span className="text-[11px] text-slate-400 mt-0.5">Transparent PNG recommended</span>
                    </div>
                  )}
                </div>

                <input
                  type="file"
                  ref={logoInputRef}
                  onChange={handleLogoFileChange}
                  accept="image/png,image/jpeg,image/webp"
                  className="hidden"
                />

                <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 pt-1">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={isLogoUploading}
                    onClick={() => logoInputRef.current?.click()}
                    className="text-xs h-9 border-teal-200 text-[#0F766E] hover:bg-teal-50 cursor-pointer w-full sm:w-auto justify-center"
                  >
                    {isLogoUploading ? (
                      <>
                        <Loader2 className="size-3.5 mr-1.5 animate-spin" />
                        Uploading...
                      </>
                    ) : (
                      <>
                        <Upload className="size-3.5 mr-1.5" />
                        {profile?.logoConfigured ? "Replace Logo" : "Upload Logo"}
                      </>
                    )}
                  </Button>

                  {profile?.logoConfigured && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteDialogType("logo")}
                      className="text-xs h-9 text-rose-600 hover:text-rose-700 hover:bg-rose-50 cursor-pointer w-full sm:w-auto justify-center"
                    >
                      <Trash2 className="size-3.5 mr-1.5" />
                      Remove
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Pathologist Signature Card */}
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-3 border-b border-slate-100">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                    <FileSignature className="size-4 text-teal-600" />
                    Authorized Pathologist Signature
                  </CardTitle>
                  {profile?.signatureConfigured ? (
                    profile?.signatureVerificationStatus === "APPROVED" ? (
                      <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] font-semibold">
                        Verified & Active
                      </Badge>
                    ) : profile?.signatureVerificationStatus === "REJECTED" ? (
                      <Badge className="bg-rose-50 text-rose-700 border-rose-200 text-[10px] font-semibold">
                        Verification Rejected
                      </Badge>
                    ) : (
                      <Badge className="bg-amber-50 text-amber-700 border-amber-200 text-[10px] font-semibold">
                        Pending Verification
                      </Badge>
                    )
                  ) : (
                    <Badge variant="outline" className="text-slate-400 text-[10px]">
                      Not Set
                    </Badge>
                  )}
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Digital signature stamped at the bottom of finalized reports. Max 2MB (PNG).
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-4 space-y-4">
                {/* Status Notice Banner */}
                {profile?.signatureConfigured && profile?.signatureVerificationStatus === "PENDING_VERIFICATION" && (
                  <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 text-xs text-amber-900 flex items-start gap-2.5">
                    <Clock className="size-4 text-amber-600 mt-0.5 shrink-0" />
                    <div>
                      <p className="font-semibold">Signature Pending Verification</p>
                      <p className="text-[11px] text-amber-700 mt-0.5 leading-relaxed">
                        Your digital signature specimen has been submitted to Super Admin for clinical verification. Reports cannot be finalized until it is approved.
                      </p>
                    </div>
                  </div>
                )}

                {profile?.signatureConfigured && profile?.signatureVerificationStatus === "APPROVED" && (
                  <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-3 text-xs text-emerald-900 flex items-start gap-2.5">
                    <CheckCircle2 className="size-4 text-emerald-600 mt-0.5 shrink-0" />
                    <div>
                      <p className="font-semibold">Verified & Active for Reports</p>
                      <p className="text-[11px] text-emerald-700 mt-0.5 leading-relaxed">
                        Verified by platform administration. This signature is automatically affixed to doctor-signed finalized PDF reports.
                      </p>
                    </div>
                  </div>
                )}

                {profile?.signatureConfigured && profile?.signatureVerificationStatus === "REJECTED" && (
                  <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 text-xs text-rose-900 flex items-start gap-2.5">
                    <AlertCircle className="size-4 text-rose-600 mt-0.5 shrink-0" />
                    <div>
                      <p className="font-semibold">Signature Verification Rejected</p>
                      <p className="text-[11px] text-rose-700 mt-0.5 leading-relaxed">
                        Reason: {profile.signatureRejectionReason || "Signature did not meet compliance requirements."} Please draw or upload a new signature.
                      </p>
                    </div>
                  </div>
                )}

                <div className="flex flex-col items-center justify-center p-6 border-2 border-dashed border-slate-200 rounded-xl bg-slate-50/50 min-h-[180px]">
                  {isSignatureBlobLoading ? (
                    <div className="flex flex-col items-center">
                      <Loader2 className="size-6 animate-spin text-teal-600" />
                      <span className="text-xs text-slate-500 mt-2">Loading signature...</span>
                    </div>
                  ) : signatureUrl ? (
                    <div className="relative group max-h-32 flex items-center justify-center">
                      <img
                        src={signatureUrl}
                        alt="Authorized Signature"
                        className="max-h-24 max-w-full object-contain filter contrast-125"
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col items-center text-center text-slate-400">
                      <FileSignature className="size-10 mb-2 stroke-[1.5]" />
                      <span className="text-xs font-medium text-slate-600">No signature uploaded</span>
                      <span className="text-[11px] text-slate-400 mt-0.5">Transparent PNG recommended</span>
                    </div>
                  )}
                </div>

                <input
                  type="file"
                  ref={signatureInputRef}
                  onChange={handleSignatureFileChange}
                  accept="image/png,image/jpeg,image/webp"
                  className="hidden"
                />

                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 pt-1">
                  <div className="grid grid-cols-2 sm:flex sm:items-center gap-2 w-full sm:w-auto">
                    <Button
                      type="button"
                      size="sm"
                      disabled={isSignatureUploading}
                      onClick={() => setIsDrawingModalOpen(true)}
                      className="text-xs h-9 bg-teal-600 hover:bg-teal-700 text-white font-medium cursor-pointer shadow-xs gap-1.5 justify-center"
                    >
                      <PenTool className="size-3.5" />
                      {profile?.signatureConfigured ? "Draw New" : "Draw Signature"}
                    </Button>

                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={isSignatureUploading}
                      onClick={() => signatureInputRef.current?.click()}
                      className="text-xs h-9 border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer gap-1.5 justify-center"
                    >
                      {isSignatureUploading ? (
                        <>
                          <Loader2 className="size-3.5 mr-1.5 animate-spin" />
                          Uploading...
                        </>
                      ) : (
                        <>
                          <Upload className="size-3.5 text-slate-500" />
                          Upload File
                        </>
                      )}
                    </Button>
                  </div>

                  {profile?.signatureConfigured && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteDialogType("signature")}
                      className="text-xs h-9 text-rose-600 hover:text-rose-700 hover:bg-rose-50 cursor-pointer w-full sm:w-auto justify-center"
                    >
                      <Trash2 className="size-3.5 mr-1.5" />
                      Remove
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* TAB 3: Report Footers & Disclaimers */}
        <TabsContent value="disclaimers" className="space-y-6 mt-0">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-xs">
              <CardHeader className="pb-4 border-b border-slate-100">
                <CardTitle className="text-base font-semibold text-slate-900">
                  Report Footer & Clinical Disclaimers
                </CardTitle>
                <CardDescription className="text-xs text-slate-500">
                  Standard legal and accreditation notices printed at the footer of all issued patient reports.
                </CardDescription>
              </CardHeader>
              <CardContent className="pt-5 space-y-5">
                <div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="reportFooterText" className="text-xs font-semibold text-slate-700">
                      Standard Report Footer Note
                    </Label>
                    <span className="text-[11px] text-slate-400">
                      {(watch("reportFooterText") || "").length} / 1000
                    </span>
                  </div>
                  <Textarea
                    id="reportFooterText"
                    rows={3}
                    placeholder="This report is digitally generated and authenticated. Verified by electronic signature under the Information Technology Act."
                    className={`mt-1.5 text-xs border-slate-200 ${errors.reportFooterText ? "border-rose-300" : ""}`}
                    {...register("reportFooterText")}
                  />
                  {errors.reportFooterText && (
                    <p className="text-[11px] text-rose-600 mt-1">{errors.reportFooterText.message}</p>
                  )}
                  <p className="text-[11px] text-slate-400 mt-1">
                    Printed directly above the clinical disclaimer on every page of generated reports.
                  </p>
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="reportDisclaimer" className="text-xs font-semibold text-slate-700">
                      Statutory Clinical Correlation Disclaimer
                    </Label>
                    <span className="text-[11px] text-slate-400">
                      {(watch("reportDisclaimer") || "").length} / 2000
                    </span>
                  </div>
                  <Textarea
                    id="reportDisclaimer"
                    rows={4}
                    placeholder="1. Laboratory test results must be clinically correlated with patient symptoms and clinical findings by the consulting physician. 2. Partial reproduction of this test report is not permitted without prior written consent..."
                    className={`mt-1.5 text-xs border-slate-200 ${errors.reportDisclaimer ? "border-rose-300" : ""}`}
                    {...register("reportDisclaimer")}
                  />
                  {errors.reportDisclaimer && (
                    <p className="text-[11px] text-rose-600 mt-1">{errors.reportDisclaimer.message}</p>
                  )}
                  <p className="text-[11px] text-slate-400 mt-1">
                    Regulatory clinical disclaimer conforming to ISO 15189 & NABL diagnostic guidelines.
                  </p>
                </div>
              </CardContent>
            </Card>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                type="submit"
                disabled={isSaving || !isDirty}
                className="w-full sm:w-auto bg-[#0F766E] hover:bg-[#115E59] text-white text-xs h-9 px-5 rounded-lg shadow-xs cursor-pointer font-semibold"
              >
                {isSaving ? (
                  <>
                    <Loader2 className="size-3.5 mr-1.5 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="size-3.5 mr-1.5" />
                    Save Disclaimers
                  </>
                )}
              </Button>
            </div>
          </form>
        </TabsContent>

        {/* TAB 4: Live Letterhead Preview */}
        <TabsContent value="preview" className="space-y-6 mt-0">
          <Card className="border-slate-200 bg-white shadow-xs overflow-hidden">
            <CardHeader className="pb-3 border-b border-slate-100 bg-slate-50/50">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <CardTitle className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                    <FileSignature className="size-4 text-teal-600" />
                    Live Simulated Diagnostic Report Letterhead
                  </CardTitle>
                  <CardDescription className="text-xs text-slate-500">
                    Real-time simulation of how your header, logo, contact, footer, and signature appear on patient reports.
                  </CardDescription>
                </div>
                <Badge variant="outline" className="text-teal-700 bg-teal-50 border-teal-200 text-xs shrink-0 self-start sm:self-auto">
                  A4 Patient Report Format
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="p-3 sm:p-6 md:p-10 bg-slate-100/60 overflow-x-auto">
              {/* Mock A4 Paper Container */}
              <div className="min-w-[320px] max-w-3xl mx-auto bg-white border border-slate-200 shadow-md rounded-xl p-4 sm:p-8 md:p-12 space-y-6 sm:space-y-8 font-sans">
                {/* 1. Header Block */}
                <div className="flex flex-col sm:flex-row items-start justify-between gap-4 sm:gap-6 border-b-2 border-teal-700 pb-5 sm:pb-6">
                  {/* Left: Logo + Name */}
                  <div className="flex items-start sm:items-center gap-3 sm:gap-4">
                    {logoUrl ? (
                      <img
                        src={logoUrl}
                        alt="Lab Logo"
                        className="size-12 sm:size-16 object-contain rounded-md shrink-0"
                      />
                    ) : (
                      <div className="size-12 sm:size-16 rounded-xl bg-teal-50 border border-teal-100 flex items-center justify-center text-teal-700 font-bold text-base sm:text-lg shrink-0">
                        {profile?.organizationName?.charAt(0) || "L"}
                      </div>
                    )}
                    <div>
                      <h2 className="text-lg sm:text-xl font-bold tracking-tight text-slate-900 leading-snug">
                        {profile?.organizationName || "Your Diagnostic Laboratory"}
                      </h2>
                      <p className="text-[11px] sm:text-xs text-teal-700 font-semibold tracking-wide uppercase mt-0.5">
                        Clinical & Pathological Laboratory Services
                      </p>
                      <p className="text-[11px] text-slate-500 mt-1 break-words">
                        {[watchedAddress1, watchedAddress2, watchedCity, watchedState, watchedPostal]
                          .filter(Boolean)
                          .join(", ") || "Laboratory Address Line 1, City, State - PIN"}
                      </p>
                    </div>
                  </div>

                  {/* Right: Contact Block */}
                  <div className="text-left sm:text-right text-xs text-slate-600 space-y-1 sm:self-start w-full sm:w-auto border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
                    {watchedPhone && (
                      <p className="flex items-center sm:justify-end gap-1.5 font-medium">
                        <Phone className="size-3 text-slate-400 shrink-0" />
                        <span>{watchedPhone}</span>
                      </p>
                    )}
                    {watchedEmail && (
                      <p className="flex items-center sm:justify-end gap-1.5 text-slate-500 break-all">
                        <Mail className="size-3 text-slate-400 shrink-0" />
                        <span>{watchedEmail}</span>
                      </p>
                    )}
                    {watchedWebsite && (
                      <p className="flex items-center sm:justify-end gap-1.5 text-teal-700 break-all">
                        <Globe className="size-3 text-teal-600 shrink-0" />
                        <span>{watchedWebsite}</span>
                      </p>
                    )}
                  </div>
                </div>

                {/* 2. Mock Patient Banner */}
                <div className="rounded-lg bg-slate-50 p-3 sm:p-4 border border-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 text-xs text-slate-700">
                  <div>
                    <span className="text-[10px] text-slate-400 uppercase font-bold block">Patient Name</span>
                    <span className="font-semibold text-slate-900 truncate block">John Doe (Sample)</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-400 uppercase font-bold block">Age / Gender</span>
                    <span>34 Yrs / Male</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-400 uppercase font-bold block">PID Code</span>
                    <span className="font-mono">PID-847291</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-400 uppercase font-bold block">Date of Report</span>
                    <span>{new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })}</span>
                  </div>
                </div>

                {/* 3. Mock Test Results Area */}
                <div className="space-y-3 py-4 border-y border-dashed border-slate-200 text-xs overflow-x-auto">
                  <div className="min-w-[280px]">
                    <div className="grid grid-cols-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider pb-1 border-b border-slate-100">
                      <span>Investigation</span>
                      <span className="text-center">Observed</span>
                      <span className="text-right">Reference</span>
                    </div>
                    <div className="grid grid-cols-3 py-2 text-slate-800 border-b border-slate-50">
                      <span className="font-medium truncate">Hemoglobin (Hb)</span>
                      <span className="font-semibold text-slate-900 text-center">14.8 g/dL</span>
                      <span className="text-slate-500 text-right">13.0 - 17.0</span>
                    </div>
                    <div className="grid grid-cols-3 py-2 text-slate-800">
                      <span className="font-medium truncate">Total Leukocyte (TLC)</span>
                      <span className="font-semibold text-slate-900 text-center">7,200 /uL</span>
                      <span className="text-slate-500 text-right">4k - 11k</span>
                    </div>
                  </div>
                </div>

                {/* 4. Footer & Signature Block */}
                <div className="pt-4 sm:pt-6 space-y-5 sm:space-y-6">
                  {/* Signature Section */}
                  <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between gap-4">
                    <div className="text-[11px] text-slate-500 max-w-sm order-2 sm:order-1">
                      {watchedFooter ||
                        "This report is digitally generated and electronically signed under the Information Technology Act."}
                    </div>

                    <div className="flex flex-col items-start sm:items-center text-left sm:text-center order-1 sm:order-2 self-end sm:self-auto">
                      {signatureUrl ? (
                        <img
                          src={signatureUrl}
                          alt="Doctor Signature"
                          className="h-12 sm:h-14 object-contain filter contrast-125 mb-1"
                        />
                      ) : (
                        <div className="h-12 sm:h-14 w-28 sm:w-32 border-b border-slate-300 flex items-center justify-center text-slate-300 text-xs italic">
                          Signature Area
                        </div>
                      )}
                      <span className="text-xs font-bold text-slate-900">
                        {profile?.signatureOwnerName || "Authorized Signatory"}
                      </span>
                      <span className="text-[10px] text-slate-500">
                        Consultant Pathologist, MD
                      </span>
                    </div>
                  </div>

                  {/* Legal Disclaimer */}
                  <div className="pt-4 border-t border-slate-200 text-[10px] text-slate-400 leading-relaxed text-justify">
                    {watchedDisclaimer ||
                      "Note: Test findings are to be clinically correlated with patient symptoms and history. For any discrepancy, contact laboratory administration within 24 hours of report issuance."}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Confirmation Dialog for Image Deletion */}
      <DeleteImageConfirmationDialog
        open={Boolean(deleteDialogType)}
        onOpenChange={(open) => !open && setDeleteDialogType(null)}
        type={deleteDialogType || "logo"}
        organizationName={profile?.organizationName || "Laboratory"}
        onConfirm={handleConfirmDelete}
        isDeleting={deleteLogoMutation.isPending || deleteSignatureMutation.isPending}
      />

      {/* Interactive Signature Drawing Modal */}
      <SignatureDrawingModal
        isOpen={isDrawingModalOpen}
        onClose={() => setIsDrawingModalOpen(false)}
        onSave={handleSaveDrawnSignature}
        isSaving={isSignatureUploading}
      />
    </div>
  );
}
