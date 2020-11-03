/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class JsonRaoResultTest extends AbstractConverterTest {

    RaoResult raoResult;

    @Before
    public void setUp() throws IOException {
        raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId("variant1");
        raoResult.setPostOptimVariantIdPerStateId(Map.of("preventive", "variant2"));
        super.setUp();
    }

    @Test
    public void roundTrip() throws IOException {
        roundTripTest(raoResult, JsonRaoResult::write, JsonRaoResult::read, "/RaoResult.json");
    }

    @Test
    public void writeExtension() throws IOException {
        raoResult.setStatus(RaoResult.Status.FAILURE);
        raoResult.setPreOptimVariantId("var1");
        raoResult.setPostOptimVariantIdPerStateId(Map.of("preventive", "var2"));
        writeTest(raoResult, JsonRaoResult::write, AbstractConverterTest::compareTxt, "/RaoResultFailure.json");
    }

    @Test
    public void readExtension() throws IOException {
        RaoResult raoResult = JsonRaoResult.read(getClass().getResourceAsStream("/RaoResultFailure.json"));
        assertFalse(raoResult.isSuccessful());
        assertEquals("var1", raoResult.getPreOptimVariantId());
        assertNotNull("var2", raoResult.getPostOptimVariantIdPerStateId());
    }
}
