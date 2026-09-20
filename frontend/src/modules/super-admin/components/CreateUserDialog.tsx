import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  Eye,
  EyeOff,
  Loader2,
  UserPlus,
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
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ApiErrorResponse } from "@/core/auth/authTypes";
import type { UserRole } from "../types/userTypes";
import { useCreateUserMutation } from "../hooks/useUsers";
import { useOrganizationsQuery } from "../hooks/useOrganizations";

const createUserSchema = z.object({
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
  password: z
    .string()
    .min(8, "Password must be at least 8 characters")
    .max(100, "Password must not exceed 100 characters"),
  role: z.enum(["ORG_ADMIN", "LAB_STAFF"], {
    message: "Role is required",
  }),
  organizationRefId: z
    .string()
    .min(1, "Organization is required"),
});

type CreateUserFormValues = z.infer<typeof createUserSchema>;

interface CreateUserDialogProps {
  trigger?: React.ReactNode;
  defaultOrganizationRefId?: string;
}

export function CreateUserDialog({
  trigger,
  defaultOrganizationRefId,
}: CreateUserDialogProps) {
  const [open, setOpen] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const createMutation = useCreateUserMutation();

  // Load real organizations from backend
  const { data: orgsData, isLoading: orgsLoading } = useOrganizationsQuery({
    size: 100,
    sortBy: "name",
    sortDirection: "ASC",
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      name: "",
      email: "",
      password: "",
      role: "LAB_STAFF",
      organizationRefId: defaultOrganizationRefId ?? "",
    },
    mode: "onBlur",
  });

  const onSubmit = async (values: CreateUserFormValues) => {
    setGeneralError(null);
    try {
      await createMutation.mutateAsync({
        name: values.name.trim(),
        email: values.email.trim().toLowerCase(),
        password: values.password,
        role: values.role as UserRole,
        organizationRefId: values.organizationRefId.trim(),
      });

      // Clear password and form state immediately
      reset();
      setShowPassword(false);
      setOpen(false);
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
                field === "password" ||
                field === "role" ||
                field === "organizationRefId"
              ) {
                setError(field, { message: msg });
              }
            });
            return;
          }

          setGeneralError(apiError.message || "Failed to create user.");
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
        reset({
          name: "",
          email: "",
          password: "",
          role: "LAB_STAFF",
          organizationRefId: defaultOrganizationRefId ?? "",
        });
        setShowPassword(false);
        setGeneralError(null);
      }
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button className="bg-slate-900 text-white hover:bg-slate-800">
            <UserPlus className="mr-2 h-4 w-4" />
            Create User
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-100 text-blue-700">
              <UserPlus className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                Create User
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Register a new system user with role and organization assignment.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

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

          {/* Full Name */}
          <div className="space-y-1.5">
            <Label htmlFor="create-user-name" className="text-xs font-medium text-slate-700">
              Full Name <span className="text-red-500">*</span>
            </Label>
            <Input
              id="create-user-name"
              placeholder="e.g., Dr. Ananya Sharma"
              maxLength={150}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.name)}
              {...register("name")}
            />
            {errors.name && (
              <p className="text-xs text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Email Address */}
          <div className="space-y-1.5">
            <Label htmlFor="create-user-email" className="text-xs font-medium text-slate-700">
              Email Address <span className="text-red-500">*</span>
            </Label>
            <Input
              id="create-user-email"
              type="email"
              placeholder="ananya@diagnosticlab.com"
              maxLength={150}
              disabled={isSubmitting}
              aria-invalid={Boolean(errors.email)}
              {...register("email")}
            />
            {errors.email && (
              <p className="text-xs text-red-600">{errors.email.message}</p>
            )}
          </div>

          {/* Password */}
          <div className="space-y-1.5">
            <Label
              htmlFor="create-user-password"
              className="text-xs font-medium text-slate-700"
            >
              Password <span className="text-red-500">*</span>
            </Label>
            <div className="relative">
              <Input
                id="create-user-password"
                type={showPassword ? "text" : "password"}
                placeholder="Minimum 8 characters"
                maxLength={100}
                disabled={isSubmitting}
                className="pr-10"
                aria-invalid={Boolean(errors.password)}
                {...register("password")}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none"
                tabIndex={-1}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            {errors.password ? (
              <p className="text-xs text-red-600">{errors.password.message}</p>
            ) : (
              <p className="text-[11px] text-slate-500">
                Must be between 8 and 100 characters. Passwords are never stored in plaintext.
              </p>
            )}
          </div>

          {/* Role Selection */}
          <div className="space-y-1.5">
            <Label className="text-xs font-medium text-slate-700">
              User Role <span className="text-red-500">*</span>
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
                    <SelectValue placeholder="Select user role" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LAB_STAFF">
                      Lab Staff (Standard Clinical Operator)
                    </SelectItem>
                    <SelectItem value="ORG_ADMIN">
                      Organization Admin (Facility Manager)
                    </SelectItem>
                  </SelectContent>
                </Select>
              )}
            />
            {errors.role && (
              <p className="text-xs text-red-600">{errors.role.message}</p>
            )}
          </div>

          {/* Organization Selection */}
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
                  disabled={isSubmitting || orgsLoading || Boolean(defaultOrganizationRefId)}
                >
                  <SelectTrigger aria-invalid={Boolean(errors.organizationRefId)}>
                    <SelectValue
                      placeholder={
                        orgsLoading
                          ? "Loading organizations..."
                          : "Select an active organization"
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
            {errors.organizationRefId ? (
              <p className="text-xs text-red-600">
                {errors.organizationRefId.message}
              </p>
            ) : (
              <p className="text-[11px] text-slate-500">
                {defaultOrganizationRefId
                  ? "Assigned automatically to this organization."
                  : "Only active organizations can be assigned new operators."}
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
                "Create User"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
