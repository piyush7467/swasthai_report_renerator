import { useState } from "react";
import {
  Shield,
  Server,
  Clock,
  HardDrive,
  Key,
  User,
  CheckCircle2,
  Lock,
  Database,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useAuth } from "@/core/auth/AuthContext";

export default function SettingsPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<"platform" | "security" | "retention">("platform");

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          System Settings & Platform Diagnostics
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Global architecture parameters, cryptographic security configuration, and data retention policies.
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2">
        <button
          type="button"
          onClick={() => setActiveTab("platform")}
          className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
            activeTab === "platform"
              ? "bg-slate-900 text-white"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <Server className="h-3.5 w-3.5" />
          Platform Overview
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("security")}
          className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
            activeTab === "security"
              ? "bg-slate-900 text-white"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <Shield className="h-3.5 w-3.5" />
          Security & Cryptography
        </button>

        <button
          type="button"
          onClick={() => setActiveTab("retention")}
          className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
            activeTab === "retention"
              ? "bg-slate-900 text-white"
              : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          }`}
        >
          <HardDrive className="h-3.5 w-3.5" />
          Data Retention & Storage
        </button>
      </div>

      {/* Tab 1: Platform Overview */}
      {activeTab === "platform" && (
        <div className="space-y-6">
          {/* Admin Identity Card */}
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <User className="h-4 w-4 text-blue-600" />
                  <CardTitle className="text-base font-semibold text-slate-900">
                    Super Admin Profile
                  </CardTitle>
                </div>
                <Badge className="bg-slate-900 text-white">{user?.role}</Badge>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Current authoritative administrator session parameters.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Full Name</p>
                  <p className="font-semibold text-slate-900 mt-0.5">{user?.name}</p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Email Address</p>
                  <p className="font-semibold text-slate-900 mt-0.5 truncate">{user?.email}</p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Authorization Scope</p>
                  <p className="font-semibold text-slate-900 mt-0.5">
                    {user?.organizationRefId || "Global / Multi-Tenant"}
                  </p>
                </div>

                <div className="rounded-lg border border-slate-100 bg-slate-50/50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Account Status</p>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                    <span className="font-semibold text-emerald-700 text-xs">Active & Verified</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Infrastructure Grid */}
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <Card className="border-slate-200 bg-white">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Database className="h-4 w-4 text-purple-600" />
                  <CardTitle className="text-sm font-semibold text-slate-900">
                    Database & Persistence Engine
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Primary relational database connection & schema migration status.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-xs">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Database Engine</span>
                  <span className="font-mono font-semibold text-slate-900">PostgreSQL (v18.2 Dialect)</span>
                </div>
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Connection Pool</span>
                  <span className="font-mono font-semibold text-slate-900">HikariCP (High Performance)</span>
                </div>
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Schema Management</span>
                  <span className="font-mono font-semibold text-slate-900">Flyway (V1 - V17 Validated)</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">DDL Mode</span>
                  <Badge variant="outline" className="text-[10px] text-emerald-700 bg-emerald-50 border-emerald-200">
                    Validate Only (Strict Production)
                  </Badge>
                </div>
              </CardContent>
            </Card>

            <Card className="border-slate-200 bg-white">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2">
                  <Server className="h-4 w-4 text-blue-600" />
                  <CardTitle className="text-sm font-semibold text-slate-900">
                    Application Backend Services
                  </CardTitle>
                </div>
                <CardDescription className="text-xs text-slate-500">
                  Runtime environment and framework specifications.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-xs">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Framework</span>
                  <span className="font-mono font-semibold text-slate-900">Spring Boot 4.1.1 (Java 21/25)</span>
                </div>
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">API Standard</span>
                  <span className="font-mono font-semibold text-slate-900">REST (Standardized ApiResponse)</span>
                </div>
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <span className="text-slate-500">Timezone Base</span>
                  <span className="font-mono font-semibold text-slate-900">Asia/Kolkata (IST, UTC+05:30)</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Health Endpoint</span>
                  <span className="font-mono text-blue-600 font-medium">/actuator/health (Live)</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* Tab 2: Security & Cryptography */}
      {activeTab === "security" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Key className="h-4 w-4 text-emerald-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Asymmetric Cryptographic Authentication
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                JWT tokens are signed via RS256 private key and validated using public keys.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Signature Algorithm</p>
                  <p className="font-mono font-bold text-slate-800 mt-1">RS256 (RSA 2048-bit)</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Access Token Validity</p>
                  <p className="font-mono font-bold text-slate-800 mt-1">15 Minutes (900 seconds)</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Refresh Token Rotation</p>
                  <p className="font-mono font-bold text-slate-800 mt-1">Enabled (Single-use)</p>
                </div>
              </div>

              <Separator />

              <div className="space-y-2">
                <h4 className="font-semibold text-slate-900 text-xs">Distributed Rate Limiting Policy</h4>
                <p className="text-slate-500 leading-relaxed text-[11px]">
                  Protects authentication endpoints from brute-force intrusions using sliding epoch-second windows.
                </p>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 mt-2">
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Max Attempts</span>
                    <p className="font-bold text-slate-800 mt-0.5">5 failed attempts</p>
                  </div>
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Evaluation Window</span>
                    <p className="font-bold text-slate-800 mt-0.5">60 seconds</p>
                  </div>
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Lockout Duration</span>
                    <p className="font-bold text-rose-700 mt-0.5">300 seconds (5 mins)</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Lock className="h-4 w-4 text-indigo-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Break-Glass Emergency Access Protocol
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Audited emergency pathway allowing Super Admins to inspect tenant clinical reports.
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs space-y-3 text-slate-600">
              <p className="leading-relaxed">
                By default, Super Admins have zero clinical report viewing permissions to maintain strict HIPAA/tenant privacy.
                When a life-critical or legal incident occurs, the Break-Glass protocol permits emergency access under three mandatory constraints:
              </p>
              <ul className="list-disc pl-5 space-y-1 text-slate-700">
                <li><strong>Target Organization Verification:</strong> The Super Admin must explicitly specify the tenant reference ID.</li>
                <li><strong>Mandatory Clinical Justification:</strong> A permanent description (min. 10 chars) must be recorded before access is granted.</li>
                <li><strong>Immutable Security Log:</strong> Every access attempt writes an append-only entry in <code className="text-indigo-700 font-mono">security_audit_logs</code> recording actor email, timestamp, and client IP address.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 3: Data Retention & Storage */}
      {activeTab === "retention" && (
        <div className="space-y-6">
          <Card className="border-slate-200 bg-white">
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-amber-600" />
                <CardTitle className="text-base font-semibold text-slate-900">
                  Automated Clinical Report Retention Policy
                </CardTitle>
              </div>
              <CardDescription className="text-xs text-slate-500">
                Lifecycle progression from clinical finalization to soft-delete and automated permanent purge.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 text-xs">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Soft-Delete Threshold</p>
                  <p className="font-bold text-slate-800 mt-1">5 Days</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Eligible for batch deletion</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Permanent Purge Cutoff</p>
                  <p className="font-bold text-rose-700 mt-1">10 Days</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Automated hard deletion</p>
                </div>
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-[10px] font-medium uppercase text-slate-400">Purge Scheduler Cron</p>
                  <p className="font-mono font-bold text-slate-800 mt-1">0 0 * * * *</p>
                  <p className="text-[10px] text-slate-500 mt-0.5">Hourly execution</p>
                </div>
              </div>

              <Separator />

              <div className="space-y-2">
                <h4 className="font-semibold text-slate-900 text-xs">Digital Asset Storage Specifications</h4>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 mt-2">
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Storage Architecture</span>
                    <p className="font-medium text-slate-800 mt-0.5">Partitioned Local Disk Storage Service</p>
                  </div>
                  <div className="rounded-lg border border-slate-100 p-2.5">
                    <span className="text-[10px] text-slate-400 uppercase">Max Image/Signature Upload</span>
                    <p className="font-medium text-slate-800 mt-0.5">5 MB (5,242,880 bytes)</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
