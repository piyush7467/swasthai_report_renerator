import {
  Building2,
  FileText,
  FlaskConical,
  Mail,
  Shield,
  User,
  Users,
} from "lucide-react";

import { Link } from "react-router-dom";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/core/auth/AuthContext";

export function SuperAdminDashboard() {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
          Super Admin Dashboard
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Global platform overview, tenant organizations, and system management.
        </p>
      </div>

      {/* Authenticated Identity Card */}
      <Card className="border-slate-200 bg-white">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-semibold text-slate-900">
              Authenticated Session Identity
            </CardTitle>
            <Badge className="bg-slate-900 text-white">
              {user.role}
            </Badge>
          </div>
          <CardDescription className="text-xs text-slate-500">
            Authoritative session parameters issued by SwasthAI authentication service.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <User className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Full Name</p>
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
                <p className="text-xs font-medium text-slate-500">System Role</p>
                <p className="mt-0.5 truncate text-sm font-semibold text-slate-900">
                  {user.role}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-3">
              <Building2 className="mt-0.5 h-4 w-4 text-slate-500 shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-slate-500">Tenant Scope</p>
                <p className="mt-0.5 truncate text-sm font-semibold text-slate-700">
                  {user.organizationRefId ? user.organizationRefId : "Global / All Tenants"}
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Admin Modules Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="border-slate-200 bg-white flex flex-col justify-between">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Organizations
              </CardTitle>
              <Building2 className="h-4 w-4 text-slate-400" />
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-slate-600">
              Manage clinical laboratory tenants, operational status, and licensing.
            </p>
            <Button variant="outline" size="sm" asChild className="w-full text-xs">
              <Link to="/super-admin/organizations">Manage Organizations &rarr;</Link>
            </Button>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Global Users
              </CardTitle>
              <Users className="h-4 w-4 text-slate-400" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              Manage user accounts, roles, and status across organizations.
            </p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Test Catalog
              </CardTitle>
              <FlaskConical className="h-4 w-4 text-slate-400" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              Standard laboratory tests, parameters, formulas, and units.
            </p>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-white">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium text-slate-600">
                Audit & Security
              </CardTitle>
              <FileText className="h-4 w-4 text-slate-400" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600">
              System-wide audit trail, break-glass review, and immutability logs.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
