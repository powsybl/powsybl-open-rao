/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracResultExtensionTest {

    @Test
    public void testCracResultExtension() {

        CracResultExtension cracResultExtension = new CracResultExtension();

        // test variant addition
        cracResultExtension.addVariant("variant-before-opt", new CracResult());
        cracResultExtension.addVariant("variant-after-opt", new CracResult());

        assertNotNull(cracResultExtension.getVariant("variant-before-opt"));
        assertNotNull(cracResultExtension.getVariant("variant-after-opt"));
        assertNull(cracResultExtension.getVariant("variant-not-created"));

        // test variant deletion
        cracResultExtension.deleteVariant("variant-before-opt");
        assertNull(cracResultExtension.getVariant("variant-before-opt"));

        // add extension to a Crac
        Crac crac = new SimpleCracFactory().create("cracId", Collections.emptySet());

        crac.addExtension(CracResultExtension.class, cracResultExtension);
        CracResultExtension ext = crac.getExtension(CracResultExtension.class);
        assertNotNull(ext);
    }

    @Test
    public void getName() {
        CracResultExtension cracResultExtension = new CracResultExtension();
        assertEquals("CracResultExtension", cracResultExtension.getName());
    }
}
