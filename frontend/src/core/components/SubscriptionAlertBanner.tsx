import { Link } from "react-router-dom";
import { AlertCircle, AlertTriangle, ArrowRight, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  useSubscriptionEligibility,
  type ExpiryStatusType,
} from "../auth/useSubscriptionEligibility";

interface SubscriptionAlertBannerProps {
  className?: string;
  showWhenExpiringSoon?: boolean;
}

export function SubscriptionAlertBanner({
  className = "",
  showWhenExpiringSoon = true,
}: SubscriptionAlertBannerProps) {
  const {
    isEligible,
    isLoading,
    expiryStatus,
    reasonMessage,
    canManageSubscription,
    subscriptionManagementPath,
    daysRemaining,
  } = useSubscriptionEligibility();

  if (isLoading) return null;

  // Active plans don't show any banner
  if (expiryStatus === "ACTIVE" || expiryStatus === "UNKNOWN") {
    return null;
  }

  // Expiring soon banner (amber warning)
  if (expiryStatus === "EXPIRING_SOON") {
    if (!showWhenExpiringSoon) return null;

    return (
      <div
        className={`bg-amber-50 border border-amber-200 text-amber-900 rounded-lg p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-2xs ${className}`}
      >
        <div className="flex items-start sm:items-center gap-2.5">
          <AlertTriangle className="size-4 text-amber-600 shrink-0 mt-0.5 sm:mt-0" />
          <div className="text-xs">
            <span className="font-semibold">Subscription Expiring Soon: </span>
            <span>
              Your plan expires in {daysRemaining} day{daysRemaining === 1 ? "" : "s"}. Renew now to avoid interruption to patient registrations and diagnostic reporting.
            </span>
          </div>
        </div>

        {canManageSubscription && (
          <Button
            size="sm"
            variant="outline"
            asChild
            className="h-7 text-xs border-amber-300 text-amber-900 hover:bg-amber-100 shrink-0 bg-white"
          >
            <Link to={subscriptionManagementPath}>
              Renew Plan <ArrowRight className="size-3 ml-1" />
            </Link>
          </Button>
        )}
      </div>
    );
  }

  // Inactive / Expired / Deactivated / No Plan banner (rose destructive alert)
  const title =
    expiryStatus === "NO_PLAN"
      ? "No Plan Active"
      : expiryStatus === "DEACTIVATED"
      ? "Subscription Deactivated"
      : "Subscription Expired";

  const message =
    reasonMessage ||
    (expiryStatus === "NO_PLAN"
      ? "No plan active. Please contact Super Admin to activate a plan."
      : "An active subscription is required to perform operational actions such as registering patients, assigning tests, and generating clinical reports.");

  return (
    <div
      className={`bg-rose-50 border border-rose-200 text-rose-900 rounded-lg p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3.5 shadow-2xs ${className}`}
    >
      <div className="flex items-start sm:items-center gap-3">
        <div className="size-8 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
          <ShieldAlert className="size-4" />
        </div>
        <div>
          <h4 className="text-sm font-semibold text-rose-950 flex items-center gap-1.5">
            <AlertCircle className="size-3.5 text-rose-600" />
            {title}
          </h4>
          <p className="text-xs text-rose-800 mt-0.5 max-w-2xl leading-relaxed">
            {message}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0 pt-1 sm:pt-0">
        {canManageSubscription ? (
          <Button
            size="sm"
            asChild
            className="bg-rose-700 hover:bg-rose-800 text-white text-xs h-8 shadow-xs"
          >
            <Link to={subscriptionManagementPath}>
              {expiryStatus === "NO_PLAN" ? "View Plans / Request Plan" : "Renew / Reactivate"}
              <ArrowRight className="size-3.5 ml-1.5" />
            </Link>
          </Button>
        ) : (
          <span className="text-xs font-medium text-rose-700 italic">
            Contact Super Admin to activate plan
          </span>
        )}
      </div>
    </div>
  );
}
