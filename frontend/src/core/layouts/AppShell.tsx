import { useState } from "react";
import { Outlet } from "react-router-dom";

import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "@/components/ui/sheet";

import { AppSidebar } from "./AppSidebar";
import { AppTopbar } from "./AppTopbar";

export function AppShell() {
  const [mobileNavigationOpen, setMobileNavigationOpen] =
    useState(false);

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="flex min-h-screen">
        {/* Desktop sidebar */}
        <div className="hidden lg:block">
          <AppSidebar />
        </div>

        {/* Mobile sidebar */}
        <Sheet
          open={mobileNavigationOpen}
          onOpenChange={setMobileNavigationOpen}
        >
          <SheetContent
            side="left"
            className="w-72 p-0"
          >
            <SheetTitle className="sr-only">
              Navigation
            </SheetTitle>

            <AppSidebar
              onNavigate={() =>
                setMobileNavigationOpen(false)
              }
            />
          </SheetContent>
        </Sheet>

        {/* Main application */}
        <div className="flex min-w-0 flex-1 flex-col">
          <AppTopbar
            onMenuClick={() =>
              setMobileNavigationOpen(true)
            }
          />

          <main className="flex-1 p-4 sm:p-6 lg:p-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}