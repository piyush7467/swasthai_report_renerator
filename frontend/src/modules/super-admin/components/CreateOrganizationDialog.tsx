import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, Building2, Loader2, Plus } from "lucide-react";

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
  DialogTrigger,
} from "@/components/ui/dialog";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import { useCreateOrganizationMutation } from "../hooks/useOrganizations";

const createOrganizationSchema = z.object({
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

type CreateOrganizationFormValues = z.infer<typeof createOrganizationSchema>;

interface CreateOrganizationDialogProps {
  trigger?: React.ReactNode;
}

export function CreateOrganizationDialog({
  trigger,
}: CreateOrganizationDialogProps) {
  const [open, setOpen] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const createMutation = useCreateOrganizationMutation();

  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateOrganizationFormValues>({
    resolver: zodResolver(createOrganizationSchema),
    defaultValues: {
      name: "",
      code: "",
    },
    mode: "onBlur",
  });

  const codeValue = watch("code");

  const onSubmit = async (values: CreateOrganizationFormValues) => {
    setGeneralError(null);
    try {
      await createMutation.mutateAsync({
        name: values.name.trim(),
        code: values.code.trim().toUpperCase(),
      });
      reset();
      setOpen(false);
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
            apiError.message || "Failed to create organization.",
          );
          return;
        }
      }
      setGeneralError(
        "Unable to connect to the server. Please check your connection and try again.",
      );
    }
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!isSubmitting) {
      setOpen(nextOpen);
      if (!nextOpen) {
        reset();
        setGeneralError(null);
      }
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button className="bg-slate-900 text-white hover:bg-slate-800">
            <Plus className="mr-2 h-4 w-4" />
            Create Organization
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
              <Building2 className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                New Organization
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Register a new diagnostic laboratory tenant in SwasthAI.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

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
          {/* Organization Name */}
          <div className="space-y-1.5">
            <Label htmlFor="create-org-name" className="text-xs font-medium text-slate-700">
              Organization Name <span className="text-red-500">*</span>
            </Label>
            <Input
              id="create-org-name"
              placeholder="e.g. Apex Clinical Laboratories"
              disabled={isSubmitting}
              className={errors.name ? "border-red-300 focus-visible:ring-red-100" : ""}
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Organization Code */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="create-org-code" className="text-xs font-medium text-slate-700">
                Organization Code <span className="text-red-500">*</span>
              </Label>
              {codeValue && (
                <span className="text-[11px] font-mono text-slate-400">
                  Normalized: {codeValue.trim().toUpperCase()}
                </span>
              )}
            </div>
            <Input
              id="create-org-code"
              placeholder="e.g. APEX-LAB"
              disabled={isSubmitting}
              className={errors.code ? "border-red-300 focus-visible:ring-red-100" : "font-mono uppercase"}
              {...register("code")}
            />
            {errors.code ? (
              <p className="text-xs text-red-600">{errors.code.message}</p>
            ) : (
              <p className="text-[11px] text-slate-500">
                Unique identifier used in references and laboratory codes (max 50 chars).
              </p>
            )}
          </div>

          <DialogFooter className="pt-3">
            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={() => handleOpenChange(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting}
              className="bg-slate-900 text-white hover:bg-slate-800"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Creating...
                </>
              ) : (
                "Create Organization"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
