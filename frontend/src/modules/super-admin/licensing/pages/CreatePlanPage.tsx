import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  ArrowLeft,
  CreditCard,
  Loader2,
  Save,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { useCreatePlanMutation } from "../hooks/useLicensing";

const createPlanSchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "Plan code is required")
    .max(50, "Plan code must not exceed 50 characters")
    .regex(
      /^[A-Za-z0-9_-]+$/,
      "Plan code may only contain letters, numbers, underscores, and hyphens",
    ),
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
  active: z.boolean().default(true),
});

type CreatePlanFormValues = z.infer<typeof createPlanSchema>;

export function CreatePlanPage() {
  const navigate = useNavigate();
  const [generalError, setGeneralError] = useState<string | null>(null);
  const createMutation = useCreatePlanMutation();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreatePlanFormValues>({
    resolver: zodResolver(createPlanSchema) as any,
    defaultValues: {
      code: "",
      name: "",
      description: "",
      annualPrice: 0,
      currency: "INR",
      active: true,
    },
  });

  const onSubmit = async (values: CreatePlanFormValues) => {
    setGeneralError(null);
    try {
      await createMutation.mutateAsync({
        code: values.code.trim().toUpperCase(),
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        annualPrice: Number(values.annualPrice),
        currency: values.currency.trim().toUpperCase(),
        active: values.active,
      });

      navigate("/super-admin/licensing/plans");
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (
            apiError.code === "RESOURCE_ALREADY_EXISTS" ||
            apiError.message?.toLowerCase().includes("already exists")
          ) {
            setError("code", { message: apiError.message });
            return;
          }
          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }
          setGeneralError(apiError.message || "Failed to create plan.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while creating the plan.");
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      {/* Top Header */}
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
        <h1 className="text-xl font-bold tracking-tight text-slate-900">
          Create Subscription Plan
        </h1>
      </div>

      {generalError && (
        <Alert variant="destructive">
          <AlertTitle>Creation Error</AlertTitle>
          <AlertDescription>{generalError}</AlertDescription>
        </Alert>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <Card className="border-slate-200 bg-white shadow-xs">
          <CardHeader>
            <div className="flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-slate-700" />
              <CardTitle className="text-base font-semibold">
                Plan Configuration
              </CardTitle>
            </div>
            <CardDescription className="text-xs">
              Define the commercial subscription terms for tenant organizations.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {/* Code & Name */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="code" className="text-xs font-semibold">
                  Plan Code <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="code"
                  placeholder="e.g. BASIC, PROFESSIONAL, ENTERPRISE"
                  {...register("code")}
                  onChange={(e) =>
                    setValue("code", e.target.value.toUpperCase())
                  }
                  className="font-mono uppercase bg-white"
                />
                {errors.code && (
                  <p className="text-xs text-red-500">{errors.code.message}</p>
                )}
                <p className="text-[11px] text-slate-500">
                  Unique identifier. Cannot be modified after creation.
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
                placeholder="Key scope, clinical tier description, or commercial notes..."
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
                    placeholder="0.00"
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
                <p className="text-[11px] text-slate-500">
                  Backend models annual licensing duration (365 days).
                </p>
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

            {/* Active Status */}
            <div className="pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label htmlFor="active" className="text-xs font-semibold">
                    Active for Licensing
                  </Label>
                  <p className="text-[11px] text-slate-500">
                    When active, this plan is immediately available for new organization license activations and renewals.
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
                Creating Plan...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                Save Plan
              </>
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}

export default CreatePlanPage;
