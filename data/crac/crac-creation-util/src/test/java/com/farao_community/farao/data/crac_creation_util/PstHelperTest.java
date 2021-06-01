/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstHelperTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
        UcteAliasesCreation.createAliases(network);
    }

    @Test
    public void testInvalidPst() {
        PstHelper pstHelper = new PstHelper("BBE1AA1  BBE2AA1  1", network);
        assertFalse(pstHelper.isPstValid());
        assertTrue(pstHelper.getInvalidPstReason().contains("was not found in network"));
    }

    @Test
    public void testInvalidPst2() {
        PstHelper pstHelper = new PstHelper("FFR3AA1  FFR3AA2  1", network);
        assertFalse(pstHelper.isPstValid());
        assertTrue(pstHelper.getInvalidPstReason().contains("does not have a phase tap changer"));
    }

    @Test
    public void testValidPst() {
        PstHelper pstHelper = new PstHelper("BBE2AA1  BBE3AA1  1", network);

        assertTrue(pstHelper.isPstValid());
        assertNull(pstHelper.getInvalidPstReason());
        assertEquals(-16, pstHelper.getLowTapPosition());
        assertEquals(16, pstHelper.getHighTapPosition());
        assertEquals(0, pstHelper.getInitialTap());

        assertEquals(-5, pstHelper.normalizeTap(-5, PstHelper.TapConvention.CENTERED_ON_ZERO));
        assertEquals(9, pstHelper.normalizeTap(9, PstHelper.TapConvention.CENTERED_ON_ZERO));
        assertEquals(-16, pstHelper.normalizeTap(1, PstHelper.TapConvention.STARTS_AT_ONE));
        assertEquals(16, pstHelper.normalizeTap(33, PstHelper.TapConvention.STARTS_AT_ONE));
        assertEquals(0, pstHelper.normalizeTap(17, PstHelper.TapConvention.STARTS_AT_ONE));
        assertEquals(3, pstHelper.normalizeTap(20, PstHelper.TapConvention.STARTS_AT_ONE));

        Map<Integer, Double> conversionMap = pstHelper.getTapToAngleConversionMap();
        assertEquals(33, conversionMap.size());
        assertEquals(-6.228, conversionMap.get(-16), DOUBLE_TOLERANCE);
        assertEquals(-5.839, conversionMap.get(-15), DOUBLE_TOLERANCE);
        assertEquals(-5.450, conversionMap.get(-14), DOUBLE_TOLERANCE);
        assertEquals(-5.062, conversionMap.get(-13), DOUBLE_TOLERANCE);
        assertEquals(-4.673, conversionMap.get(-12), DOUBLE_TOLERANCE);
        assertEquals(-4.284, conversionMap.get(-11), DOUBLE_TOLERANCE);
        assertEquals(-3.895, conversionMap.get(-10), DOUBLE_TOLERANCE);
        assertEquals(-3.505, conversionMap.get(-9), DOUBLE_TOLERANCE);
        assertEquals(-3.116, conversionMap.get(-8), DOUBLE_TOLERANCE);
        assertEquals(-2.727, conversionMap.get(-7), DOUBLE_TOLERANCE);
        assertEquals(-2.337, conversionMap.get(-6), DOUBLE_TOLERANCE);
        assertEquals(-1.948, conversionMap.get(-5), DOUBLE_TOLERANCE);
        assertEquals(-1.558, conversionMap.get(-4), DOUBLE_TOLERANCE);
        assertEquals(-1.169, conversionMap.get(-3), DOUBLE_TOLERANCE);
        assertEquals(-0.779, conversionMap.get(-2), DOUBLE_TOLERANCE);
        assertEquals(-0.390, conversionMap.get(-1), DOUBLE_TOLERANCE);
        assertEquals(0.000, conversionMap.get(0), DOUBLE_TOLERANCE);
        assertEquals(0.390, conversionMap.get(1), DOUBLE_TOLERANCE);
        assertEquals(0.779, conversionMap.get(2), DOUBLE_TOLERANCE);
        assertEquals(1.169, conversionMap.get(3), DOUBLE_TOLERANCE);
        assertEquals(1.558, conversionMap.get(4), DOUBLE_TOLERANCE);
        assertEquals(1.948, conversionMap.get(5), DOUBLE_TOLERANCE);
        assertEquals(2.337, conversionMap.get(6), DOUBLE_TOLERANCE);
        assertEquals(2.727, conversionMap.get(7), DOUBLE_TOLERANCE);
        assertEquals(3.116, conversionMap.get(8), DOUBLE_TOLERANCE);
        assertEquals(3.505, conversionMap.get(9), DOUBLE_TOLERANCE);
        assertEquals(3.895, conversionMap.get(10), DOUBLE_TOLERANCE);
        assertEquals(4.284, conversionMap.get(11), DOUBLE_TOLERANCE);
        assertEquals(4.673, conversionMap.get(12), DOUBLE_TOLERANCE);
        assertEquals(5.062, conversionMap.get(13), DOUBLE_TOLERANCE);
        assertEquals(5.450, conversionMap.get(14), DOUBLE_TOLERANCE);
        assertEquals(5.839, conversionMap.get(15), DOUBLE_TOLERANCE);
        assertEquals(6.228, conversionMap.get(16), DOUBLE_TOLERANCE);
    }
}
