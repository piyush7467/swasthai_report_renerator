import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { AlertCircle, FolderPlus, Loader2, Save } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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

import {
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
} from "../hooks/useCategories";
import type {
  TestCategoryResponse,
  TestCategoryStatus,
} from "../types/categoryTypes";

const categorySchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, "Category code is required")
    .max(50, "Category code must not exceed 50 characters"),
  name: z
    .string()
    .trim()
    .min(1, "Category name is required")
    .max(100, "Category name must not exceed 100 characters"),
  description: z
    .string()
    .trim()
    .max(500, "Description must not exceed 500 characters")
    .optional()
    .or(z.literal("")),
  status: z.enum(["ACTIVE", "INACTIVE"]).optional(),
});

type CategoryFormValues = z.infer<typeof categorySchema>;

interface CategoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  categoryToEdit?: TestCategoryResponse | null;
}

export function CategoryDialog({
  open,
  onOpenChange,
  categoryToEdit,
}: CategoryDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);

  const isEdit = Boolean(categoryToEdit);
  const createMutation = useCreateCategoryMutation();
  const updateMutation = useUpdateCategoryMutation();

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      code: "",
      name: "",
      description: "",
      status: "ACTIVE",
    },
    mode: "onBlur",
  });

  useEffect(() => {
    if (categoryToEdit) {
      reset({
        code: categoryToEdit.code,
        name: categoryToEdit.name,
        description: categoryToEdit.description || "",
        status: categoryToEdit.status,
      });
    } else {
      reset({
        code: "",
        name: "",
        description: "",
        status: "ACTIVE",
      });
    }
  }, [categoryToEdit, reset, open]);

  const onSubmit = async (values: CategoryFormValues) => {
    setGeneralError(null);
    try {
      if (isEdit && categoryToEdit) {
        await updateMutation.mutateAsync({
          refId: categoryToEdit.refId,
          request: {
            code: values.code.trim().toUpperCase(),
            name: values.name.trim(),
            description: values.description?.trim() || undefined,
            status: values.status as TestCategoryStatus,
          },
        });
      } else {
        await createMutation.mutateAsync({
          code: values.code.trim().toUpperCase(),
          name: values.name.trim(),
          description: values.description?.trim() || undefined,
        });
      }

      onOpenChange(false);
    } catch (err: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(err)) {
        const apiError = err.response?.data;
        if (apiError) {
          if (
            apiError.code === "RESOURCE_ALREADY_EXISTS" ||
            apiError.message.toLowerCase().includes("already exists")
          ) {
            const msg = apiError.message.toLowerCase();
            if (msg.includes("code")) {
              setError("code", { message: apiError.message });
            } else if (msg.includes("name")) {
              setError("name", { message: apiError.message });
            } else {
              setGeneralError(apiError.message);
            }
            return;
          }

          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              setError(field as any, { message: msg });
            });
            return;
          }

          setGeneralError(apiError.message || "Failed to save category.");
          return;
        }
      }
      setGeneralError("An unexpected error occurred while saving the category.");
    }
  };

  const isPending =
    isSubmitting || createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <FolderPlus className="h-5 w-5 text-blue-600" />
            <DialogTitle className="text-lg font-semibold text-slate-900">
              {isEdit ? "Edit Test Category" : "Create Test Category"}
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            {isEdit
              ? "Update master diagnostic category details."
              : "Define a top-level clinical classification for diagnostic tests."}
          </DialogDescription>
        </DialogHeader>

        {generalError && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Error</AlertTitle>
            <AlertDescription className="text-xs">{generalError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <Label htmlFor="cat-code" className="text-xs font-semibold">
              Category Code <span className="text-red-500">*</span>
            </Label>
            <Input
              id="cat-code"
              placeholder="e.g. HEMATOLOGY"
              className="font-mono uppercase"
              {...register("code")}
            />
            {errors.code && (
              <p className="text-xs text-red-500">{errors.code.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="cat-name" className="text-xs font-semibold">
              Category Name <span className="text-red-500">*</span>
            </Label>
            <Input
              id="cat-name"
              placeholder="e.g. Hematology & Coagulation"
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-red-500">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="cat-desc" className="text-xs font-semibold">
              Description
            </Label>
            <Textarea
              id="cat-desc"
              placeholder="Clinical category overview..."
              rows={2}
              {...register("description")}
            />
            {errors.description && (
              <p className="text-xs text-red-500">{errors.description.message}</p>
            )}
          </div>

          {isEdit && (
            <div className="space-y-1.5">
              <Label htmlFor="cat-status" className="text-xs font-semibold">
                Status
              </Label>
              <Controller
                name="status"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="cat-status">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="INACTIVE">Inactive</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          )}

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
                  {isEdit ? "Save Changes" : "Create Category"}
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
