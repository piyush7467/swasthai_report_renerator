package com.swasthai.report_generator.test.masterdata.loader;

import com.swasthai.report_generator.test.masterdata.dto.MasterCategorySeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterParameterSeedDto;
import com.swasthai.report_generator.test.masterdata.dto.MasterTestSeedDto;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

/**
 * Container holding the parsed master data items loaded from JSON resources.
 */
@Builder
public record MasterDataPayload(
        List<MasterCategorySeedDto> categories,
        List<MasterTestSeedDto> tests,
        List<MasterParameterSeedDto> parameters
) {
    public static MasterDataPayload empty() {
        return new MasterDataPayload(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
