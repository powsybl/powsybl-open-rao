/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoInputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    public void testBuildWithoutRefProg() {
        RaoInput raoInput = new RaoInput.Builder().newRaoInput().build();
        assertFalse(raoInput.getReferenceProgram().isPresent());
    }

    @Test
    public void testBuildWithRefProg() {
        List<ReferenceExchangeData> referenceExchangeDataList = Arrays.asList(
                new ReferenceExchangeData(Country.FR, Country.BE, 100),
                new ReferenceExchangeData(Country.DE, Country.NL, -200));
        RaoInput raoInput = new RaoInput.Builder().newRaoInput()
                .withRefProg(new ReferenceProgram(referenceExchangeDataList))
                .build();
        assertTrue(raoInput.getReferenceProgram().isPresent());
        ReferenceProgram actualRefProg =  raoInput.getReferenceProgram().get();
        assertEquals(2, actualRefProg.getReferenceExchangeDataList().size());
        assertEquals(100, actualRefProg.getExchange(Country.FR, Country.BE), DOUBLE_TOLERANCE);
        assertEquals(-200, actualRefProg.getExchange(Country.DE, Country.NL), DOUBLE_TOLERANCE);
    }
}
