import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, CheckCircle2, KeyRound, Loader2, ShieldCheck } from "lucide-react";

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import { usePlansQuery, useActivateLicenseMutation } from "../hooks/useLicensing";
import type { LicenseResponse } from "../types/licensingTypes";

const activateLicenseSchema = z.object({
  planRefId: z.string().min(1, "Please select an active subscription plan"),
  paymentReference: z
    .string()
    .trim()
    .max(150, "Payment reference must not exceed 150 characters")
    .optional()
    .or(z.literal("")),
});

type ActivateLicenseFormValues = z.infer<typeof activateLicenseSchema>;

interface ActivateLicenseDialogProps {
  organizationRefId: string;
  organizationName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: (license: LicenseResponse) => void;
}

export function ActivateLicenseDialog({
  organizationRefId,
  organizationName,
  open,
  onOpenChange,
  onSuccess,
}: ActivateLicenseDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);
  const { data: plans = [], isLoading: plansLoading } = usePlansQuery();
  const activateMutation = useActivateLicenseMutation();

  const activePlans = plans.filter((p) => p.active);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ActivateLicenseFormValues>({
    resolver: zodResolver(activateLicenseSchema),
    defaultValues: {
      planRefId: "",
      paymentReference: "",
    },
  });

  const handleClose = () => {
    reset();
    setGeneralError(null);
    onOpenChange(false);
  };

  const onSubmit = async (values: ActivateLicenseFormValues) => {
    setGeneralError(null);
    try {
      const result = await activateMutation.mutateAsync({
        organizationRefId,
        request: {
          planRefId: values.planRefId,
          paymentReference: values.paymentReference?.trim() || undefined,
        },
      });

      handleClose();
      if (onSuccess) {
        onSuccess(result);
      }
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (apiError.message) {
            setGeneralError(apiError.message);
            return;
          }
        }
      }
      setGeneralError("Failed to activate license. Please verify organization status and try again.");
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-base font-semibold">
                Activate Organization License
              </DialogTitle>
              <DialogDescription className="text-xs">
                Issue a 1-year active license for this healthcare organization.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {generalError && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Activation Error</AlertTitle>
            <AlertDescription>{generalError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-2">
          {/* Organization Indicator */}
          <div className="rounded-md border border-slate-100 bg-slate-50/70 p-3 space-y-1">
            <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
              Target Organization
            </p>
            <p className="text-sm font-semibold text-slate-900 truncate">
              {organizationName || organizationRefId}
            </p>
            <p className="text-xs font-mono text-slate-500 truncate">
              ID: {organizationRefId}
            </p>
          </div>

          {/* Plan Selection */}
          <div className="space-y-1.5">
            <Label htmlFor="planRefId" className="text-xs font-semibold">
              Subscription Plan <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="planRefId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={plansLoading || isSubmitting}
                >
                  <SelectTrigger id="planRefId" className="bg-white">
                    <SelectValue placeholder={plansLoading ? "Loading plans..." : "Select active plan..."} />
                  </SelectTrigger>
                  <SelectContent>
                    {activePlans.length === 0 ? (
                      <div className="p-3 text-center text-xs text-slate-500">
                        No active plans available. Please create or activate a plan first.
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
              <p className="text-xs text-red-500">{errors.planRefId.message}</p>
            )}
            <p className="text-[11px] text-slate-500">
              Only active plans configured in the system can be assigned.
            </p>
          </div>

          {/* Payment Reference */}
          <div className="space-y-1.5">
            <Label htmlFor="paymentReference" className="text-xs font-semibold">
              Payment Reference <span className="text-slate-400 font-normal">(Optional)</span>
            </Label>
            <Input
              id="paymentReference"
              placeholder="e.g., NEFT-883921, BANK-CHQ-1049, INVOICE-2026-09"
              {...register("paymentReference")}
              disabled={isSubmitting}
              className="bg-white"
            />
            {errors.paymentReference && (
              <p className="text-xs text-red-500">{errors.paymentReference.message}</p>
            )}
            <p className="text-[11px] text-slate-500">
              Manual bank transfer, invoice number, or check verification reference.
            </p>
          </div>

          {/* Backend Controlled Duration Notice */}
          <div className="rounded-md border border-emerald-100 bg-emerald-50/50 p-2.5 flex items-start gap-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
            <p className="text-xs text-emerald-900 leading-relaxed">
              License validity is automatically set to <strong>365 days</strong> from activation by the backend licensing engine.
            </p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || activePlans.length === 0}
              className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Activating...
                </>
              ) : (
                <>
                  <KeyRound className="h-4 w-4" />
                  Activate License
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
