package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import org.junit.jupiter.api.Test;

import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getCsaCracCreationContext;

public class CsaProfileCracFilteringTest {

    @Test
    void testCracCreatorFiltersOutBadTimeStamps() {
        CsaProfileCracCreationContext cracCreationContext = getCsaCracCreationContext("/CSA_82_FilterBadTS.zip");
        cracCreationContext.getCrac();
        // todo: It doesn't work and I don't know why ...
    }
}
