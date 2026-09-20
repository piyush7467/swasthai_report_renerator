import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  Edit,
  Loader2,
  ShieldAlert,
} from "lucide-react";

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
import type { UserResponse, UserRole } from "../types/userTypes";
import { useUpdateUserMutation } from "../hooks/useUsers";
import { useOrganizationsQuery } from "../hooks/useOrganizations";

const editUserSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "User name is required")
    .max(150, "User name must not exceed 150 characters"),
  email: z
    .string()
    .trim()
    .min(1, "Email is required")
    .email("Invalid email address")
    .max(150, "Email must not exceed 150 characters"),
  role: z.enum(["ORG_ADMIN", "LAB_STAFF"], {
    message: "Role is required",
  }),
  organizationRefId: z
    .string()
    .min(1, "Organization is required for this role"),
});

type EditUserFormValues = z.infer<typeof editUserSchema>;

interface EditUserDialogProps {
  user: UserResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditUserDialog({
  user,
  open,
  onOpenChange,
}: EditUserDialogProps) {
  const [generalError, setGeneralError] = useState<string | null>(null);

  const updateMutation = useUpdateUserMutation();

  const { data: orgsData, isLoading: orgsLoading } = useOrganizationsQuery({
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const isProtectedSuperAdmin = user?.role === "SUPER_ADMIN";

  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EditUserFormValues>({
    resolver: zodResolver(editUserSchema),
    values: user
      ? {
          name: user.name,
          email: user.email,
          role: user.role === "ORG_ADMIN" ? "ORG_ADMIN" : "LAB_STAFF",
          organizationRefId: user.organizationRefId ?? "",
        }
      : undefined,
    mode: "onBlur",
  });

  if (!user) return null;

  const onSubmit = async (values: EditUserFormValues) => {
    if (isProtectedSuperAdmin) return;
    setGeneralError(null);

    try {
      await updateMutation.mutateAsync({
        refId: user.refId,
        data: {
          name: values.name.trim(),
          email: values.email.trim().toLowerCase(),
          role: values.role as UserRole,
          organizationRefId: values.organizationRefId.trim(),
        },
      });

      onOpenChange(false);
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        const apiError = error.response?.data;
        if (apiError) {
          if (apiError.code === "RESOURCE_ALREADY_EXISTS") {
            const message = apiError.message.toLowerCase();
            if (message.includes("email")) {
              setError("email", { message: apiError.message });
            } else {
              setGeneralError(apiError.message);
            }
            return;
          }

          if (apiError.code === "VALIDATION_ERROR" && apiError.errors) {
            Object.entries(apiError.errors).forEach(([field, msg]) => {
              if (
                field === "name" ||
                field === "email" ||
                field === "role" ||
                field === "organizationRefId"
              ) {
                setError(field as keyof EditUserFormValues, { message: msg });
              }
            });
            return;
          }

          setGeneralError(apiError.message || "Failed to update user.");
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
      onOpenChange(nextOpen);
      if (!nextOpen) {
        setGeneralError(null);
      }
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
              <Edit className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                Edit User
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500 font-mono">
                {user.refId}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isProtectedSuperAdmin ? (
          <div className="space-y-4 py-2">
            <Alert className="border-purple-200 bg-purple-50 text-purple-900">
              <ShieldAlert className="h-4 w-4 text-purple-700" />
              <AlertTitle className="text-xs font-semibold">
                Protected Account
              </AlertTitle>
              <AlertDescription className="text-xs text-purple-800 leading-relaxed">
                The platform SUPER_ADMIN account cannot be modified through the standard user management interface to ensure system stability and authorization integrity.
              </AlertDescription>
            </Alert>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Close
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-2">
            {generalError && (
              <Alert
                variant="destructive"
                className="border-red-200 bg-red-50 py-2.5 text-red-800"
              >
                <AlertCircle className="h-4 w-4 text-red-600" />
                <AlertTitle className="text-xs font-semibold">Error</AlertTitle>
                <AlertDescription className="text-xs text-red-700">
                  {generalError}
                </AlertDescription>
              </Alert>
            )}

            {/* Name */}
            <div className="space-y-1.5">
              <Label htmlFor="edit-user-name" className="text-xs font-medium text-slate-700">
                Full Name <span className="text-red-500">*</span>
              </Label>
              <Input
                id="edit-user-name"
                maxLength={150}
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.name)}
                {...register("name")}
              />
              {errors.name && (
                <p className="text-xs text-red-600">{errors.name.message}</p>
              )}
            </div>

            {/* Email */}
            <div className="space-y-1.5">
              <Label htmlFor="edit-user-email" className="text-xs font-medium text-slate-700">
                Email Address <span className="text-red-500">*</span>
              </Label>
              <Input
                id="edit-user-email"
                type="email"
                maxLength={150}
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.email)}
                {...register("email")}
              />
              {errors.email && (
                <p className="text-xs text-red-600">{errors.email.message}</p>
              )}
            </div>

            {/* Role */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-slate-700">
                Role <span className="text-red-500">*</span>
              </Label>
              <Controller
                control={control}
                name="role"
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                    disabled={isSubmitting}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select role" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="LAB_STAFF">
                        Lab Staff (Clinical Operator)
                      </SelectItem>
                      <SelectItem value="ORG_ADMIN">
                        Organization Admin (Facility Manager)
                      </SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.role ? (
                <p className="text-xs text-red-600">{errors.role.message}</p>
              ) : (
                <p className="text-[11px] text-slate-500">
                  Super Admin role cannot be reassigned through user updates.
                </p>
              )}
            </div>

            {/* Organization */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-slate-700">
                Assigned Organization <span className="text-red-500">*</span>
              </Label>
              <Controller
                control={control}
                name="organizationRefId"
                render={({ field }) => (
                  <Select
                    value={field.value || ""}
                    onValueChange={field.onChange}
                    disabled={isSubmitting || orgsLoading}
                  >
                    <SelectTrigger aria-invalid={Boolean(errors.organizationRefId)}>
                      <SelectValue
                        placeholder={
                          orgsLoading
                            ? "Loading organizations..."
                            : "Select organization"
                        }
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {orgsData?.content && orgsData.content.length > 0 ? (
                        orgsData.content.map((org) => (
                          <SelectItem
                            key={org.refId}
                            value={org.refId}
                            disabled={org.status !== "ACTIVE"}
                          >
                            <span className="font-medium text-slate-900">
                              {org.name}
                            </span>{" "}
                            <span className="text-xs text-slate-500">
                              ({org.code})
                            </span>
                            {org.status !== "ACTIVE" && (
                              <span className="ml-2 text-[10px] text-amber-600 font-medium">
                                [{org.status}]
                              </span>
                            )}
                          </SelectItem>
                        ))
                      ) : (
                        <SelectItem value="none" disabled>
                          No organizations found
                        </SelectItem>
                      )}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.organizationRefId && (
                <p className="text-xs text-red-600">
                  {errors.organizationRefId.message}
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
                    Saving...
                  </>
                ) : (
                  "Save Changes"
                )}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}
