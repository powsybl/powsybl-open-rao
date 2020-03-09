/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultsExtensionTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testOk() {
        CnecResultsExtension cnecResultsExtension = new CnecResultsExtension();

        // test variant addition
        cnecResultsExtension.addVariant("variant-before-opt");
        cnecResultsExtension.addVariant("variant-after-opt", new CnecResult(75.0, 50.0));
        assertNotNull(cnecResultsExtension.getVariant("variant-before-opt"));
        assertNotNull(cnecResultsExtension.getVariant("variant-after-opt"));
        assertEquals(75.0, cnecResultsExtension.getVariant("variant-after-opt").getFlowInMW(), DOUBLE_TOLERANCE);
        assertNull(cnecResultsExtension.getVariant("variant-not-created"));

        // test variant deletion
        cnecResultsExtension.deleteVariant("variant-before-opt");
        assertNull(cnecResultsExtension.getVariant("variant-before-opt"));
    }
}
