import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  Activity,
  AlertCircle,
  Eye,
  EyeOff,
  Loader2,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "../AuthContext";
import type { ApiErrorResponse, UserRole } from "../authTypes";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

const loginSchema = z.object({
  email: z
    .string()
    .min(1, "Email is required")
    .max(150, "Email must not exceed 150 characters")
    .email("Please enter a valid email address"),
  password: z
    .string()
    .min(1, "Password is required")
    .max(100, "Password must not exceed 100 characters"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

interface LocationState {
  from?: {
    pathname?: string;
  };
}

function getDefaultRouteForRole(role: UserRole): string {
  switch (role) {
    case "SUPER_ADMIN":
      return "/super-admin";
    case "ORG_ADMIN":
      return "/org-admin";
    case "LAB_STAFF":
      return "/lab-staff";
    default:
      return "/unauthorized";
  }
}

function getSafeReturnUrl(requestedPath: unknown, role: UserRole): string {
  if (typeof requestedPath !== "string") {
    return getDefaultRouteForRole(role);
  }

  const trimmed = requestedPath.trim();

  // Reject absolute URLs, scheme prefixes, protocol-relative URLs, and non-internal paths
  if (
    !trimmed.startsWith("/") ||
    trimmed.startsWith("//") ||
    trimmed.includes("://") ||
    trimmed.toLowerCase().startsWith("javascript:") ||
    trimmed.toLowerCase().startsWith("data:")
  ) {
    return getDefaultRouteForRole(role);
  }

  // Only permit redirect if the requested path belongs to the user's role area
  if (role === "SUPER_ADMIN" && trimmed.startsWith("/super-admin")) {
    return trimmed;
  }
  if (role === "ORG_ADMIN" && trimmed.startsWith("/org-admin")) {
    return trimmed;
  }
  if (role === "LAB_STAFF" && trimmed.startsWith("/lab-staff")) {
    return trimmed;
  }

  return getDefaultRouteForRole(role);
}

export function LoginPage() {
  const { login, isAuthenticated, isInitializing, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [showPassword, setShowPassword] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
    mode: "onBlur",
  });

  // Navigate according to backend-provided authoritative role once authenticated
  useEffect(() => {
    if (!isInitializing && isAuthenticated && user) {
      const state = location.state as LocationState | null;
      const safePath = getSafeReturnUrl(state?.from?.pathname, user.role);
      navigate(safePath, { replace: true });
    }
  }, [isInitializing, isAuthenticated, user, location.state, navigate]);

  const onSubmit = async (values: LoginFormValues) => {
    setGeneralError(null);

    try {
      await login({
        email: values.email.trim(),
        password: values.password,
      });
      // AuthContext will update user state and trigger useEffect navigation above
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        if (error.code === "ECONNABORTED" || error.message?.includes("timeout")) {
          setGeneralError("Request timed out. Please try again.");
          return;
        }

        const response = error.response;
        if (!response) {
          setGeneralError(
            "Unable to connect to SwasthAI. Please check your connection and try again.",
          );
          return;
        }

        const status = response.status;
        const apiError = response.data;

        if (status === 401) {
          setGeneralError("Invalid email or password.");
          return;
        }

        if (status === 429 || apiError?.code === "RATE_LIMIT_EXCEEDED") {
          setGeneralError(
            apiError?.message ||
              "Too many login attempts. Please wait and try again.",
          );
          return;
        }

        if (apiError?.code === "VALIDATION_ERROR" && apiError.errors) {
          Object.entries(apiError.errors).forEach(([field, msg]) => {
            if (field === "email" || field === "password") {
              setError(field as keyof LoginFormValues, { message: msg });
            }
          });
          setGeneralError(
            apiError.message || "Please correct the highlighted errors.",
          );
          return;
        }

        if (
          status === 400 ||
          apiError?.code === "BAD_REQUEST" ||
          apiError?.code === "ILLEGAL_STATE"
        ) {
          setGeneralError(
            apiError?.message ||
              "Unable to sign in. Please verify your account status with an administrator.",
          );
          return;
        }

        if (status >= 500) {
          setGeneralError(
            "Something went wrong on the server. Please try again later.",
          );
          return;
        }

        setGeneralError(
          apiError?.message ||
            "An unexpected error occurred. Please try again.",
        );
        return;
      }

      setGeneralError(
        "Unable to connect to SwasthAI. Please check your connection and try again.",
      );
    }
  };

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center text-center">
          <Loader2 className="size-8 animate-spin text-slate-700" />
          <p className="mt-3 text-sm text-slate-600">Checking your session...</p>
        </div>
      </div>
    );
  }

  if (isAuthenticated && user) {
    return null;
  }

  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-6">
        {/* Brand Header */}
        <div className="flex flex-col items-center text-center">
          <div className="flex size-12 items-center justify-center rounded-xl bg-slate-900 text-white shadow-sm">
            <Activity className="size-6 text-emerald-400" />
          </div>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            SwasthAI
          </h1>
          <p className="text-sm font-medium text-slate-500">
            Report Generator & Laboratory Management
          </p>
        </div>

        {/* Authentication Card */}
        <Card className="border-slate-200 bg-white shadow-sm">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-xl font-semibold text-slate-900">
              Sign in to your account
            </CardTitle>
            <CardDescription className="text-sm text-slate-600">
              Enter your laboratory credentials to access your workspace
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4 pt-2">
            {generalError && (
              <Alert variant="destructive" className="border-red-200 bg-red-50 text-red-800">
                <AlertCircle className="size-4 text-red-600" />
                <AlertTitle className="text-sm font-semibold">Authentication Notice</AlertTitle>
                <AlertDescription className="text-xs text-red-700">
                  {generalError}
                </AlertDescription>
              </Alert>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
              {/* Email Field */}
              <div className="space-y-2">
                <Label htmlFor="email" className="text-sm font-medium text-slate-700">
                  Email address
                </Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="username"
                  placeholder="name@laboratory.com"
                  disabled={isSubmitting}
                  aria-invalid={Boolean(errors.email)}
                  aria-describedby={errors.email ? "email-error" : undefined}
                  className={errors.email ? "border-red-300 focus-visible:ring-red-200" : ""}
                  {...register("email")}
                />
                {errors.email && (
                  <p id="email-error" className="text-xs text-red-600">
                    {errors.email.message}
                  </p>
                )}
              </div>

              {/* Password Field */}
              <div className="space-y-2">
                <Label htmlFor="password" className="text-sm font-medium text-slate-700">
                  Password
                </Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    disabled={isSubmitting}
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? "password-error" : undefined}
                    className={`pr-10 ${
                      errors.password ? "border-red-300 focus-visible:ring-red-200" : ""
                    }`}
                    {...register("password")}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none focus:text-slate-600 disabled:cursor-not-allowed"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? (
                      <EyeOff className="size-4" />
                    ) : (
                      <Eye className="size-4" />
                    )}
                  </button>
                </div>
                {errors.password && (
                  <p id="password-error" className="text-xs text-red-600">
                    {errors.password.message}
                  </p>
                )}
              </div>

              {/* Submit Button */}
              <Button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-slate-900 text-white hover:bg-slate-800 focus-visible:ring-slate-400 py-2.5 text-sm font-medium"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="size-4 animate-spin mr-2" />
                    Signing in...
                  </>
                ) : (
                  "Sign in"
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Security & Regulatory Notice Footer */}
        <div className="flex items-center justify-center gap-2 text-center text-xs text-slate-500">
          <ShieldCheck className="size-4 text-slate-400 shrink-0" />
          <span>Tenant-isolated diagnostic data • HIPAA & ISO 15189 aligned</span>
        </div>
      </div>
    </main>
  );
}