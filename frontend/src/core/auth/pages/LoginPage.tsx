import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import {
  AlertCircle,
  Eye,
  EyeOff,
  Info,
  Loader2,
  Lock,
  Mail,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "../AuthContext";
import type { ApiErrorResponse, UserRole } from "../authTypes";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import loginBg from "@/assets/reportloginpage.png";

/**
 * Modern healthcare emblem SVG representing SwasthAI
 * Features caring hands, health figure, and vibrant cyan-to-emerald gradient
 */
function SwasthAiBrandIcon({ className = "size-10" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="swasthCyanTeal" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#06b6d4" />
          <stop offset="100%" stopColor="#0d9488" />
        </linearGradient>
        <linearGradient id="swasthTealEmerald" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#0d9488" />
          <stop offset="100%" stopColor="#10b981" />
        </linearGradient>
      </defs>

      {/* Head / Core Sphere */}
      <circle cx="32" cy="14" r="5.5" fill="url(#swasthCyanTeal)" />

      {/* Upper Caring Wings / Torso */}
      <path
        d="M19 25C23 20 28 22 32 24C36 22 41 20 45 25C42 29.5 37 32 32 31C27 32 22 29.5 19 25Z"
        fill="url(#swasthCyanTeal)"
      />

      {/* Lower Health Leaves / Cupped Hands */}
      <path
        d="M14 30C14 41 23 48 32 48C41 48 50 41 50 30C45 37 38 40.5 32 40C26 40.5 19 37 14 30Z"
        fill="url(#swasthTealEmerald)"
      />

      {/* Central Stem Pillar */}
      <path
        d="M29.5 28H34.5V44C34.5 44.8 33.8 45.5 33 45.5H31C30.2 45.5 29.5 44.8 29.5 44V28Z"
        fill="url(#swasthTealEmerald)"
      />
    </svg>
  );
}

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
  const [isForgotModalOpen, setIsForgotModalOpen] = useState(false);

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
          <Loader2 className="size-8 animate-spin text-teal-600" />
          <p className="mt-3 text-sm text-slate-600">Checking your session...</p>
        </div>
      </div>
    );
  }

  if (isAuthenticated && user) {
    return null;
  }

  return (
    <main className="relative min-h-screen w-full flex items-center justify-center lg:justify-start overflow-x-hidden bg-[#eaf4f7]">
      {/* Background laboratory illustration */}
      <div
        className="absolute inset-0 bg-cover bg-no-repeat bg-right"
        style={{ backgroundImage: `url(${loginBg})` }}
        aria-hidden="true"
      />

      {/* Subtle overlay on smaller viewports to ensure contrast */}
      <div
        className="absolute inset-0 bg-white/40 lg:hidden backdrop-blur-xs"
        aria-hidden="true"
      />

      {/* Form container - positioned on the left side to fit inside the curved blank space */}
      <div className="relative z-10 w-full px-4 sm:px-6 py-8 sm:py-12 lg:py-0 lg:pl-14 xl:pl-24 2xl:pl-32 flex justify-center lg:justify-start">
        <div className="w-full max-w-[430px] sm:max-w-[450px]">
          <div className="rounded-3xl border border-white/80 bg-white/95 p-6 sm:p-9 shadow-2xl shadow-teal-950/10 backdrop-blur-md transition-all">
            {/* Branding Header */}
            <div className="flex flex-col items-center text-center">
              <div className="flex size-14 items-center justify-center rounded-2xl bg-teal-50/90 border border-teal-100 shadow-xs transition-transform hover:scale-105 duration-200">
                <SwasthAiBrandIcon className="size-10" />
              </div>
              <div className="mt-3 flex items-center justify-center gap-1">
                <span className="text-2xl font-bold tracking-tight text-slate-900">
                  Swasth
                </span>
                <span className="text-2xl font-bold tracking-tight text-teal-600">
                  AI
                </span>
              </div>
              <span className="mt-0.5 text-xs font-semibold tracking-wider text-slate-500 uppercase">
                Report Generator
              </span>
            </div>

            {/* Welcome Back Heading */}
            <div className="mt-6 text-center">
              <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-[26px]">
                Welcome Back
              </h1>
              <p className="mt-1 text-sm text-slate-500">
                Sign in to access your laboratory dashboard
              </p>
            </div>

            {/* General Error Banner */}
            {generalError && (
              <Alert
                variant="destructive"
                className="mt-5 border-red-200 bg-red-50 text-red-800 rounded-xl"
              >
                <AlertCircle className="size-4 text-red-600 shrink-0" />
                <AlertTitle className="text-xs font-semibold">Authentication Notice</AlertTitle>
                <AlertDescription className="text-xs text-red-700">
                  {generalError}
                </AlertDescription>
              </Alert>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 space-y-4">
              {/* Email Field */}
              <div className="space-y-1.5">
                <Label htmlFor="email" className="text-xs font-semibold text-slate-700">
                  Email Address
                </Label>
                <div className="relative">
                  <Mail className="size-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <Input
                    id="email"
                    type="email"
                    autoComplete="username"
                    placeholder="Enter your email"
                    disabled={isSubmitting}
                    aria-invalid={Boolean(errors.email)}
                    aria-describedby={errors.email ? "email-error" : undefined}
                    className={`pl-10 h-11 bg-white border-slate-200 text-slate-900 placeholder:text-slate-400 rounded-xl focus-visible:ring-2 focus-visible:ring-teal-500/20 focus-visible:border-teal-600 transition-all ${
                      errors.email ? "border-red-300 focus-visible:ring-red-200" : ""
                    }`}
                    {...register("email")}
                  />
                </div>
                {errors.email && (
                  <p id="email-error" className="text-xs font-medium text-red-600">
                    {errors.email.message}
                  </p>
                )}
              </div>

              {/* Password Field */}
              <div className="space-y-1.5">
                <Label htmlFor="password" className="text-xs font-semibold text-slate-700">
                  Password
                </Label>
                <div className="relative">
                  <Lock className="size-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    disabled={isSubmitting}
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? "password-error" : undefined}
                    className={`pl-10 pr-10 h-11 bg-white border-slate-200 text-slate-900 placeholder:text-slate-400 rounded-xl focus-visible:ring-2 focus-visible:ring-teal-500/20 focus-visible:border-teal-600 transition-all ${
                      errors.password ? "border-red-300 focus-visible:ring-red-200" : ""
                    }`}
                    {...register("password")}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1 rounded-md transition-colors disabled:cursor-not-allowed"
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
                  <p id="password-error" className="text-xs font-medium text-red-600">
                    {errors.password.message}
                  </p>
                )}
              </div>

              {/* Forgot Password link */}
              <div className="flex justify-end pt-0.5">
                <button
                  type="button"
                  onClick={() => setIsForgotModalOpen(true)}
                  className="text-xs font-semibold text-teal-600 hover:text-teal-700 hover:underline transition-colors cursor-pointer"
                >
                  Forgot Password?
                </button>
              </div>

              {/* Submit Button */}
              <Button
                type="submit"
                disabled={isSubmitting}
                className="w-full h-11 bg-teal-600 hover:bg-teal-700 active:bg-teal-800 text-white font-semibold text-sm rounded-xl shadow-sm hover:shadow transition-all duration-200 cursor-pointer disabled:opacity-70"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="size-4 animate-spin mr-2" />
                    Signing in...
                  </>
                ) : (
                  "Sign In"
                )}
              </Button>
            </form>

            {/* Regulatory & Security Compliance Footer */}
            <div className="mt-6 flex items-center justify-center gap-1.5 text-center text-xs text-slate-400">
              <ShieldCheck className="size-3.5 text-teal-600/70 shrink-0" />
              <span>Tenant-isolated diagnostic data • HIPAA & ISO 15189 aligned</span>
            </div>
          </div>
        </div>
      </div>

      {/* Forgot Password Modal */}
      <Dialog open={isForgotModalOpen} onOpenChange={setIsForgotModalOpen}>
        <DialogContent className="sm:max-w-md rounded-2xl bg-white p-6">
          <DialogHeader>
            <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-teal-50 text-teal-600 mb-2">
              <Info className="size-6" />
            </div>
            <DialogTitle className="text-center text-lg font-bold text-slate-900">
              Password Reset Assistance
            </DialogTitle>
            <DialogDescription className="text-center text-sm text-slate-600 pt-1">
              To safeguard patient health data and organizational security, passwords must be reset through an authorized administrator.
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-xl bg-slate-50 p-4 text-xs text-slate-600 space-y-2 border border-slate-200 my-2">
            <p>
              • <strong className="text-slate-800">Lab Staff Members:</strong> Please contact your Organization Administrator to issue a password reset.
            </p>
            <p>
              • <strong className="text-slate-800">Organization Admins:</strong> Please reach out to your platform Super Administrator or contact <span className="font-mono text-teal-700">admin@swasthai.com</span>.
            </p>
          </div>
          <div className="flex justify-end pt-2">
            <Button
              type="button"
              onClick={() => setIsForgotModalOpen(false)}
              className="w-full bg-teal-600 hover:bg-teal-700 text-white rounded-xl h-10 font-medium"
            >
              Understood
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </main>
  );
}