/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ResultExtensionTest {

    @Test
    public void testCracResultExtension() {

        ResultExtension<Crac, CracResult> resultExtension = new ResultExtension<>();

        // test variant addition
        resultExtension.addVariant("variant-before-opt", new CracResult());
        resultExtension.addVariant("variant-after-opt", new CracResult());

        assertNotNull(resultExtension.getVariant("variant-before-opt"));
        assertNotNull(resultExtension.getVariant("variant-after-opt"));
        assertNull(resultExtension.getVariant("variant-not-created"));

        // test variant deletion
        resultExtension.deleteVariant("variant-before-opt");
        assertNull(resultExtension.getVariant("variant-before-opt"));

        // add extension to a Crac
        Crac crac = new SimpleCrac("cracId");
        crac.addExtension(ResultExtension.class, resultExtension);
        ResultExtension<Crac, CracResult> ext = crac.getExtension(ResultExtension.class);
        assertNotNull(ext);
    }

    @Test
    public void getName() {
        ResultExtension<Crac, CracResult> resultExtension = new ResultExtension<>();
        assertEquals("ResultExtension", resultExtension.getName());
    }
}
