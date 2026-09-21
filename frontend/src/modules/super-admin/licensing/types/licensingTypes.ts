export type LicenseStatus = "ACTIVE" | "DEACTIVATED" | "EXPIRED";

export interface PlanResponse {
  refId: string;
  code: string;
  name: string;
  description?: string | null;
  annualPrice: number;
  currency: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  code: string;
  name: string;
  description?: string;
  annualPrice: number;
  currency: string;
  active?: boolean;
}

export interface UpdatePlanRequest {
  name: string;
  description?: string;
  annualPrice: number;
  currency: string;
  active: boolean;
}

export interface LicenseResponse {
  refId: string;
  organizationRefId: string;
  planRefId: string;
  planCode: string;
  planName: string;
  status: LicenseStatus;
  startedAt: string;
  expiresAt: string;
  currentlyUsable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ActivateLicenseRequest {
  planRefId: string;
  paymentReference?: string;
}

export interface RenewLicenseRequest {
  planRefId: string;
  paymentReference?: string;
}

export interface LicenseExpiryInfo {
  daysLeft: number;
  isExpired: boolean;
  isExpiringSoon: boolean; // <= 30 days
  isUrgent: boolean; // <= 7 days
  isDeactivated?: boolean;
  label: string;
  percentElapsed?: number; // 0 to 100
}

export function getLicenseExpiryInfo(
  expiresAtStr?: string | null,
  startedAtStr?: string | null,
  status?: LicenseStatus,
): LicenseExpiryInfo | null {
  if (!expiresAtStr) return null;
  const expiresAt = new Date(expiresAtStr).getTime();
  if (isNaN(expiresAt)) return null;

  const now = Date.now();
  const diffMs = expiresAt - now;
  const daysLeft = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  const isDeactivated = status === "DEACTIVATED";
  const isExpired = !isDeactivated && diffMs <= 0;
  const isExpiringSoon = !isDeactivated && !isExpired && daysLeft <= 30;
  const isUrgent = !isDeactivated && !isExpired && daysLeft <= 7;

  let label = "";
  if (isDeactivated) {
    label = daysLeft > 0 ? `${daysLeft} days frozen (Paused)` : "Deactivated (Paused)";
  } else if (daysLeft > 1) {
    label = `${daysLeft} days left`;
  } else if (daysLeft === 1) {
    label = "1 day left";
  } else if (daysLeft === 0) {
    label = "Expires today";
  } else if (daysLeft === -1) {
    label = "Expired 1 day ago";
  } else {
    label = `Expired ${Math.abs(daysLeft)} days ago`;
  }

  let percentElapsed: number | undefined;
  if (startedAtStr) {
    const startedAt = new Date(startedAtStr).getTime();
    if (!isNaN(startedAt) && expiresAt > startedAt) {
      const elapsed = now - startedAt;
      const total = expiresAt - startedAt;
      percentElapsed = Math.min(100, Math.max(0, Math.round((elapsed / total) * 100)));
    }
  }

  return {
    daysLeft,
    isExpired,
    isExpiringSoon,
    isUrgent,
    label,
    percentElapsed,
  };
}
