/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards <philippe.edwards at rte-france.com>
 */
public class RaoResultTest {

    RaoResult raoResult;

    @Before
    public void setUp() {
        raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId("preOptimVariant");
        raoResult.setPostOptimVariantId("postOptimVariant");
    }

    @Test
    public void testGetters() {
        assertTrue(raoResult.isSuccessful());
        assertEquals("preOptimVariant", raoResult.getPreOptimVariantId());
        assertEquals("postOptimVariant", raoResult.getPostOptimVariantId());
    }

}
