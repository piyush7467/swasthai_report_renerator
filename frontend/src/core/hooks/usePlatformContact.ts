import { useQuery } from "@tanstack/react-query";
import {
  platformContactApi,
  DEFAULT_CONTACT_SETTINGS,
  type PlatformContactSetting,
} from "../api/platformContactApi";

export function usePlatformContact(): PlatformContactSetting {
  const { data } = useQuery({
    queryKey: ["platform-public-contact"],
    queryFn: platformContactApi.getPublicContactInfo,
    staleTime: 1000 * 60 * 15, // 15 mins
    retry: 1,
  });

  return data || DEFAULT_CONTACT_SETTINGS;
}

