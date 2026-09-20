import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, Edit, Loader2, Save } from "lucide-react";

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

import { useUpdateAssignmentMutation } from "../hooks/useAssignments";
import type {
  OrganizationTestResponse,
  OrganizationTestStatus,
} from "../types/assignmentTypes";

const updateAssignmentSchema = z
  .object({
    status: z.enum(["ACTIVE", "INACTIVE"]),
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

type UpdateAssignmentFormValues = z.infer<typeof updateAssignmentSchema>;

interface UpdateAssignmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  assignment: OrganizationTestResponse | null;
}

export function UpdateAssignmentDialog({
  open,
  onOpenChange,
  assignment,
}: UpdateAssignmentDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);

  const updateMutation = useUpdateAssignmentMutation();

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<UpdateAssignmentFormValues>({
    resolver: zodResolver(updateAssignmentSchema),
    defaultValues: {
      status: "ACTIVE",
      effectiveFrom: "",
      effectiveUntil: "",
    },
    mode: "onBlur",
  });

  useEffect(() => {
    if (assignment) {
      reset({
        status: assignment.status,
        effectiveFrom: assignment.effectiveFrom || "",
        effectiveUntil: assignment.effectiveUntil || "",
      });
    }
  }, [assignment, reset, open]);

  const onSubmit = async (values: UpdateAssignmentFormValues) => {
    if (!assignment) return;
    setGeneralError(null);
    try {
      await updateMutation.mutateAsync({
        refId: assignment.refId,
        request: {
          status: values.status as OrganizationTestStatus,
          effectiveFrom: values.effectiveFrom || undefined,
          effectiveUntil: values.effectiveUntil || undefined,
        },
      });

      onOpenChange(false);
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
          setGeneralError(apiError.message || "Failed to update assignment.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while updating the assignment.");
    }
  };

  if (!assignment) return null;

  const isPending = isSubmitting || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <Edit className="h-5 w-5 text-blue-600" />
            <DialogTitle className="text-lg font-semibold text-slate-900">
              Update Test Assignment
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            {assignment.organizationName} · {assignment.testName} ({assignment.testCode})
          </DialogDescription>
        </DialogHeader>

        {generalError && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Cannot Update</AlertTitle>
            <AlertDescription className="text-xs">{generalError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <Label htmlFor="assign-status" className="text-xs font-semibold">
              Assignment Status <span className="text-red-500">*</span>
            </Label>
            <Controller
              name="status"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="assign-status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ACTIVE">Active (Available for reporting)</SelectItem>
                    <SelectItem value="INACTIVE">Inactive (Suspended)</SelectItem>
                  </SelectContent>
                </Select>
              )}
            />
          </div>

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
                  Saving...
                </>
              ) : (
                <>
                  <Save className="mr-2 h-4 w-4" />
                  Save Changes
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
