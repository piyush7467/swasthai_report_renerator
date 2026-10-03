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
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
              Subscription Plans
            </h1>
            <Badge variant="outline" className="font-mono text-xs bg-slate-50 text-slate-700">
              {plans.length} {plans.length === 1 ? "Plan" : "Plans"}
            </Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl">
            Configure global master subscription tiers (e.g. Basic, Standard, Enterprise) available for tenant organizations.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1 sm:pb-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => void refetch()}
              disabled={isFetching || isLoading}
              className="text-slate-600 hover:text-slate-900 text-xs h-8 shrink-0"
              title="Refresh plans"
            >
              <RefreshCw className={`mr-1.5 h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`} />
              <span>Refresh</span>
            </Button>

            <Button
              variant="outline"
              size="sm"
              asChild
              className="text-slate-700 hover:text-slate-900 gap-1.5 border-slate-200 text-xs h-8 shrink-0"
            >
              <Link to="/super-admin/licensing/licenses">
                <Building2 className="h-3.5 w-3.5 text-teal-600" />
                <span className="hidden sm:inline">Organization </span>Licenses
              </Link>
            </Button>

            <Button
              variant="outline"
              size="sm"
              asChild
              className="text-slate-700 hover:text-slate-900 gap-1.5 border-slate-200 text-xs h-8 shrink-0"
            >
              <Link to="/super-admin/licensing/upgrade-requests">
                <Sparkles className="h-3.5 w-3.5 text-teal-600" />
                <span className="hidden sm:inline">Upgrade </span>Requests
              </Link>
            </Button>
          </div>

          <Button
            asChild
            size="sm"
            className="bg-[#0F766E] hover:bg-[#115E59] text-white gap-1.5 shadow-2xs font-semibold text-xs h-8 shrink-0 justify-center"
          >
            <Link to="/super-admin/licensing/plans/new">
              <Plus className="h-3.5 w-3.5" />
              <span>Create Plan</span>
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
                    className="bg-[#0F766E] hover:bg-[#115E59] text-white gap-1.5 shadow-2xs font-semibold"
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
            <>
              {/* Desktop Table View */}
              <div className="hidden md:block overflow-x-auto">
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
                                className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900 rounded-md"
                              >
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-40 text-xs">
                              <DropdownMenuLabel className="text-xs text-slate-500">
                                Plan Actions
                              </DropdownMenuLabel>
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(
                                    `/super-admin/licensing/plans/${plan.refId}/edit`,
                                  )
                                }
                                className="cursor-pointer flex items-center gap-2"
                              >
                                <Edit className="h-3.5 w-3.5 text-slate-500" />
                                Edit Plan
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={() => setPlanToToggle(plan)}
                                className={`cursor-pointer flex items-center gap-2 ${
                                  plan.active
                                    ? "text-rose-600 focus:text-rose-700"
                                    : "text-emerald-600 focus:text-emerald-700"
                                }`}
                              >
                                {plan.active ? (
                                  <>
                                    <ToggleLeft className="h-3.5 w-3.5" />
                                    Deactivate
                                  </>
                                ) : (
                                  <>
                                    <ToggleRight className="h-3.5 w-3.5" />
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

              {/* Mobile Card View */}
              <div className="md:hidden divide-y divide-slate-100">
                {filteredPlans.map((plan) => (
                  <div key={plan.refId} className="p-4 space-y-3 hover:bg-slate-50/50 transition-colors">
                    {/* Card Header: Name + Code on left, Status + 3-Dot Menu on right */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1 pr-2">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h4 className="font-bold text-sm text-slate-900">{plan.name}</h4>
                          <span className="font-mono text-[11px] font-semibold text-slate-600 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded">
                            {plan.code}
                          </span>
                        </div>
                        {plan.description && (
                          <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">
                            {plan.description}
                          </p>
                        )}
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        <PlanStatusBadge active={plan.active} />

                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900 rounded-md"
                              aria-label="Plan actions"
                            >
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-44 text-xs shadow-md">
                            <DropdownMenuLabel className="text-[11px] text-slate-500 font-medium">
                              Plan Actions
                            </DropdownMenuLabel>
                            <DropdownMenuItem
                              onClick={() => navigate(`/super-admin/licensing/plans/${plan.refId}/edit`)}
                              className="cursor-pointer flex items-center gap-2 py-2"
                            >
                              <Edit className="h-3.5 w-3.5 text-slate-500" />
                              <span>Edit Plan</span>
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => setPlanToToggle(plan)}
                              className={`cursor-pointer flex items-center gap-2 py-2 ${
                                plan.active
                                  ? "text-rose-600 focus:text-rose-700 focus:bg-rose-50"
                                  : "text-emerald-600 focus:text-emerald-700 focus:bg-emerald-50"
                              }`}
                            >
                              {plan.active ? (
                                <>
                                  <ToggleLeft className="h-3.5 w-3.5" />
                                  <span>Deactivate Plan</span>
                                </>
                              ) : (
                                <>
                                  <ToggleRight className="h-3.5 w-3.5" />
                                  <span>Activate Plan</span>
                                </>
                              )}
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>

                    {/* Capacity and Pricing Stats Strip */}
                    <div className="grid grid-cols-2 gap-2 text-xs p-2.5 rounded-lg bg-slate-50/80 border border-slate-100">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">Capacity</span>
                        <div className="flex items-center gap-1.5 mt-0.5 font-medium text-slate-700 text-xs">
                          <Users className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span>{plan.maxLabStaff ?? 3} staff seats</span>
                        </div>
                        <div className="flex items-center gap-1.5 text-slate-500 text-[11px] mt-0.5">
                          <FileText className="h-3 w-3 text-slate-400 shrink-0" />
                          <span>{plan.maxReportsPerMonth ? `${plan.maxReportsPerMonth}/mo` : "Unlimited reports"}</span>
                        </div>
                      </div>

                      <div className="border-l border-slate-200/80 pl-2.5">
                        <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">Annual Price</span>
                        <div className="mt-0.5">
                          <span className="text-xs font-semibold text-slate-500 mr-1">{plan.currency}</span>
                          <span className="text-sm font-bold text-slate-900">
                            {Number(plan.annualPrice).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </span>
                          <span className="text-[10px] text-slate-400 block">per year</span>
                        </div>
                      </div>
                    </div>

                    {/* Card Footer: Last Updated Date */}
                    <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                      <span>Updated {new Date(plan.updatedAt).toLocaleDateString(undefined, {
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      })}</span>
                      <span className="font-mono text-[10px] text-slate-400">Ref: {plan.refId.substring(0, 8)}...</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
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
