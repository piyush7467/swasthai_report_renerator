import { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import {
  Sparkles,
  RefreshCw,
  Search,
  CheckCircle2,
  Clock,
  PhoneCall,
  AlertCircle,
  Building2,
  CreditCard,
  ArrowRight,
  Eye,
  Mail,
  MoreVertical,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
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
import { useAdminUpgradeRequestsQuery } from "../hooks/useLicensing";
import { ReviewUpgradeRequestModal } from "../components/ReviewUpgradeRequestModal";
import type {
  PlanUpgradeRequestResponse,
  UpgradeRequestStatus,
} from "../types/licensingTypes";

export function AdminUpgradeRequestsPage() {
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedRequest, setSelectedRequest] =
    useState<PlanUpgradeRequestResponse | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [page, setPage] = useState(0);

  const {
    data: pagedData,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useAdminUpgradeRequestsQuery({
    status: statusFilter === "ALL" ? undefined : statusFilter,
    page,
    size: 20,
  });

  const requests: PlanUpgradeRequestResponse[] = useMemo(() => {
    if (!pagedData) return [];
    if (Array.isArray(pagedData)) return pagedData as unknown as PlanUpgradeRequestResponse[];
    if (Array.isArray(pagedData.content)) return pagedData.content;
    return [];
  }, [pagedData]);

  // Client-side text filter for search
  const filteredRequests = useMemo(() => {
    const term = searchTerm.toLowerCase().trim();
    if (!term) return requests;
    return requests.filter(
      (r) =>
        r.organizationName.toLowerCase().includes(term) ||
        r.contactName.toLowerCase().includes(term) ||
        r.contactEmail.toLowerCase().includes(term) ||
        r.refId.toLowerCase().includes(term) ||
        r.requestedPlanName.toLowerCase().includes(term)
    );
  }, [requests, searchTerm]);

  // Aggregate counts from current view
  const pendingCount = requests.filter((r) => r.status === "PENDING").length;
  const contactedCount = requests.filter((r) => r.status === "CONTACTED").length;
  const approvedCount = requests.filter((r) => r.status === "APPROVED").length;

  const handleOpenReview = (request: PlanUpgradeRequestResponse) => {
    setSelectedRequest(request);
    setModalOpen(true);
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "N/A";
    try {
      return new Date(isoString).toLocaleDateString("en-IN", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  const getStatusBadge = (status: UpgradeRequestStatus) => {
    switch (status) {
      case "PENDING":
        return (
          <Badge
            variant="outline"
            className="bg-amber-50 text-amber-700 border-amber-200 gap-1 text-[11px] font-medium"
          >
            <Clock className="h-3 w-3" />
            Pending Review
          </Badge>
        );
      case "CONTACTED":
        return (
          <Badge
            variant="outline"
            className="bg-blue-50 text-blue-700 border-blue-200 gap-1 text-[11px] font-medium"
          >
            <PhoneCall className="h-3 w-3" />
            Contacted
          </Badge>
        );
      case "APPROVED":
        return (
          <Badge
            variant="outline"
            className="bg-emerald-50 text-emerald-700 border-emerald-200 gap-1 text-[11px] font-medium"
          >
            <CheckCircle2 className="h-3 w-3" />
            Approved
          </Badge>
        );
      case "REJECTED":
        return (
          <Badge
            variant="outline"
            className="bg-rose-50 text-rose-700 border-rose-200 gap-1 text-[11px] font-medium"
          >
            <AlertCircle className="h-3 w-3" />
            Rejected
          </Badge>
        );
      case "CANCELLED":
        return (
          <Badge
            variant="outline"
            className="bg-slate-50 text-slate-600 border-slate-200 text-[11px]"
          >
            Cancelled
          </Badge>
        );
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
              Subscription Upgrade Requests
            </h1>
            <Badge variant="outline" className="font-mono text-xs bg-slate-50 text-slate-700">
              Super Admin Review
            </Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl">
            Review and process manual plan upgrade inquiries submitted by healthcare organization administrators.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1 sm:pb-0">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void refetch()}
            disabled={isFetching || isLoading}
            className="text-slate-600 hover:text-slate-900 text-xs h-8 shrink-0"
            title="Refresh requests"
          >
            <RefreshCw
              className={`mr-1.5 h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`}
            />
            <span>Refresh</span>
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5 border-slate-200 text-xs h-8 shrink-0"
          >
            <Link to="/super-admin/licensing/plans">
              <CreditCard className="h-3.5 w-3.5 text-teal-600" />
              <span>Plans Catalog</span>
            </Link>
          </Button>

          <Button
            variant="outline"
            size="sm"
            asChild
            className="text-slate-700 hover:text-slate-900 gap-1.5 border-slate-200 text-xs h-8 shrink-0"
          >
            <Link to="/super-admin/licensing/licenses">
              <Building2 className="h-3.5 w-3.5 text-teal-600" />
              <span>Org Licenses</span>
            </Link>
          </Button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-4">
        <Card className="border-slate-200 shadow-xs">
          <CardContent className="p-3 sm:p-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] sm:text-xs text-slate-500 font-medium">Total Requests</p>
              <p className="text-lg sm:text-xl font-bold text-slate-900 mt-0.5">
                {pagedData?.totalElements ?? requests.length}
              </p>
            </div>
            <div className="p-2 sm:p-2.5 rounded-lg bg-slate-100 text-slate-600">
              <Sparkles className="h-4 sm:h-5 w-4 sm:w-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-amber-200 bg-amber-50/30 shadow-xs">
          <CardContent className="p-3 sm:p-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] sm:text-xs text-amber-700 font-medium">Pending Review</p>
              <p className="text-lg sm:text-xl font-bold text-amber-900 mt-0.5">{pendingCount}</p>
            </div>
            <div className="p-2 sm:p-2.5 rounded-lg bg-amber-100 text-amber-700">
              <Clock className="h-4 sm:h-5 w-4 sm:w-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-blue-200 bg-blue-50/30 shadow-xs">
          <CardContent className="p-3 sm:p-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] sm:text-xs text-blue-700 font-medium">In Contact</p>
              <p className="text-lg sm:text-xl font-bold text-blue-900 mt-0.5">{contactedCount}</p>
            </div>
            <div className="p-2 sm:p-2.5 rounded-lg bg-blue-100 text-blue-700">
              <PhoneCall className="h-4 sm:h-5 w-4 sm:w-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-emerald-200 bg-emerald-50/30 shadow-xs">
          <CardContent className="p-3 sm:p-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] sm:text-xs text-emerald-700 font-medium">Approved</p>
              <p className="text-lg sm:text-xl font-bold text-emerald-900 mt-0.5">{approvedCount}</p>
            </div>
            <div className="p-2 sm:p-2.5 rounded-lg bg-emerald-100 text-emerald-700">
              <CheckCircle2 className="h-4 sm:h-5 w-4 sm:w-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="border-slate-200 shadow-xs">
        <CardContent className="p-3 sm:p-3.5 space-y-3 sm:space-y-0 sm:flex sm:items-center sm:justify-between gap-4">
          {/* Status Buttons Pill Rail (horizontal scrollable on mobile) */}
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1 sm:pb-0 w-full sm:w-auto">
            {["ALL", "PENDING", "CONTACTED", "APPROVED", "REJECTED", "CANCELLED"].map(
              (st) => (
                <Button
                  key={st}
                  variant={statusFilter === st ? "default" : "outline"}
                  size="sm"
                  onClick={() => {
                    setStatusFilter(st);
                    setPage(0);
                  }}
                  className={`text-xs h-8 whitespace-nowrap shrink-0 ${
                    statusFilter === st
                      ? "bg-[#0F766E] text-white shadow-xs hover:bg-[#115E59]"
                      : "text-slate-600 hover:text-slate-900 border-slate-200"
                  }`}
                >
                  {st === "ALL" ? "All Requests" : st.charAt(0) + st.slice(1).toLowerCase()}
                </Button>
              )
            )}
          </div>

          {/* Search Box */}
          <div className="relative w-full sm:w-72 shrink-0">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
            <Input
              placeholder="Search org, contact, ref..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 text-xs h-8 bg-white"
            />
          </div>
        </CardContent>
      </Card>

      {/* Requests Table */}
      <Card className="border-slate-200 shadow-xs">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : isError ? (
            <div className="p-6">
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  {error?.message || "Failed to load upgrade requests."}
                </AlertDescription>
              </Alert>
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Sparkles className="h-10 w-10 mx-auto text-slate-300 mb-2" />
              <p className="font-semibold text-slate-700">No upgrade requests found</p>
              <p className="text-xs text-slate-400 mt-1">
                {statusFilter !== "ALL"
                  ? `No requests match the "${statusFilter}" filter.`
                  : "No organizations have submitted upgrade requests yet."}
              </p>
            </div>
          ) : (
            <>
              {/* Mobile Cards View (< md) */}
              <div className="md:hidden divide-y divide-slate-100">
                {filteredRequests.map((req) => (
                  <div key={req.refId} className="p-4 space-y-3 hover:bg-slate-50/50 transition-colors">
                    {/* Header: Org Name + Status Badge + 3-Dot Menu */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-0.5 pr-1">
                        <h4 className="font-semibold text-sm text-slate-900 leading-snug">
                          {req.organizationName}
                        </h4>
                        <span className="font-mono text-[11px] text-slate-400 block">
                          {req.organizationRefId} &bull; {req.refId}
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        {getStatusBadge(req.status)}

                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900 rounded-md"
                              aria-label="Request actions"
                            >
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="w-48 text-xs shadow-md">
                            <DropdownMenuLabel className="text-[11px] text-slate-500 font-medium">
                              Request Actions
                            </DropdownMenuLabel>
                            <DropdownMenuItem
                              onClick={() => handleOpenReview(req)}
                              className="cursor-pointer flex items-center gap-2 py-2"
                            >
                              <Eye className="h-3.5 w-3.5 text-slate-500" />
                              <span>{req.status === "PENDING" || req.status === "CONTACTED" ? "Review & Process" : "View Details"}</span>
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => {
                                window.location.href = `mailto:${req.contactEmail}?subject=Regarding your plan upgrade request (${req.refId})`;
                              }}
                              className="cursor-pointer flex items-center gap-2 py-2"
                            >
                              <Mail className="h-3.5 w-3.5 text-slate-500" />
                              <span>Email Requester</span>
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem asChild className="cursor-pointer flex items-center gap-2 py-2">
                              <Link to={`/super-admin/licensing/licenses?org=${req.organizationRefId}`}>
                                <Building2 className="h-3.5 w-3.5 text-slate-500" />
                                <span>View Org License</span>
                              </Link>
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>

                    <div className="rounded-lg bg-slate-50/80 p-2.5 text-xs space-y-1.5 border border-slate-100">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-slate-500">Transition:</span>
                        <span className="text-slate-700 font-medium">{req.currentPlanName}</span>
                        <ArrowRight className="h-3 w-3 text-slate-400 shrink-0" />
                        <span className="text-teal-900 font-bold bg-teal-50 px-1.5 py-0.5 rounded-sm border border-teal-100">
                          {req.requestedPlanName}
                        </span>
                        <span className="text-[11px] text-slate-400">
                          ({req.requestedStaffCapacity} seats)
                        </span>
                      </div>

                      <div className="text-[11px] text-slate-600 flex items-center justify-between pt-1 border-t border-slate-200/60">
                        <span>Requester: <strong className="text-slate-800">{req.contactName}</strong></span>
                        <a href={`mailto:${req.contactEmail}`} className="text-teal-700 hover:underline">
                          {req.contactEmail}
                        </a>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1 text-xs">
                      <span className="text-[11px] text-slate-400">
                        {formatDate(req.createdAt)}
                      </span>
                      <Button
                        variant={req.status === "PENDING" ? "default" : "outline"}
                        size="sm"
                        onClick={() => handleOpenReview(req)}
                        className={`text-xs h-7 gap-1 ${
                          req.status === "PENDING"
                            ? "bg-[#0F766E] hover:bg-[#115E59] text-white shadow-xs"
                            : "text-slate-700"
                        }`}
                      >
                        <Eye className="h-3 w-3" />
                        {req.status === "PENDING" || req.status === "CONTACTED"
                          ? "Review Request"
                          : "Details"}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop Table (>= md) */}
              <div className="hidden md:block overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50/70 hover:bg-slate-50/70">
                      <TableHead className="text-xs font-semibold text-slate-600 w-[140px]">
                        Request Ref
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600">
                        Organization
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600">
                        Plan Transition
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600">
                        Requester Contact
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600">
                        Status
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600">
                        Submitted
                      </TableHead>
                      <TableHead className="text-xs font-semibold text-slate-600 text-right">
                        Actions
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredRequests.map((req) => (
                      <TableRow key={req.refId} className="hover:bg-slate-50/50">
                        <TableCell className="font-mono text-xs text-slate-600">
                          {req.refId}
                        </TableCell>

                        <TableCell>
                          <div>
                            <span className="font-semibold text-slate-800 text-xs block">
                              {req.organizationName}
                            </span>
                            <span className="font-mono text-[11px] text-slate-400">
                              {req.organizationRefId}
                            </span>
                          </div>
                        </TableCell>

                        <TableCell>
                          <div className="flex items-center gap-1.5 text-xs">
                            <span className="text-slate-600 font-medium">
                              {req.currentPlanName}
                            </span>
                            <ArrowRight className="h-3 w-3 text-slate-400 shrink-0" />
                            <span className="text-teal-900 font-bold bg-teal-50 px-1.5 py-0.5 rounded-sm border border-teal-100">
                              {req.requestedPlanName}
                            </span>
                            <span className="text-[11px] text-slate-400">
                              ({req.requestedStaffCapacity} seats)
                            </span>
                          </div>
                        </TableCell>

                        <TableCell>
                          <div className="text-xs">
                            <span className="font-medium text-slate-800 block">
                              {req.contactName}
                            </span>
                            <a
                              href={`mailto:${req.contactEmail}`}
                              className="text-teal-700 hover:underline text-[11px] block"
                            >
                              {req.contactEmail}
                            </a>
                          </div>
                        </TableCell>

                        <TableCell>{getStatusBadge(req.status)}</TableCell>

                        <TableCell className="text-xs text-slate-500 whitespace-nowrap">
                          {formatDate(req.createdAt)}
                        </TableCell>

                        <TableCell className="text-right">
                          <Button
                            variant={req.status === "PENDING" ? "default" : "outline"}
                            size="sm"
                            onClick={() => handleOpenReview(req)}
                            className={`text-xs h-7 gap-1 ${
                              req.status === "PENDING"
                                ? "bg-[#0F766E] hover:bg-[#115E59] text-white shadow-xs"
                                : "text-slate-700"
                            }`}
                          >
                            <Eye className="h-3 w-3" />
                            {req.status === "PENDING" || req.status === "CONTACTED"
                              ? "Review"
                              : "Details"}
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Review & Action Modal */}
      <ReviewUpgradeRequestModal
        request={selectedRequest}
        isOpen={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setSelectedRequest(null);
        }}
        onSuccess={() => {
          void refetch();
        }}
      />
    </div>
  );
}

export default AdminUpgradeRequestsPage;
