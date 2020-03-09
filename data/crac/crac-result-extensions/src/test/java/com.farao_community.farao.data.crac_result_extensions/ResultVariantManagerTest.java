/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
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
        crac = new SimpleCrac("cracId");
        variantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, variantManager);
    }

    @Test
    public void testOk() {

        assertTrue(variantManager.getVariants().isEmpty());

        variantManager.createVariant("variant1");
        variantManager.createVariant("variant2");

        assertEquals(2, variantManager.getVariants().size());

        variantManager.deleteVariant("variant2");

        assertEquals(1, variantManager.getVariants().size());
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
