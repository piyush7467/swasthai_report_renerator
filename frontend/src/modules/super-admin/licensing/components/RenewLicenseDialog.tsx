import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  Calendar,
  CheckCircle2,
  Clock,
  Loader2,
  RefreshCw,
} from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import { usePlansQuery, useRenewLicenseMutation } from "../hooks/useLicensing";
import type { LicenseResponse } from "../types/licensingTypes";
import { LicenseStatusBadge } from "./LicenseStatusBadge";

const renewLicenseSchema = z.object({
  planRefId: z.string().min(1, "Please select an active subscription plan"),
  paymentReference: z
    .string()
    .trim()
    .max(150, "Payment reference must not exceed 150 characters")
    .optional()
    .or(z.literal("")),
});

type RenewLicenseFormValues = z.infer<typeof renewLicenseSchema>;

interface RenewLicenseDialogProps {
  organizationRefId: string;
  organizationName?: string;
  currentLicense: LicenseResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (license: LicenseResponse) => void;
}

export function RenewLicenseDialog({
  organizationRefId,
  organizationName,
  currentLicense,
  open,
  onOpenChange,
  onSuccess,
}: RenewLicenseDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isConfirming, setIsConfirming] = useState<boolean>(false);
  const [pendingValues, setPendingValues] =
    useState<RenewLicenseFormValues | null>(null);

  const { data: plans = [], isLoading: plansLoading } = usePlansQuery();
  const renewMutation = useRenewLicenseMutation();

  const activePlans = plans.filter((p) => p.active);

  const {
    register,
    handleSubmit,
    control,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<RenewLicenseFormValues>({
    resolver: zodResolver(renewLicenseSchema),
    defaultValues: {
      planRefId: currentLicense?.planRefId || "",
      paymentReference: "",
    },
  });

  const selectedPlanRefId = watch("planRefId");

  useEffect(() => {
    if (currentLicense?.planRefId) {
      setValue("planRefId", currentLicense.planRefId);
    }
  }, [currentLicense, setValue]);

  const handleClose = () => {
    reset();
    setGeneralError(null);
    setIsConfirming(false);
    setPendingValues(null);
    onOpenChange(false);
  };

  // Step 1: Form submission -> move to confirmation step
  const handlePreSubmit = (values: RenewLicenseFormValues) => {
    setGeneralError(null);
    setPendingValues(values);
    setIsConfirming(true);
  };

  // Step 2: Final confirmation execution
  const handleFinalConfirm = async () => {
    if (!pendingValues) return;
    setGeneralError(null);

    try {
      const result = await renewMutation.mutateAsync({
        organizationRefId,
        request: {
          planRefId: pendingValues.planRefId,
          paymentReference: pendingValues.paymentReference?.trim() || undefined,
        },
      });

      handleClose();
      if (onSuccess) {
        onSuccess(result);
      }
    } catch (err: unknown) {
      setIsConfirming(false); // Return to edit mode if there was an error
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError && apiError.message) {
          setGeneralError(apiError.message);
          return;
        }
      }
      setGeneralError(
        "Failed to renew license. Please verify organization status and try again.",
      );
    }
  };

  const formattedExpiry = currentLicense?.expiresAt
    ? new Date(currentLicense.expiresAt).toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      })
    : "—";

  const effectivePlanRefId =
    pendingValues?.planRefId || selectedPlanRefId || currentLicense?.planRefId;
  const chosenPlan = plans.find((p) => p.refId === effectivePlanRefId);

  // Calculate projected new expiration date
  const projectedExpiryDate = (() => {
    const baseDate = currentLicense.currentlyUsable
      ? new Date(currentLicense.expiresAt)
      : new Date();
    const projected = new Date(baseDate);
    projected.setDate(projected.getDate() + 365);
    return projected.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  })();

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-md">
        {/* View A: Confirmation Step */}
        {isConfirming ? (
          <div className="space-y-4">
            <DialogHeader>
              <div className="flex items-center gap-2">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-100 text-amber-700">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div>
                  <DialogTitle className="text-base font-semibold text-slate-900">
                    Are you sure you want to renew this license?
                  </DialogTitle>
                  <DialogDescription className="text-xs">
                    Please review the renewal terms and validity window before confirming.
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            {generalError && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Renewal Error</AlertTitle>
                <AlertDescription>{generalError}</AlertDescription>
              </Alert>
            )}

            <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Target Organization:</span>
                <span className="font-semibold text-slate-900">
                  {organizationName || organizationRefId}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Selected Plan:</span>
                <div className="flex items-center gap-1.5">
                  <span className="font-semibold text-slate-900">
                    {chosenPlan?.name || currentLicense.planName}
                  </span>
                  <Badge variant="outline" className="font-mono text-[10px] bg-white">
                    {chosenPlan?.code || currentLicense.planCode}
                  </Badge>
                </div>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Annual Rate:</span>
                <span className="font-semibold text-slate-900">
                  {chosenPlan
                    ? `${chosenPlan.currency} ${chosenPlan.annualPrice.toLocaleString()}/yr`
                    : "—"}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Payment Reference:</span>
                <span className="font-mono text-slate-800">
                  {pendingValues?.paymentReference || "(None specified)"}
                </span>
              </div>

              <div className="border-t border-slate-200 pt-3 space-y-1.5">
                <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                  Projected License Validity
                </span>
                <div className="rounded-lg border border-indigo-100 bg-indigo-50/50 p-2.5 text-xs text-indigo-900 space-y-1">
                  {currentLicense.currentlyUsable ? (
                    <div className="space-y-1">
                      <div className="flex items-center gap-1.5 font-semibold">
                        <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                        <span>Extending Existing Active Subscription</span>
                      </div>
                      <p className="text-[11px] text-indigo-700 leading-relaxed">
                        Adds 365 days to current expiration date.
                        <br />
                        <span className="font-mono">{formattedExpiry}</span> ➔{" "}
                        <strong className="font-mono text-indigo-900">{projectedExpiryDate}</strong>
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <div className="flex items-center gap-1.5 font-semibold">
                        <Clock className="h-4 w-4 text-amber-600 shrink-0" />
                        <span>Starting Fresh 365-Day Subscription Window</span>
                      </div>
                      <p className="text-[11px] text-indigo-700 leading-relaxed">
                        Because the license is expired, validity resets starting today.
                        <br />
                        Valid until: <strong className="font-mono text-indigo-900">{projectedExpiryDate}</strong>
                      </p>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <DialogFooter className="gap-2 sm:gap-0 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsConfirming(false)}
                disabled={renewMutation.isPending}
              >
                Back to Edit
              </Button>
              <Button
                type="button"
                onClick={handleFinalConfirm}
                disabled={renewMutation.isPending}
                className="bg-indigo-600 hover:bg-indigo-700 text-white gap-1.5"
              >
                {renewMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Applying Renewal...
                  </>
                ) : (
                  <>
                    <RefreshCw className="h-4 w-4" />
                    Yes, Confirm Renewal
                  </>
                )}
              </Button>
            </DialogFooter>
          </div>
        ) : (
          /* View B: Form Input Step */
          <>
            <DialogHeader>
              <div className="flex items-center gap-2">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                  <RefreshCw className="h-5 w-5" />
                </div>
                <div>
                  <DialogTitle className="text-base font-semibold">
                    Renew Organization License
                  </DialogTitle>
                  <DialogDescription className="text-xs">
                    Extend subscription duration for {organizationName || organizationRefId}.
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            {generalError && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Renewal Error</AlertTitle>
                <AlertDescription>{generalError}</AlertDescription>
              </Alert>
            )}

            <form
              onSubmit={handleSubmit(handlePreSubmit)}
              className="space-y-4 pt-2"
            >
              {/* Current License Summary */}
              <div className="rounded-lg border border-slate-200 bg-slate-50/70 p-3.5 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                    Current Subscription
                  </span>
                  <LicenseStatusBadge
                    status={currentLicense.status}
                    currentlyUsable={currentLicense.currentlyUsable}
                    showUsability
                  />
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-600">Plan:</span>
                  <span className="font-semibold text-slate-900">
                    {currentLicense.planName} ({currentLicense.planCode})
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-600">Expires:</span>
                  <span className="font-medium text-slate-800 flex items-center gap-1">
                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                    {formattedExpiry}
                  </span>
                </div>
              </div>

              {/* New / Continued Plan Selection */}
              <div className="space-y-1.5">
                <Label htmlFor="planRefId" className="text-xs font-semibold">
                  Renewal Plan <span className="text-red-500">*</span>
                </Label>
                <Controller
                  name="planRefId"
                  control={control}
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={field.onChange}
                      disabled={plansLoading}
                    >
                      <SelectTrigger id="planRefId" className="bg-white">
                        <SelectValue
                          placeholder={
                            plansLoading
                              ? "Loading plans..."
                              : "Select plan..."
                          }
                        />
                      </SelectTrigger>
                      <SelectContent>
                        {activePlans.length === 0 ? (
                          <div className="p-3 text-center text-xs text-slate-500">
                            No active plans available for renewal.
                          </div>
                        ) : (
                          activePlans.map((p) => (
                            <SelectItem key={p.refId} value={p.refId}>
                              <div className="flex items-center justify-between w-full gap-3 text-left">
                                <span className="font-semibold text-slate-900">
                                  {p.name} ({p.code})
                                </span>
                                <span className="text-xs text-slate-500">
                                  {p.currency} {p.annualPrice.toLocaleString()}/yr
                                </span>
                              </div>
                            </SelectItem>
                          ))
                        )}
                      </SelectContent>
                    </Select>
                  )}
                />
                {errors.planRefId && (
                  <p className="text-xs text-red-500">
                    {errors.planRefId.message}
                  </p>
                )}
                <p className="text-[11px] text-slate-500">
                  You may keep the current plan or switch to any active plan.
                </p>
              </div>

              {/* Payment Reference */}
              <div className="space-y-1.5">
                <Label htmlFor="paymentReference" className="text-xs font-semibold">
                  Payment Reference <span className="text-slate-400 font-normal">(Optional)</span>
                </Label>
                <Input
                  id="paymentReference"
                  placeholder="e.g., NEFT-RENEW-9901, BANK-REC-481"
                  {...register("paymentReference")}
                  className="bg-white"
                />
                {errors.paymentReference && (
                  <p className="text-xs text-red-500">
                    {errors.paymentReference.message}
                  </p>
                )}
              </div>

              {/* Renewal Window Explanation */}
              <div className="rounded-md border border-slate-200 bg-slate-50 p-2.5 flex items-start gap-2">
                <Clock className="h-4 w-4 text-slate-500 shrink-0 mt-0.5" />
                <div className="text-xs text-slate-600 leading-relaxed">
                  {currentLicense.currentlyUsable ? (
                    <span>
                      Because the subscription is active, renewal extends the expiration date by 365 days to <strong>{projectedExpiryDate}</strong>.
                    </span>
                  ) : (
                    <span>
                      Because the subscription has expired, renewal starts a new 365-day license window starting today until <strong>{projectedExpiryDate}</strong>.
                    </span>
                  )}
                </div>
              </div>

              <DialogFooter className="gap-2 sm:gap-0 pt-2">
                <Button type="button" variant="outline" onClick={handleClose}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={activePlans.length === 0}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white gap-1.5"
                >
                  Review & Renew
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </DialogFooter>
            </form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

export default RenewLicenseDialog;
