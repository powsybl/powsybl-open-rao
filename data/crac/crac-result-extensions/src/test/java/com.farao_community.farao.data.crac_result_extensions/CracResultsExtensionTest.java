/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracResultsExtensionTest {

    @Test
    public void testVariantManagementOk() {
        CracResultsExtension cracResultsExtension = new CracResultsExtension();

        // test variant addition
        cracResultsExtension.addVariant("variant-before-opt", new CracResult());
        cracResultsExtension.addVariant("variant-after-opt", new CracResult());

        assertNotNull(cracResultsExtension.getVariant("variant-before-opt"));
        assertNotNull(cracResultsExtension.getVariant("variant-after-opt"));
        assertNull(cracResultsExtension.getVariant("variant-not-created"));

        // test variant deletion
        cracResultsExtension.deleteVariant("variant-before-opt");
        assertNull(cracResultsExtension.getVariant("variant-before-opt"));
    }

    @Test
    public void getName() {
        CracResultsExtension cracResultsExtension = new CracResultsExtension();
        assertEquals("CracResultsExtension", cracResultsExtension.getName());
    }
}
