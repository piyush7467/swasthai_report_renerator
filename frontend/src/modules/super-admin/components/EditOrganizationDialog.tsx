import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, Edit, Loader2, Lock } from "lucide-react";

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
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import type { OrganizationResponse } from "../types/organizationTypes";
import { useUpdateOrganizationMutation } from "../hooks/useOrganizations";

const editOrganizationSchema = z.object({
  name: z
    .string()
    .min(1, "Organization name is required")
    .max(150, "Organization name must not exceed 150 characters"),
  code: z
    .string()
    .min(1, "Organization code is required")
    .max(50, "Organization code must not exceed 50 characters")
    .regex(
      /^[A-Za-z0-9_-]+$/,
      "Code can only contain letters, numbers, hyphens, and underscores",
    ),
});

type EditOrganizationFormValues = z.infer<typeof editOrganizationSchema>;

interface EditOrganizationDialogProps {
  organization: OrganizationResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditOrganizationDialog({
  organization,
  open,
  onOpenChange,
}: EditOrganizationDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);
  const updateMutation = useUpdateOrganizationMutation();

  const isDisabled = organization?.status === "DISABLED";

  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<EditOrganizationFormValues>({
    resolver: zodResolver(editOrganizationSchema),
    defaultValues: {
      name: organization?.name ?? "",
      code: organization?.code ?? "",
    },
    mode: "onBlur",
  });

  useEffect(() => {
    if (organization) {
      reset({
        name: organization.name,
        code: organization.code,
      });
      setGeneralError(null);
    }
  }, [organization, reset]);

  const codeValue = watch("code");

  const onSubmit = async (values: EditOrganizationFormValues) => {
    if (!organization || isDisabled) return;

    setGeneralError(null);
    try {
      await updateMutation.mutateAsync({
        refId: organization.refId,
        data: {
          name: values.name.trim(),
          code: values.code.trim().toUpperCase(),
        },
      });
      onOpenChange(false);
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const apiError = error.response?.data;
        if (apiError) {
          if (apiError.code === "RESOURCE_ALREADY_EXISTS") {
            const message = apiError.message.toLowerCase();
            if (message.includes("name")) {
              setError("name", { message: apiError.message });
            } else if (message.includes("code")) {
              setError("code", { message: apiError.message });
            } else {
              setGeneralError(apiError.message);
            }
            return;
          }

          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              if (field === "name" || field === "code") {
                setError(field, { message: msg });
              }
            });
            return;
          }

          setGeneralError(
            apiError.message || "Failed to update organization.",
          );
          return;
        }
      }
      setGeneralError(
        "Unable to connect to the server. Please check your connection and try again.",
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={(val) => !isSubmitting && onOpenChange(val)}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
              <Edit className="h-4 w-4" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                Edit Organization
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                Ref ID: {organization?.refId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isDisabled ? (
          <Alert variant="destructive" className="border-rose-200 bg-rose-50 py-3 text-rose-800">
            <Lock className="h-4 w-4 text-rose-600" />
            <AlertTitle className="text-xs font-semibold">Modification Locked</AlertTitle>
            <AlertDescription className="text-xs text-rose-700">
              This organization is in a terminal DISABLED state and cannot be modified.
            </AlertDescription>
          </Alert>
        ) : (
          <>
            {generalError && (
              <Alert variant="destructive" className="border-red-200 bg-red-50 py-2.5 text-red-800">
                <AlertCircle className="h-4 w-4 text-red-600" />
                <AlertTitle className="text-xs font-semibold">Error</AlertTitle>
                <AlertDescription className="text-xs text-red-700">
                  {generalError}
                </AlertDescription>
              </Alert>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <Label htmlFor="edit-org-name" className="text-xs font-medium text-slate-700">
                  Organization Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="edit-org-name"
                  disabled={isSubmitting}
                  className={errors.name ? "border-red-300 focus-visible:ring-red-100" : ""}
                  {...register("name")}
                />
                {errors.name && (
                  <p className="text-xs text-red-600">{errors.name.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <Label htmlFor="edit-org-code" className="text-xs font-medium text-slate-700">
                    Organization Code <span className="text-red-500">*</span>
                  </Label>
                  {codeValue && (
                    <span className="text-[11px] font-mono text-slate-400">
                      Normalized: {codeValue.trim().toUpperCase()}
                    </span>
                  )}
                </div>
                <Input
                  id="edit-org-code"
                  disabled={isSubmitting}
                  className={errors.code ? "border-red-300 focus-visible:ring-red-100" : "font-mono uppercase"}
                  {...register("code")}
                />
                {errors.code && (
                  <p className="text-xs text-red-600">{errors.code.message}</p>
                )}
              </div>

              <DialogFooter className="pt-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isSubmitting}
                  onClick={() => onOpenChange(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting || !isDirty}
                  className="bg-slate-900 text-white hover:bg-slate-800"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    "Save Changes"
                  )}
                </Button>
              </DialogFooter>
            </form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
