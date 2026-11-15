import { apiClient } from "./apiClient";

export interface PlatformContactSetting {
  supportEmail: string;
  privacyEmail: string;
  legalEmail: string;
  helplinePhone: string;
  whatsappNumber: string;
  operatingHours: string;
  officeAddress: string;
  updatedAt?: string;
}

export const DEFAULT_CONTACT_SETTINGS: PlatformContactSetting = {
  supportEmail: "support@swasthai.com",
  privacyEmail: "privacy@swasthai.com",
  legalEmail: "legal@swasthai.com",
  helplinePhone: "+91 99999 99999",
  whatsappNumber: "919999999999",
  operatingHours: "Monday to Saturday, 9:00 AM – 6:00 PM IST",
  officeAddress: "SwasthAI Healthcare Technologies Private Limited",
};

export const platformContactApi = {
  getPublicContactInfo: async (): Promise<PlatformContactSetting> => {
    try {
      const response = await apiClient.get<{ success: boolean; data: PlatformContactSetting }>(
        "/public/contact-info"
      );
      return response.data?.data || DEFAULT_CONTACT_SETTINGS;
    } catch {
      return DEFAULT_CONTACT_SETTINGS;
    }
  },

  getAdminContactInfo: async (): Promise<PlatformContactSetting> => {
    const response = await apiClient.get<{ success: boolean; data: PlatformContactSetting }>(
      "/admin/contact-info"
    );
    return response.data?.data || DEFAULT_CONTACT_SETTINGS;
  },

  updateAdminContactInfo: async (
    data: PlatformContactSetting
  ): Promise<PlatformContactSetting> => {
    const response = await apiClient.put<{ success: boolean; data: PlatformContactSetting }>(
      "/admin/contact-info",
      data
    );
    return response.data?.data;
  },
};

