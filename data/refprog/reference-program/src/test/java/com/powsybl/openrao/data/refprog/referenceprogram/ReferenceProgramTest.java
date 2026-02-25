/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.EICode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ReferenceProgramTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private EICode eiCodeFrance;
    private EICode eiCodeBelgium;
    private EICode eiCodeDk;
    private EICode eiCodeSk;
    private EICode eiCodeGermany;

    @BeforeEach
    public void setUp() {
        eiCodeFrance = new EICode(Country.FR);
        eiCodeBelgium = new EICode(Country.BE);
        eiCodeDk = new EICode(Country.DK);
        eiCodeSk = new EICode(Country.SK);
        eiCodeGermany = new EICode(Country.DE);
    }

    @Test
    void testGlobalNetPositionMap() {
        List<ReferenceExchangeData> list = Arrays.asList(
                new ReferenceExchangeData(eiCodeFrance, eiCodeBelgium, 100),
                new ReferenceExchangeData(eiCodeFrance, eiCodeGermany, -250),
                new ReferenceExchangeData(eiCodeDk, eiCodeSk, 100));
        ReferenceProgram referenceProgram = new ReferenceProgram(list);
        Map<EICode, Double> netPositions = referenceProgram.getAllGlobalNetPositions();
        assertEquals(5, referenceProgram.getListOfAreas().size());
        assertEquals(5, netPositions.size());
        assertEquals(-150, netPositions.get(eiCodeFrance), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(eiCodeBelgium), DOUBLE_TOLERANCE);
        assertEquals(250, netPositions.get(eiCodeGermany), DOUBLE_TOLERANCE);
        assertEquals(100, netPositions.get(eiCodeDk), DOUBLE_TOLERANCE);
        assertEquals(-100, netPositions.get(eiCodeSk), DOUBLE_TOLERANCE);
    }
}
