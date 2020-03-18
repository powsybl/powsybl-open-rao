/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ResultVariantManagerTest {

    private Crac crac;
    private ResultVariantManager variantManager;

    @Before
    public void setUp() {
        crac = CracImporters.importCrac("small-crac-without-extension.json", getClass().getResourceAsStream("/small-crac-without-extension.json"));
        variantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, variantManager);
    }

    @Test
    public void testOk() {

        String variantId1 = "variant1";
        String variantId2 = "variant2";

        CracResultExtension cracExtension;
        CnecResultExtension cnecExtension;
        PstRangeResultExtension pstExtension;

        assertTrue(variantManager.getVariants().isEmpty());

        // add 2 variants
        variantManager.createVariant(variantId1);
        variantManager.createVariant(variantId2);

        assertEquals(2, variantManager.getVariants().size());

        cracExtension = crac.getExtension(CracResultExtension.class);
        cnecExtension = crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResultExtension.class);
        pstExtension = (PstRangeResultExtension) crac.getRangeAction("PRA_PST_BE").getExtension(PstRangeResultExtension.class);

        assertNotNull(cracExtension.getVariant(variantId1));
        assertNotNull(cracExtension.getVariant(variantId2));
        assertNotNull(cnecExtension.getVariant(variantId1));
        assertNotNull(cnecExtension.getVariant(variantId2));
        assertNotNull(pstExtension.getVariant(variantId1));
        assertNotNull(pstExtension.getVariant(variantId2));

        // delete one variant
        variantManager.deleteVariant(variantId2);

        assertEquals(1, variantManager.getVariants().size());

        assertNotNull(cracExtension.getVariant(variantId1));
        assertNull(cracExtension.getVariant(variantId2));
        assertNotNull(cnecExtension.getVariant(variantId1));
        assertNull(cnecExtension.getVariant(variantId2));
        assertNotNull(pstExtension.getVariant(variantId1));
        assertNull(pstExtension.getVariant(variantId2));

        // delete the other variant

        variantManager.deleteVariant(variantId1);

        assertEquals(0, variantManager.getVariants().size());

        cracExtension = crac.getExtension(CracResultExtension.class);
        cnecExtension = crac.getCnec("Tieline BE FR - Défaut - N-1 NL1-NL3").getExtension(CnecResultExtension.class);
        pstExtension = (PstRangeResultExtension) crac.getRangeAction("PRA_PST_BE").getExtension(PstRangeResultExtension.class);

        assertNull(cracExtension);
        assertNull(cnecExtension);
        assertNull(pstExtension);
    }

    @Test
    public void addAlreadyExistingVariantTest() {
        try {
            variantManager.createVariant("variant1");
            variantManager.createVariant("variant1");
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void deleteNonExistingVariant() {
        try {
            variantManager.deleteVariant("variant1");
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
