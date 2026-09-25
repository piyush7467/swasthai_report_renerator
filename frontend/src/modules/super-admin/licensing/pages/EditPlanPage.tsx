import { useState, useEffect } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  ArrowLeft,
  CreditCard,
  FileText,
  Loader2,
  Save,
  Users,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import { usePlanQuery, useUpdatePlanMutation } from "../hooks/useLicensing";

const editPlanSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Plan name is required")
    .max(100, "Plan name must not exceed 100 characters"),
  description: z
    .string()
    .trim()
    .max(500, "Description must not exceed 500 characters")
    .optional()
    .or(z.literal("")),
  annualPrice: z
    .preprocess(
      (val) => (val === "" || val == null ? NaN : Number(val)),
      z
        .number()
        .min(0, "Annual price cannot be negative")
        .max(99999999.99, "Annual price is too large"),
    ),
  currency: z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{3}$/, "Currency must be a 3-letter ISO code (e.g. INR, USD)"),
  maxLabStaff: z
    .preprocess(
      (val) => (val === "" || val == null ? 3 : Number(val)),
      z
        .number()
        .int("Must be an integer")
        .min(1, "At least 1 staff seat is required")
        .max(10000, "Limit is too high"),
    ),
  maxReportsPerMonth: z
    .preprocess(
      (val) => (val === "" || val == null ? 100 : Number(val)),
      z
        .number()
        .int("Must be an integer")
        .min(0, "Monthly quota cannot be negative"),
    ),
  maxReportsPerDay: z
    .preprocess(
      (val) => (val === "" || val == null ? 25 : Number(val)),
      z
        .number()
        .int("Must be an integer")
        .min(0, "Daily quota cannot be negative"),
    ),
  active: z.boolean(),
});

type EditPlanFormValues = z.infer<typeof editPlanSchema>;

