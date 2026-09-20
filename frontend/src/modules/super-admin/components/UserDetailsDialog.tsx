import { useState } from "react";
import {
  Calendar,
  Check,
  Clock,
  Copy,
  Edit,
  Mail,
  Shield,
  ShieldAlert,
  User as UserIcon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { useUserQuery } from "../hooks/useUsers";
import { UserRoleBadge } from "./UserRoleBadge";
import { UserStatusBadge } from "./UserStatusBadge";

interface UserDetailsDialogProps {
  refId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit?: (refId: string) => void;
  onChangeStatus?: (refId: string) => void;
}

export function UserDetailsDialog({
  refId,
  open,
  onOpenChange,
  onEdit,
  onChangeStatus,
}: UserDetailsDialogProps) {
  const [copied, setCopied] = useState(false);

  const {
    data: user,
    isLoading,
    isError,
    error,
  } = useUserQuery(refId, open);

  const copyRefId = () => {
    if (user?.refId) {
      void navigator.clipboard.writeText(user.refId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "Never";
    try {
      return new Date(isoString).toLocaleString(undefined, {
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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
              <UserIcon className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle className="text-lg font-semibold text-slate-900">
                User Details
              </DialogTitle>
              <DialogDescription className="text-xs text-slate-500">
                Authoritative user account information and operational status.
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {isLoading ? (
          <div className="space-y-4 py-4">
            <Skeleton className="h-6 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <div className="grid grid-cols-2 gap-3 pt-2">
              <Skeleton className="h-16 rounded-lg" />
              <Skeleton className="h-16 rounded-lg" />
              <Skeleton className="h-16 rounded-lg" />
              <Skeleton className="h-16 rounded-lg" />
            </div>
          </div>
        ) : isError || !user ? (
          <div className="py-4">
            <Alert variant="destructive">
              <ShieldAlert className="h-4 w-4" />
              <AlertTitle>Error Loading User</AlertTitle>
              <AlertDescription>
                {(error as Error)?.message ||
                  "Failed to load user details from the backend."}
              </AlertDescription>
            </Alert>
          </div>
        ) : (
          <div className="space-y-5 py-2">
            {/* User Profile Summary */}
            <div className="flex items-start justify-between rounded-lg border border-slate-100 bg-slate-50 p-4">
              <div className="space-y-1">
                <h3 className="text-base font-semibold text-slate-900">
                  {user.name}
                </h3>
                <div className="flex items-center gap-1.5 text-xs text-slate-600">
                  <Mail className="h-3.5 w-3.5 text-slate-400" />
                  <span>{user.email}</span>
                </div>
                <div className="flex items-center gap-1.5 pt-1 text-xs font-mono text-slate-500">
                  <span>Ref: {user.refId}</span>
                  <button
                    type="button"
                    onClick={copyRefId}
                    className="ml-1 inline-flex items-center text-slate-400 hover:text-slate-700 focus:outline-none"
                    title="Copy Ref ID"
                  >
                    {copied ? (
                      <Check className="h-3.5 w-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="h-3.5 w-3.5" />
                    )}
                  </button>
                </div>
              </div>

              <div className="flex flex-col items-end gap-1.5">
                <UserStatusBadge status={user.status} />
                <UserRoleBadge role={user.role} />
              </div>
            </div>

            {/* Structured Details Grid */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {/* Organization Assignment */}
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <p className="text-[11px] font-medium text-slate-500">
                  Organization Assignment
                </p>
                <p className="mt-1 text-xs font-mono font-semibold text-slate-800 break-all">
                  {user.organizationRefId ? (
                    user.organizationRefId
                  ) : (
                    <span className="font-sans text-slate-400 font-normal">
                      None (Platform Administrator)
                    </span>
                  )}
                </p>
              </div>

              {/* Last Login */}
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
                  <Clock className="h-3 w-3 text-slate-400" />
                  <span>Last Sign In</span>
                </div>
                <p className="mt-1 text-xs font-medium text-slate-800">
                  {formatDate(user.lastLoginAt)}
                </p>
              </div>

              {/* Account Created */}
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
                  <Calendar className="h-3 w-3 text-slate-400" />
                  <span>Created At</span>
                </div>
                <p className="mt-1 text-xs font-medium text-slate-800">
                  {formatDate(user.createdAt)}
                </p>
              </div>

              {/* Account Updated */}
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
                  <Calendar className="h-3 w-3 text-slate-400" />
                  <span>Last Updated</span>
                </div>
                <p className="mt-1 text-xs font-medium text-slate-800">
                  {formatDate(user.updatedAt)}
                </p>
              </div>
            </div>

            {/* Footer / Quick Actions */}
            <DialogFooter className="pt-2 sm:justify-between items-center">
              <div>
                {user.role === "SUPER_ADMIN" ? (
                  <span className="text-[11px] text-purple-700 font-medium">
                    Protected Super Admin account
                  </span>
                ) : (
                  <div className="flex items-center gap-2">
                    {onEdit && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          onOpenChange(false);
                          onEdit(user.refId);
                        }}
                      >
                        <Edit className="mr-1.5 h-3.5 w-3.5" />
                        Edit User
                      </Button>
                    )}
                    {onChangeStatus && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          onOpenChange(false);
                          onChangeStatus(user.refId);
                        }}
                      >
                        <Shield className="mr-1.5 h-3.5 w-3.5" />
                        Change Status
                      </Button>
                    )}
                  </div>
                )}
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Close
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
