package com.swasthai.report_generator.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeOrganizationLogo(
            String organizationRefId,
            MultipartFile file
    );

    String storeOrganizationSignature(
            String organizationRefId,
            MultipartFile file
    );

    byte[] load(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);
}