import { useState, useEffect, useMemo } from "react";
import { Building2, User as UserIcon } from "lucide-react";
import logoImg from "@/assets/logo.png";
import { useAuth } from "../auth/AuthContext";
import {
  useMyOrganizationProfileQuery,
  useMyLogoBlobQuery,
} from "@/modules/org-admin/settings/hooks/useOrgProfile";

export interface UserAvatarProps {
  size?: "xs" | "sm" | "md" | "lg" | "xl";
  className?: string;
  showFallbackInitials?: boolean;
}

const sizeClasses = {
  xs: "h-6 w-6 text-[10px]",
  sm: "h-7 w-7 text-xs",
  md: "h-8 w-8 text-xs",
  lg: "h-9 w-9 text-sm",
  xl: "h-11 w-11 text-base",
};

export function UserAvatar({
  size = "md",
  className = "",
  showFallbackInitials = true,
}: UserAvatarProps) {
  const { user } = useAuth();
  const [imgError, setImgError] = useState(false);
  const [logoUrl, setLogoUrl] = useState<string | null>(null);

  const isSuperAdmin = user?.role === "SUPER_ADMIN";
  const isOrgUser = Boolean(user && user.role !== "SUPER_ADMIN" && user.organizationRefId);

  // Fetch organization profile only for org admin or lab staff
  const { data: profile } = useMyOrganizationProfileQuery(isOrgUser);

  // Fetch organization logo blob when configured
  const hasOrgLogo = Boolean(isOrgUser && profile?.logoConfigured);
  const { data: logoBlob } = useMyLogoBlobQuery(hasOrgLogo);

  useEffect(() => {
    if (logoBlob) {
      const url = URL.createObjectURL(logoBlob);
      setLogoUrl(url);
      setImgError(false);
      return () => {
        URL.revokeObjectURL(url);
      };
    } else {
      setLogoUrl(null);
    }
  }, [logoBlob]);

  // Generate fallback initials
  const initials = useMemo(() => {
    const nameToUse = (profile?.organizationName || user?.name || "").trim();
    if (!nameToUse) return "";
    const parts = nameToUse.split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return parts[0].substring(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }, [profile?.organizationName, user?.name]);

  const sizeClass = sizeClasses[size] || sizeClasses.md;

  // 1. Super Admin DP: Always use official SwasthAI Logo
  if (isSuperAdmin) {
    return (
      <div
        className={`relative flex shrink-0 items-center justify-center rounded-full bg-white border border-teal-200/80 shadow-2xs overflow-hidden select-none ${sizeClass} ${className}`}
        title={`${user?.name || "Super Admin"} (System Super Admin)`}
      >
        <img
          src={logoImg}
          alt="SwasthAI Admin Logo"
          className="h-full w-full object-contain p-0.5"
        />
      </div>
    );
  }

  // 2. Organization User (Org Admin / Lab Staff): Use Org Logo if present
  if (isOrgUser && hasOrgLogo && logoUrl && !imgError) {
    return (
      <div
        className={`relative flex shrink-0 items-center justify-center rounded-full bg-white border border-slate-200 shadow-2xs overflow-hidden select-none ${sizeClass} ${className}`}
        title={profile?.organizationName || user?.name || "Organization"}
      >
        <img
          src={logoUrl}
          alt={profile?.organizationName || "Organization Logo"}
          onError={() => setImgError(true)}
          className="h-full w-full object-contain p-0.5 rounded-full"
        />
      </div>
    );
  }

  // 3. Fallback when no org logo is present
  return (
    <div
      className={`relative flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-teal-600 to-teal-800 text-white font-bold shadow-2xs select-none ${sizeClass} ${className}`}
      title={user?.name || "User"}
    >
      {showFallbackInitials && initials ? (
        <span className="tracking-wider">{initials}</span>
      ) : isOrgUser ? (
        <Building2 className="h-4 w-4 text-teal-100" />
      ) : (
        <UserIcon className="h-4 w-4 text-teal-100" />
      )}
    </div>
  );
}

export default UserAvatar;
