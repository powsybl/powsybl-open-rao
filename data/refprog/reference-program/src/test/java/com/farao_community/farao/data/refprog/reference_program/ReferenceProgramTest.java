/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.network.Country;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ReferenceProgramTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private ReferenceProgramArea referenceProgramAreaFrance;
    private ReferenceProgramArea referenceProgramAreaBelgium;
    private ReferenceProgramArea referenceProgramAreaDk;
    private ReferenceProgramArea referenceProgramAreaSk;
    private ReferenceProgramArea referenceProgramAreaGermany;

    @Before
    public void setUp() throws Exception {
        referenceProgramAreaFrance = new ReferenceProgramArea(Country.FR);
        referenceProgramAreaBelgium = new ReferenceProgramArea(Country.BE);
        referenceProgramAreaDk = new ReferenceProgramArea(Country.DK);
        referenceProgramAreaSk = new ReferenceProgramArea(Country.SK);
        referenceProgramAreaGermany = new ReferenceProgramArea(Country.DE);
    }

    @Test
    public void testGlobalNetPositionMap() {
        List<ReferenceExchangeData> list = Arrays.asList(
                new ReferenceExchangeData(referenceProgramAreaFrance, referenceProgramAreaBelgium, 100),
                new ReferenceExchangeData(referenceProgramAreaFrance, referenceProgramAreaGermany, -250),
                new ReferenceExchangeData(referenceProgramAreaDk, referenceProgramAreaSk, 100));
        ReferenceProgram referenceProgram = new ReferenceProgram(list);
        Map<ReferenceProgramArea, Double> netPositions = referenceProgram.getAllGlobalNetPositions();
        assertEquals(5, referenceProgram.getListOfAreas().size());
        assertEquals(5, netPositions.size());
        assertEquals(-150, netPositions.get(referenceProgramAreaFrance), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(referenceProgramAreaBelgium), DOUBLE_TOLERANCE);
        assertEquals(250, netPositions.get(referenceProgramAreaGermany), DOUBLE_TOLERANCE);
        assertEquals(100, netPositions.get(referenceProgramAreaDk), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(referenceProgramAreaSk), DOUBLE_TOLERANCE);
    }
}
