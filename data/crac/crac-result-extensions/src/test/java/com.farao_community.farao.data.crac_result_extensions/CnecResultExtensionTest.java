/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CnecResultExtensionTest {

    @Test
    public void testCnecResultExtension() {

        CnecResultExtension cnecResultExtension = new CnecResultExtension();

        // test variant addition
        cnecResultExtension.addVariant("variant-before-opt", new CnecResult());
        cnecResultExtension.addVariant("variant-after-opt", new CnecResult());

        assertNotNull(cnecResultExtension.getVariant("variant-before-opt"));
        assertNotNull(cnecResultExtension.getVariant("variant-after-opt"));
        assertNull(cnecResultExtension.getVariant("variant-not-created"));

        // test variant deletion
        cnecResultExtension.deleteVariant("variant-before-opt");
        assertNull(cnecResultExtension.getVariant("variant-before-opt"));
    }

    @Test
    public void getName() {
        CnecResultExtension cnecResultExtension = new CnecResultExtension();
        assertEquals("CnecResultExtension", cnecResultExtension.getName());
    }
}
