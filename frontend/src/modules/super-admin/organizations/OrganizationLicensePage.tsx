import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import { OrganizationHeaderNav } from "../components/OrganizationHeaderNav";
import { useOrganizationQuery } from "../hooks/useOrganizations";
import { useOrganizationLicenseQuery } from "../licensing/hooks/useLicensing";
import { OrganizationLicenseCard } from "../licensing/components/OrganizationLicenseCard";
import { ActivateLicenseDialog } from "../licensing/components/ActivateLicenseDialog";
import { RenewLicenseDialog } from "../licensing/components/RenewLicenseDialog";

export function OrganizationLicensePage() {
  const params = useParams<{ refId?: string; orgRefId?: string }>();
  const refId = params.refId || params.orgRefId || "";
  const navigate = useNavigate();

  const [activateDialogOpen, setActivateDialogOpen] = useState(false);
  const [renewDialogOpen, setRenewDialogOpen] = useState(false);

  const organizationQuery = useOrganizationQuery(refId);
  const licenseQuery = useOrganizationLicenseQuery(refId);

  const organization = organizationQuery.data;
  const license = licenseQuery.data;

  if (!refId) {
    return (
      <div className="p-6">
        <Alert variant="destructive">
          <AlertDescription>No organization identifier provided.</AlertDescription>
        </Alert>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => navigate("/super-admin/organizations")}
        >
          <ArrowLeft className="mr-1.5 h-4 w-4" />
          Back to Organizations
        </Button>
      </div>
    );
  }

  const handleRefresh = () => {
    void organizationQuery.refetch();
    void licenseQuery.refetch();
  };

  return (
    <div className="space-y-6">
      {/* Context & Navigation Header with Tabs */}
      <OrganizationHeaderNav
        organization={organization}
        isLoading={organizationQuery.isLoading}
        onRefresh={handleRefresh}
        isRefreshing={organizationQuery.isFetching || licenseQuery.isFetching}
      />

      {/* Organization License Management Card */}
      <OrganizationLicenseCard
        license={license ?? null}
        organizationRefId={refId}
        organizationName={organization?.name}
        isLoading={organizationQuery.isLoading || licenseQuery.isLoading}
        onActivateClick={() => setActivateDialogOpen(true)}
        onRenewClick={() => setRenewDialogOpen(true)}
        onRefresh={handleRefresh}
      />

      {/* Activate License Dialog */}
      <ActivateLicenseDialog
        organizationRefId={refId}
        organizationName={organization?.name}
        open={activateDialogOpen}
        onOpenChange={setActivateDialogOpen}
        onSuccess={() => {
          void licenseQuery.refetch();
        }}
      />

      {/* Renew License Dialog */}
      {license && (
        <RenewLicenseDialog
          organizationRefId={refId}
          organizationName={organization?.name}
          currentLicense={license}
          open={renewDialogOpen}
          onOpenChange={setRenewDialogOpen}
          onSuccess={() => {
            void licenseQuery.refetch();
          }}
        />
      )}
    </div>
  );
}

export default OrganizationLicensePage;