export function EditPlanPage() {
  const { refId } = useParams<{ refId: string }>();
  const navigate = useNavigate();
  const [generalError, setGeneralError] = useState<string | null>(null);

  const { data: plan, isLoading, isError, error } = usePlanQuery(refId);
  const updateMutation = useUpdatePlanMutation();

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EditPlanFormValues>({
    resolver: zodResolver(editPlanSchema) as any,
    defaultValues: {
      name: "",
      description: "",
      annualPrice: 0,
      currency: "INR",
      maxLabStaff: 3,
      maxReportsPerMonth: 100,
      maxReportsPerDay: 25,
      active: true,
    },
  });

  useEffect(() => {
    if (plan) {
      reset({
        name: plan.name,
        description: plan.description || "",
        annualPrice: Number(plan.annualPrice),
        currency: plan.currency || "INR",
        maxLabStaff: plan.maxLabStaff ?? 3,
        maxReportsPerMonth: plan.maxReportsPerMonth ?? 100,
        maxReportsPerDay: plan.maxReportsPerDay ?? 25,
        active: plan.active,
      });
    }
  }, [plan, reset]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (isError || !plan) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Alert variant="destructive">
          <AlertTitle>Plan Not Found</AlertTitle>
          <AlertDescription>
            {error instanceof Error
              ? error.message
              : "Unable to load the requested subscription plan."}
          </AlertDescription>
        </Alert>
        <Button variant="outline" onClick={() => navigate("/super-admin/licensing/plans")}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Plans
        </Button>
      </div>
    );
  }

  const onSubmit = async (values: EditPlanFormValues) => {
    if (!refId) return;
    setGeneralError(null);
    try {
      await updateMutation.mutateAsync({
        planRefId: refId,
        request: {
          name: values.name.trim(),
          description: values.description?.trim() || undefined,
          annualPrice: Number(values.annualPrice),
          currency: values.currency.trim().toUpperCase(),
          maxLabStaff: Number(values.maxLabStaff),
          maxReportsPerMonth: Number(values.maxReportsPerMonth),
          maxReportsPerDay: Number(values.maxReportsPerDay),
          active: values.active,
        },
      });

      navigate("/super-admin/licensing/plans");
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }
          setGeneralError(apiError.message || "Failed to update plan.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while saving the plan.");
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button
          variant="outline"
          size="sm"
          asChild
          className="text-slate-600 hover:text-slate-900"
        >
          <Link to="/super-admin/licensing/plans">
            <ArrowLeft className="mr-1.5 h-4 w-4" />
            Plans
          </Link>
        </Button>
        <span className="text-slate-300">/</span>
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold tracking-tight text-slate-900">
            Edit Plan: {plan.name}
          </h1>
          <Badge variant="outline" className="font-mono text-xs bg-white">
            {plan.code}
          </Badge>
        </div>
      </div>

      {generalError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Update Error</AlertTitle>
          <AlertDescription>{generalError}</AlertDescription>
        </Alert>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Card className="border-slate-200 bg-white shadow-xs">
          <CardHeader>
            <div className="flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-slate-700" />
              <CardTitle className="text-base font-semibold">
                Plan Properties
              </CardTitle>
            </div>
            <CardDescription className="text-xs">
              Update name, description, annual price, and activation status.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {/* Readonly Code & Editable Name */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="code" className="text-xs font-semibold text-slate-500">
                  Plan Code (Immutable)
                </Label>
                <Input
                  id="code"
                  value={plan.code}
                  disabled
                  className="font-mono uppercase bg-slate-50 cursor-not-allowed text-slate-600"
                />
                <p className="text-[11px] text-slate-400">
                  Code cannot be changed to protect existing license references.
                </p>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="name" className="text-xs font-semibold">
                  Plan Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="name"
                  placeholder="e.g. Professional Diagnostic Tier"
                  {...register("name")}
                  className="bg-white"
                />
                {errors.name && (
                  <p className="text-xs text-red-500">{errors.name.message}</p>
                )}
              </div>
            </div>

            {/* Description */}
            <div className="space-y-1.5">
              <Label htmlFor="description" className="text-xs font-semibold">
                Description <span className="text-slate-400 font-normal">(Optional)</span>
              </Label>
              <Textarea
                id="description"
                rows={3}
                {...register("description")}
                className="bg-white"
              />
              {errors.description && (
                <p className="text-xs text-red-500">
                  {errors.description.message}
                </p>
              )}
            </div>

            {/* Pricing & Currency */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="annualPrice" className="text-xs font-semibold">
                  Annual Price <span className="text-red-500">*</span>
                </Label>
                <div className="relative">
                  <Input
                    id="annualPrice"
                    type="number"
                    step="0.01"
                    min="0"
                    {...register("annualPrice")}
                    className="bg-white font-medium"
                  />
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400">
                    / year
                  </div>
                </div>
                {errors.annualPrice && (
                  <p className="text-xs text-red-500">
                    {errors.annualPrice.message}
                  </p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="currency" className="text-xs font-semibold">
                  Currency <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="currency"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={(val) => field.onChange(val.toUpperCase())}
                    >
                      <SelectTrigger id="currency" className="bg-white font-mono">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="INR">INR (₹)</SelectItem>
                        <SelectItem value="USD">USD ($)</SelectItem>
                        <SelectItem value="EUR">EUR (€)</SelectItem>
                        <SelectItem value="GBP">GBP (£)</SelectItem>
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.currency && (
                  <p className="text-xs text-red-500">
                    {errors.currency.message}
                  </p>
                )}
              </div>
            </div>

            {/* Resource & Capacity Limits */}
            <div className="pt-2 border-t border-slate-100">
              <div className="mb-3">
                <Label className="text-xs font-semibold text-slate-900">
                  Capacity & Usage Quotas
                </Label>
                <p className="text-[11px] text-slate-500">
                  Configure maximum active team seats and report generation quotas. Enter 0 for unlimited reports.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                {/* Staff Seats */}
                <div className="space-y-1.5">
                  <Label htmlFor="maxLabStaff" className="text-xs font-semibold flex items-center gap-1.5">
                    <Users className="h-3.5 w-3.5 text-slate-500" />
                    Staff License Seats <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="maxLabStaff"
                    type="number"
                    min="1"
                    placeholder="3"
                    {...register("maxLabStaff")}
                    className="bg-white font-medium"
                  />
                  {errors.maxLabStaff && (
                    <p className="text-xs text-red-500">{errors.maxLabStaff.message}</p>
                  )}
                  <p className="text-[11px] text-slate-400">
                    Max active staff accounts (min 1).
                  </p>
                </div>

                {/* Monthly Reports Quota */}
                <div className="space-y-1.5">
                  <Label htmlFor="maxReportsPerMonth" className="text-xs font-semibold flex items-center gap-1.5">
                    <FileText className="h-3.5 w-3.5 text-slate-500" />
                    Reports / Month <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="maxReportsPerMonth"
                    type="number"
                    min="0"
                    placeholder="100"
                    {...register("maxReportsPerMonth")}
                    className="bg-white font-medium"
                  />
                  {errors.maxReportsPerMonth && (
                    <p className="text-xs text-red-500">{errors.maxReportsPerMonth.message}</p>
                  )}
                  <p className="text-[11px] text-slate-400">
                    0 = unlimited reports.
                  </p>
                </div>

                {/* Daily Reports Quota */}
                <div className="space-y-1.5">
                  <Label htmlFor="maxReportsPerDay" className="text-xs font-semibold flex items-center gap-1.5">
                    <FileText className="h-3.5 w-3.5 text-slate-500" />
                    Reports / Day <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="maxReportsPerDay"
                    type="number"
                    min="0"
                    placeholder="25"
                    {...register("maxReportsPerDay")}
                    className="bg-white font-medium"
                  />
                  {errors.maxReportsPerDay && (
                    <p className="text-xs text-red-500">{errors.maxReportsPerDay.message}</p>
                  )}
                  <p className="text-[11px] text-slate-400">
                    0 = unlimited daily reports.
                  </p>
                </div>
              </div>
            </div>

            {/* Active Status */}
            <div className="pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label htmlFor="active" className="text-xs font-semibold">
                    Plan Availability
                  </Label>
                  <p className="text-[11px] text-slate-500">
                    Deactivating this plan prevents new activations, but existing licenses continue functioning until expiry.
                  </p>
                </div>
                <Controller
                  name="active"
                  control={control}
                  render={({ field }) => (
                    <input
                      id="active"
                      type="checkbox"
                      checked={field.value}
                      onChange={(e) => field.onChange(e.target.checked)}
                      className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-950"
                    />
                  )}
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate("/super-admin/licensing/plans")}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isSubmitting}
            className="bg-slate-900 hover:bg-slate-800 text-white gap-1.5 shadow-xs"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Saving Changes...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                Update Plan
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default EditPlanPage;
