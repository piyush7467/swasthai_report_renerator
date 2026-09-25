import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Building2,
  CreditCard,
  Edit,
  FileText,
  Info,
  MoreVertical,
  Plus,
  RefreshCw,
  Search,
  Sparkles,
  ToggleLeft,
  ToggleRight,
  Users,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { usePlansQuery, useUpdatePlanMutation } from "../hooks/useLicensing";
import { PlanStatusBadge } from "../components/PlanStatusBadge";
import { ConfirmPlanStatusDialog } from "../components/ConfirmPlanStatusDialog";
import type { PlanResponse } from "../types/licensingTypes";

export function PlansPage() {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [planToToggle, setPlanToToggle] = useState<PlanResponse | null>(null);
  const { data: plans = [], isLoading, isError, error, refetch, isFetching } = usePlansQuery();
  const updateMutation = useUpdatePlanMutation();

  const filteredPlans = plans.filter((plan) => {
    const term = searchTerm.toLowerCase().trim();
    if (!term) return true;
    return (
      plan.name.toLowerCase().includes(term) ||
      plan.code.toLowerCase().includes(term) ||
      (plan.description && plan.description.toLowerCase().includes(term))
    );
  });

  const handleToggleStatus = async (plan: PlanResponse) => {
    try {
      await updateMutation.mutateAsync({
        planRefId: plan.refId,
        request: {
          name: plan.name,
          description: plan.description ?? undefined,
          annualPrice: plan.annualPrice,
          currency: plan.currency,
          active: !plan.active,
        },
      });
    } catch {
      // Errors handled via mutation error state
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Subscription Plans (Master Catalog)
            </h1>
            <Badge variant="outline" className="font-mono text-xs">
              {plans.length} {plans.length === 1 ? "Plan" : "Plans"}
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Configure global master subscription tiers (e.g. Basic, Standard, Enterprise) available for tenant organizations.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching || isLoading}
            className="text-slate-600 hover:text-slate-900"
          >
            <RefreshCw className={`mr-1.5 h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`} />
            Refresh
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5"
          >
            <Link to="/super-admin/licensing/licenses">
              <Building2 className="h-4 w-4 text-indigo-600" />
              Organization Licenses
            </Link>
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5"
          >
            <Link to="/super-admin/licensing/upgrade-requests">
              <Sparkles className="h-4 w-4 text-teal-600" />
              Upgrade Requests
            </Link>
          </Button>

          <Button
            asChild
            className="bg-slate-900 hover:bg-slate-800 text-white gap-1.5 shadow-xs"
          >
            <Link to="/super-admin/licensing/plans/new">
              <Plus className="h-4 w-4" />
              Create Plan
            </Link>
          </Button>
        </div>
      </div>

      {/* Distinction Guide Card */}
      <div className="rounded-lg border border-slate-200 bg-slate-50/70 p-3.5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 text-xs text-slate-600">
        <div className="flex items-start sm:items-center gap-2.5">
          <Info className="h-4 w-4 text-slate-500 shrink-0 mt-0.5 sm:mt-0" />
          <div>
            <span className="font-semibold text-slate-800">Master Plans vs. Organization Licenses:</span>{" "}
            Plans listed here are global templates. To view or manage an individual healthcare organization's assigned license, switch to Organization Licenses.
          </div>
        </div>
        <Button
          variant="ghost"
          size="sm"
          asChild
          className="text-xs text-indigo-600 hover:text-indigo-700 font-semibold shrink-0 gap-1 p-0 h-auto"
        >
          <Link to="/super-admin/licensing/licenses">
            Manage Organization Licenses
            <ArrowRight className="h-3 w-3" />
          </Link>
        </Button>
      </div>

      {/* Error state */}
      {isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {error instanceof Error
              ? error.message
              : "Failed to load plans from backend."}
          </AlertDescription>
        </Alert>
      )}

      {/* Main Content Card */}
      <Card className="border-slate-200 bg-white shadow-xs">
        <CardHeader className="pb-3 border-b border-slate-100">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Search plans by name or code..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 h-9 text-xs"
              />
            </div>
            <div className="text-xs text-slate-500">
              Annual pricing is authoritative for organization licensing.
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-3">
              <Skeleton className="h-12 w-full rounded-md" />
              <Skeleton className="h-12 w-full rounded-md" />
              <Skeleton className="h-12 w-full rounded-md" />
            </div>
          ) : filteredPlans.length === 0 ? (
            <div className="p-12 text-center space-y-3">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                <CreditCard className="h-6 w-6" />
              </div>
              <h3 className="text-sm font-semibold text-slate-900">
                {searchTerm ? "No matching plans" : "No plans configured"}
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                {searchTerm
                  ? "Try clearing your search query to see all plans."
                  : "Create subscription plans to begin licensing tenant organizations."}
              </p>
              {!searchTerm && (
                <div className="pt-2">
                  <Button
                    size="sm"
                    asChild
                    className="bg-slate-900 hover:bg-slate-800 text-white gap-1.5"
                  >
                    <Link to="/super-admin/licensing/plans/new">
                      <Plus className="h-4 w-4" />
                      Create Plan
                    </Link>
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-slate-50/70">
                  <TableRow>
                    <TableHead className="w-[120px]">Code</TableHead>
                    <TableHead>Name & Description</TableHead>
                    <TableHead className="w-[170px]">Capacity & Limits</TableHead>
                    <TableHead className="text-right">Annual Price</TableHead>
                    <TableHead className="w-[110px]">Status</TableHead>
                    <TableHead className="w-[130px]">Last Updated</TableHead>
                    <TableHead className="w-[80px] text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredPlans.map((plan) => (
                    <TableRow key={plan.refId} className="hover:bg-slate-50/60">
                      <TableCell>
                        <Badge
                          variant="outline"
                          className="font-mono text-xs font-semibold bg-slate-50 text-slate-800"
                        >
                          {plan.code}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-0.5">
                          <div className="font-semibold text-slate-900 text-sm">
                            {plan.name}
                          </div>
                          {plan.description && (
                            <p className="text-xs text-slate-500 line-clamp-1 max-w-md">
                              {plan.description}
                            </p>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-1 text-xs text-slate-700">
                          <div className="flex items-center gap-1.5 font-medium">
                            <Users className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                            <span>{plan.maxLabStaff ?? 3} staff seats</span>
                          </div>
                          <div className="flex items-center gap-1.5 text-slate-500 text-[11px]">
                            <FileText className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                            <span>
                              {plan.maxReportsPerMonth && plan.maxReportsPerMonth > 0
                                ? `${plan.maxReportsPerMonth.toLocaleString()}/mo (${plan.maxReportsPerDay ?? 25}/day)`
                                : "Unlimited reports"}
                            </span>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-medium text-slate-900">
                        <span className="text-xs text-slate-500 mr-1">
                          {plan.currency}
                        </span>
                        <span className="font-semibold">
                          {Number(plan.annualPrice).toLocaleString(undefined, {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2,
                          })}
                        </span>
                        <span className="text-xs text-slate-400 font-normal">
                          {" "}/ yr
                        </span>
                      </TableCell>
                      <TableCell>
                        <PlanStatusBadge active={plan.active} />
                      </TableCell>
                      <TableCell className="text-xs text-slate-500">
                        {new Date(plan.updatedAt).toLocaleDateString(undefined, {
                          year: "numeric",
                          month: "short",
                          day: "numeric",
                        })}
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900"
                            >
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-40">
                            <DropdownMenuLabel className="text-xs text-slate-500">
                              Plan Actions
                            </DropdownMenuLabel>
                            <DropdownMenuItem
                              onClick={() =>
                                navigate(
                                  `/super-admin/licensing/plans/${plan.refId}/edit`,
                                )
                              }
                            >
                              <Edit className="mr-2 h-4 w-4" />
                              Edit Plan
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => setPlanToToggle(plan)}
                              className={
                                plan.active
                                  ? "text-rose-600 focus:text-rose-700"
                                  : "text-emerald-600 focus:text-emerald-700"
                              }
                            >
                              {plan.active ? (
                                <>
                                  <ToggleLeft className="mr-2 h-4 w-4" />
                                  Deactivate
                                </>
                              ) : (
                                <>
                                  <ToggleRight className="mr-2 h-4 w-4" />
                                  Activate
                                </>
                              )}
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Confirmation Dialog for Plan Activation / Deactivation */}
      <ConfirmPlanStatusDialog
        plan={planToToggle}
        open={!!planToToggle}
        onOpenChange={(open) => {
          if (!open) setPlanToToggle(null);
        }}
        onConfirm={async () => {
          if (planToToggle) {
            await handleToggleStatus(planToToggle);
            setPlanToToggle(null);
          }
        }}
        isLoading={updateMutation.isPending}
      />
    </div>
  );
}

export default PlansPage;
