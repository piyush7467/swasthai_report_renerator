import * as React from "react";
import { cn } from "@/lib/utils";
import emptyStatsImg from "@/assets/emptystats.png";
import { Button } from "@/components/ui/button";
import type { LucideIcon } from "lucide-react";

export interface EmptyStateAction {
  label: string;
  onClick?: () => void;
  icon?: LucideIcon | React.ComponentType<{ className?: string }>;
  disabled?: boolean;
  variant?: "default" | "outline" | "secondary" | "ghost" | "link" | "destructive";
  className?: string;
}

export interface EmptyStateProps {
  /** Main heading */
  title: string;
  /** Contextual descriptive subtitle or explanation */
  description?: React.ReactNode;
  /** Optional badge or status tag above the heading */
  badge?: React.ReactNode;
  /** Primary call-to-action button or custom React node */
  action?: EmptyStateAction | React.ReactNode;
  /** Optional secondary action (e.g. Clear Filters) */
  secondaryAction?: EmptyStateAction | React.ReactNode;
  /** Custom image source (defaults to src/assets/emptystats.png) */
  imageSrc?: string;
  /** Image alt text */
  imageAlt?: string;
  /** Custom wrapper CSS classes */
  className?: string;
  /** Custom image CSS classes */
  imageClassName?: string;
  /** Use more compact sizing for modals, profile tabs, or nested cards */
  compact?: boolean;
  /** Additional custom children content below actions */
  children?: React.ReactNode;
}

function isActionObject(
  action: EmptyStateAction | React.ReactNode,
): action is EmptyStateAction {
  return (
    typeof action === "object" &&
    action !== null &&
    "label" in action &&
    typeof (action as EmptyStateAction).label === "string"
  );
}

export function EmptyState({
  title,
  description,
  badge,
  action,
  secondaryAction,
  imageSrc = emptyStatsImg,
  imageAlt,
  className,
  imageClassName,
  compact = false,
  children,
}: EmptyStateProps) {
  const renderAction = (
    act: EmptyStateAction | React.ReactNode,
    isPrimary = true,
  ) => {
    if (!act) return null;

    if (React.isValidElement(act)) {
      return act;
    }

    if (isActionObject(act)) {
      const Icon = act.icon;
      const defaultVariant = isPrimary ? "default" : "outline";
      const defaultPrimaryClass =
        "bg-[#0F766E] hover:bg-[#115E59] text-white font-semibold shadow-xs text-xs h-9 px-4 rounded-lg cursor-pointer";
      const defaultSecondaryClass =
        "border-slate-200 text-slate-700 hover:bg-slate-100/80 text-xs h-9 px-3.5 rounded-lg cursor-pointer";

      return (
        <Button
          type="button"
          variant={act.variant || defaultVariant}
          onClick={act.onClick}
          disabled={act.disabled}
          className={cn(
            isPrimary && !act.variant && !act.className
              ? defaultPrimaryClass
              : !isPrimary && !act.variant && !act.className
                ? defaultSecondaryClass
                : "",
            act.className,
          )}
        >
          {Icon && <Icon className="mr-1.5 h-3.5 w-3.5 shrink-0" />}
          {act.label}
        </Button>
      );
    }

    return null;
  };

  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center text-center mx-auto w-full animate-in fade-in duration-200",
        compact ? "py-6 sm:py-8 px-3" : "py-10 sm:py-14 px-4",
        className,
      )}
    >
      {/* Contextual Empty State Illustration */}
      <div
        className={cn(
          "relative mb-4 flex items-center justify-center w-full select-none",
          compact
            ? "max-w-[190px] sm:max-w-[220px]"
            : "max-w-[260px] sm:max-w-[320px] md:max-w-[350px]",
        )}
      >
        <img
          src={imageSrc}
          alt={imageAlt || title}
          className={cn(
            "w-full h-auto object-contain transition-transform duration-300 hover:scale-[1.02]",
            compact ? "max-h-40" : "max-h-56 sm:max-h-64",
            imageClassName,
          )}
          loading="lazy"
          decoding="async"
        />
      </div>

      {/* Optional Badge */}
      {badge && <div className="mb-2">{badge}</div>}

      {/* Heading */}
      <h3
        className={cn(
          "font-bold text-slate-900 tracking-tight",
          compact ? "text-sm sm:text-base" : "text-base sm:text-lg",
        )}
      >
        {title}
      </h3>

      {/* Contextual Description */}
      {description && (
        <div
          className={cn(
            "text-slate-500 leading-relaxed mx-auto mt-1",
            compact ? "text-xs max-w-sm" : "text-xs sm:text-sm max-w-md",
          )}
        >
          {typeof description === "string" ? (
            <p>{description}</p>
          ) : (
            description
          )}
        </div>
      )}

      {/* Action Buttons */}
      {(action || secondaryAction) && (
        <div className="mt-5 flex flex-wrap items-center justify-center gap-2.5">
          {renderAction(action, true)}
          {renderAction(secondaryAction, false)}
        </div>
      )}

      {/* Custom Children */}
      {children && <div className="mt-4 w-full">{children}</div>}
    </div>
  );
}
