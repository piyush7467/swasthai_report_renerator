import { Badge } from "@/components/ui/badge";
import { CheckCircle2, Clock, AlertTriangle, PauseCircle } from "lucide-react";
import type { LicenseStatus } from "../types/licensingTypes";
import { getLicenseExpiryInfo } from "../types/licensingTypes";

interface LicenseStatusBadgeProps {
  status: LicenseStatus;
  currentlyUsable?: boolean;
  showUsability?: boolean;
  expiresAt?: string;
  showDaysLeft?: boolean;
  className?: string;
}

export function LicenseStatusBadge({
  status,
  currentlyUsable,
  showUsability = false,
  expiresAt,
  showDaysLeft = false,
  className = "",
}: LicenseStatusBadgeProps) {
  const expiryInfo = showDaysLeft && expiresAt ? getLicenseExpiryInfo(expiresAt, undefined, status) : null;

  if (status === "ACTIVE") {
    const isUsable = currentlyUsable ?? true;
    return (
      <div className={`inline-flex flex-wrap items-center gap-1.5 ${className}`}>
        <Badge
          variant="outline"
          className="border-emerald-200 bg-emerald-50 text-emerald-700 font-semibold px-2.5 py-0.5"
        >
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-emerald-500 inline-block animate-pulse" />
          ACTIVE
        </Badge>

        {showUsability && (
          <Badge
            variant="outline"
            className={
              isUsable
                ? "border-green-200 bg-green-50 text-green-700 text-[11px]"
                : "border-amber-200 bg-amber-50 text-amber-700 text-[11px]"
            }
          >
            {isUsable ? (
              <>
                <CheckCircle2 className="mr-1 h-3 w-3 text-green-600" />
                Usable
              </>
            ) : (
              <>
                <AlertTriangle className="mr-1 h-3 w-3 text-amber-600" />
                Unusable
              </>
            )}
          </Badge>
        )}

        {expiryInfo && (
          <Badge
            variant="outline"
            className={
              expiryInfo.isExpired
                ? "border-rose-200 bg-rose-50 text-rose-700 text-[11px] font-medium"
                : expiryInfo.isUrgent
                ? "border-rose-300 bg-rose-50 text-rose-800 text-[11px] font-semibold animate-pulse"
                : expiryInfo.isExpiringSoon
                ? "border-amber-300 bg-amber-50 text-amber-800 text-[11px] font-semibold"
                : "border-slate-200 bg-slate-50 text-slate-700 text-[11px] font-medium"
            }
          >
            <Clock className="mr-1 h-3 w-3" />
            {expiryInfo.label}
          </Badge>
        )}
      </div>
    );
  }

  if (status === "DEACTIVATED") {
    return (
      <div className={`inline-flex flex-wrap items-center gap-1.5 ${className}`}>
        <Badge
          variant="outline"
          className="border-amber-300 bg-amber-50 text-amber-800 font-semibold px-2.5 py-0.5"
        >
          <PauseCircle className="mr-1.5 h-3.5 w-3.5 text-amber-600" />
          DEACTIVATED
        </Badge>

        {showUsability && (
          <Badge
            variant="outline"
            className="border-amber-200 bg-amber-50 text-amber-700 text-[11px]"
          >
            <AlertTriangle className="mr-1 h-3 w-3 text-amber-600" />
            Blocked
          </Badge>
        )}

        {expiryInfo && (
          <Badge
            variant="outline"
            className="border-amber-200 bg-white text-amber-800 text-[11px] font-medium"
          >
            <Clock className="mr-1 h-3 w-3" />
            {expiryInfo.label}
          </Badge>
        )}
      </div>
    );
  }

  return (
    <div className={`inline-flex flex-wrap items-center gap-1.5 ${className}`}>
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700 font-semibold px-2.5 py-0.5"
      >
        <Clock className="mr-1.5 h-3 w-3 text-rose-500" />
        EXPIRED
      </Badge>

      {expiryInfo && (
        <Badge
          variant="outline"
          className="border-rose-200 bg-rose-50 text-rose-700 text-[11px] font-medium"
        >
          {expiryInfo.label}
        </Badge>
      )}
    </div>
  );
}


