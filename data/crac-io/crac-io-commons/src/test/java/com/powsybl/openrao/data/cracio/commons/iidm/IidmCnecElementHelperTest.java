/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.iidm;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class IidmCnecElementHelperTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
    }

    @Test
    void testValidBranch() {
        // internal branch
        IidmCnecElementHelper cnecHelper = new IidmCnecElementHelper("BBE1AA1  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE1AA1  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertFalse(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());

        // tie-line with full id
        cnecHelper = new IidmCnecElementHelper("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertFalse(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());
    }

    @Test
    void testInvalidInternalBranch() {
        // unknown from
        assertFalse(new IidmCnecElementHelper("UNKNOW1 BBE1AA1 1", network).isValid());

        // unknown to
        assertFalse(new IidmCnecElementHelper("BBE3AA1 UNKNOW1 1", network).isValid());

        // branch exists but not with this order code
        assertFalse(new IidmCnecElementHelper("BBE1AA1  BBE2AA1  4", network).isValid());
    }

    @Test
    void testValidTransformer() {

        /* note that transformers from/to node are systematically inverted by PowSyBl UCTE importer
        For instance, the transformer with id "UCTNODE1 UCTNODE2 1" have :
            - terminal1 = UCTNODE2
            - terminal2 = UCTNODE1
        That's why the branch is inverted when from/to is aligned with what is defined in the UCTE file, and vice versa.
        This is note the case for the other type of Branch, where terminal 1 and 2 match the id.
         */

        // transformer with order code, from/to same as network
        IidmCnecElementHelper cnecHelper = new IidmCnecElementHelper("BBE2AA1  BBE3AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecElementHelper("FFR1AA2  FFR1AA1  5", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR1AA2  FFR1AA1  5", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecElementHelper("BBE2AA2  BBE2AA1  2", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA2  BBE2AA1  2", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(1200. * 220 / 380, cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(1200., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void testInvalidTransformer() {

        // transformer exists but not with this order code
        assertFalse(new IidmCnecElementHelper("BBE2AA1  BBE3AA1  2", network).isValid());

        // transformer exists but not with this element name
        assertFalse(new IidmCnecElementHelper("FFR1AA2  FFR1AA1  COUCOU", network).isValid());
    }

    @Test
    void testValidDanglingLine() {

        // dangling-line with order code
        IidmCnecElementHelper cnecHelper = new IidmCnecElementHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(1250, cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(1250, cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecElementHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", cnecHelper.getIdInNetwork());
    }

    @Test
    void testInvalidDanglingLine() {
        // dangling-line exists but not with this order code
        assertFalse(new IidmCnecElementHelper("XBE2AL1  BBE2AA1  2", network).isValid());
    }

    @Test
    void testValidHalfLine() {

        // if half-line is put in argument, the associated tie-line is recognized
        IidmCnecElementHelper cnecHelper = new IidmCnecElementHelper("FFR3AA1  XBEFR11  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertTrue(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());

        cnecHelper = new IidmCnecElementHelper("XBEFR11  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(TwoSides.TWO), DOUBLE_TOLERANCE);
        assertTrue(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());
    }

    @Test
    void testInvalidConstructor() {
        assertFalse(new IidmCnecElementHelper(null, network).isValid());
    }
}
