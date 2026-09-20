import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, Link2, Loader2, Save } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ApiErrorResponse } from "@/core/auth/authTypes";

import { useAssignTestMutation } from "../hooks/useAssignments";
import { useTestsQuery } from "../hooks/useTests";
import { useOrganizationsQuery } from "../../hooks/useOrganizations";

const assignTestSchema = z
  .object({
    organizationRefId: z
      .string()
      .min(1, "Organization is required"),
    testRefId: z
      .string()
      .min(1, "Test is required"),
    effectiveFrom: z.string().optional().or(z.literal("")),
    effectiveUntil: z.string().optional().or(z.literal("")),
  })
  .superRefine((data, ctx) => {
    if (data.effectiveFrom && data.effectiveUntil) {
      if (new Date(data.effectiveUntil) < new Date(data.effectiveFrom)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["effectiveUntil"],
          message: "Effective until date cannot be before effective from date",
        });
      }
    }
  });

type AssignTestFormValues = z.infer<typeof assignTestSchema>;

interface AssignTestDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defaultOrganizationRefId?: string;
}

export function AssignTestDialog({
  open,
  onOpenChange,
  defaultOrganizationRefId,
}: AssignTestDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);

  const assignMutation = useAssignTestMutation();

  const { data: orgsData, isLoading: orgsLoading } = useOrganizationsQuery({
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const { data: testsData, isLoading: testsLoading } = useTestsQuery({
    size: 100,
    status: "ACTIVE",
    sort: "name",
    direction: "asc",
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AssignTestFormValues>({
    resolver: zodResolver(assignTestSchema),
    defaultValues: {
      organizationRefId: defaultOrganizationRefId || "",
      testRefId: "",
      effectiveFrom: "",
      effectiveUntil: "",
    },
    mode: "onBlur",
  });

  const onSubmit = async (values: AssignTestFormValues) => {
    setGeneralError(null);
    try {
      await assignMutation.mutateAsync({
        organizationRefId: values.organizationRefId.trim(),
        testRefId: values.testRefId.trim(),
        effectiveFrom: values.effectiveFrom || undefined,
        effectiveUntil: values.effectiveUntil || undefined,
      });

      reset();
      onOpenChange(false);
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (
            apiError.code === "RESOURCE_ALREADY_EXISTS" ||
            apiError.message.toLowerCase().includes("already assigned")
          ) {
            setGeneralError("This test is already assigned to the selected organization.");
            return;
          }
          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }
          setGeneralError(apiError.message || "Failed to assign test.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while assigning the test.");
    }
  };

  const isPending = isSubmitting || assignMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <Link2 className="h-5 w-5 text-blue-600" />
            <DialogTitle className="text-lg font-semibold text-slate-900">
              Assign Test to Organization
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            Grant permission for an organization to perform and report this diagnostic test.
          </DialogDescription>
        </DialogHeader>

        {generalError && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Cannot Assign</AlertTitle>
            <AlertDescription className="text-xs">{generalError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-2">
          {/* Organization Select */}
          <div className="space-y-1.5">
            <Label htmlFor="assign-org" className="text-xs font-semibold">
              Organization <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="organizationRefId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={orgsLoading}
                >
                  <SelectTrigger id="assign-org">
                    <SelectValue placeholder="Select organization..." />
                  </SelectTrigger>
                  <SelectContent>
                    {orgsData?.content.map((org) => (
                      <SelectItem key={org.refId} value={org.refId}>
                        {org.name} ({org.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.organizationRefId && (
              <p className="text-xs text-red-500">
                {errors.organizationRefId.message}
              </p>
            )}
          </div>

          {/* Test Select */}
          <div className="space-y-1.5">
            <Label htmlFor="assign-test" className="text-xs font-semibold">
              Diagnostic Test <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="testRefId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={testsLoading}
                >
                  <SelectTrigger id="assign-test">
                    <SelectValue placeholder="Select test from catalog..." />
                  </SelectTrigger>
                  <SelectContent>
                    {testsData?.content.map((t) => (
                      <SelectItem key={t.refId} value={t.refId}>
                        {t.name} ({t.code}) · {t.categoryName || t.categoryRefId}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.testRefId && (
              <p className="text-xs text-red-500">{errors.testRefId.message}</p>
            )}
          </div>

          {/* Validity dates */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="effectiveFrom" className="text-xs font-semibold">
                Effective From
              </Label>
              <Input
                id="effectiveFrom"
                type="date"
                {...register("effectiveFrom")}
              />
              {errors.effectiveFrom && (
                <p className="text-xs text-red-500">
                  {errors.effectiveFrom.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="effectiveUntil" className="text-xs font-semibold">
                Effective Until
              </Label>
              <Input
                id="effectiveUntil"
                type="date"
                {...register("effectiveUntil")}
              />
              {errors.effectiveUntil && (
                <p className="text-xs text-red-500">
                  {errors.effectiveUntil.message}
                </p>
              )}
            </div>
          </div>

          <DialogFooter className="mt-4 flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isPending}
              className="bg-blue-600 hover:bg-blue-700 text-white min-w-28"
            >
              {isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Assigning...
                </>
              ) : (
                <>
                  <Save className="mr-2 h-4 w-4" />
                  Assign Test
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
