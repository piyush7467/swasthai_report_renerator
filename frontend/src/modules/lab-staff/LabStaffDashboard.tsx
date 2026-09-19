import {
  Building2,
  FileCheck2,
  FilePlus,
  Mail,
  Shield,
  User,
  UserPlus,
} from "lucide-react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/core/auth/AuthContext";

export function LabStaffDashboard() {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          Laboratory Workspace
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Patient testing, diagnostic parameter entry, and clinical report preparation.
        </p>
      </div>

      {/* Authenticated Identity Card */}
      <Card className="border-slate-200 bg-white">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-semibold text-slate-900">
              Authenticated Session Identity
            </CardTitle>
            <Badge className="bg-emerald-700 text-white">
              {user.role.replace("_", " ")}
            </Badge>
          </div>
          <CardDescription className="text-xs text-slate-500">
            Assigned laboratory credentials issued by SwasthAI authentication service.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <User className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Staff Member</p>
                <p className="mt-0.5 truncate text-sm font-semibold text-slate-900">
                  {user.name}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <Mail className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Email Address</p>
                <p className="mt-0.5 truncate text-sm font-semibold text-slate-900">
                  {user.email}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <Shield className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Staff Role</p>
                <p className="mt-0.5 truncate text-sm font-semibold text-slate-900">
                  {user.role}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <Building2 className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Laboratory Tenant</p>
                <p
                  className="mt-0.5 truncate font-mono text-xs font-bold text-slate-900"
                  title={user.organizationRefId ?? "None"}
                >
                  {user.organizationRefId ?? "N/A"}
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Laboratory Quick Actions Overview */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Patient Registration
              </CardTitle>
              <UserPlus className="h-4 w-4 text-emerald-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              Register new patients, search existing records, and update demographics.
            </p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Create Draft Report
              </CardTitle>
              <FilePlus className="h-4 w-4 text-emerald-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              Select patient, add ordered tests, and prepare draft report structure.
            </p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Report Finalization
              </CardTitle>
              <FileCheck2 className="h-4 w-4 text-emerald-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              Review completed test parameters, finalize results, and generate tamper-evident PDFs.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
