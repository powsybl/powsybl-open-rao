/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgramTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    public void testGlobalNetPositionMap() {
        List<ReferenceExchangeData> list = Arrays.asList(
                new ReferenceExchangeData(Country.FR, Country.BE, 100),
                new ReferenceExchangeData(Country.FR, Country.DE, -250),
                new ReferenceExchangeData(Country.DK, Country.SK, 100));
        ReferenceProgram referenceProgram = new ReferenceProgram(list);
        Map<Country, Double> netPositions = referenceProgram.getAllGlobalNetPositions();
        assertEquals(5, referenceProgram.getListOfCountries().size());
        assertEquals(5, netPositions.size());
        assertEquals(-150, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(250, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        assertEquals(100, netPositions.get(Country.DK), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(Country.SK), DOUBLE_TOLERANCE);
    }
}
