/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.iidm;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class IidmCnecHelperTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
    }

    @Test
    public void testValidBranch() {
        // internal branch
        IidmCnecHelper cnecHelper = new IidmCnecHelper("BBE1AA1  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE1AA1  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertFalse(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());

        // tie-line with full id
        cnecHelper = new IidmCnecHelper("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertFalse(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());
    }

    @Test
    public void testInvalidInternalBranch() {
        // unknown from
        assertFalse(new IidmCnecHelper("UNKNOW1 BBE1AA1 1", network).isValid());

        // unknown to
        assertFalse(new IidmCnecHelper("BBE3AA1 UNKNOW1 1", network).isValid());

        // branch exists but not with this order code
        assertFalse(new IidmCnecHelper("BBE1AA1  BBE2AA1  4", network).isValid());
    }

    @Test
    public void testValidTransformer() {

        /* note that transformers from/to node are systematically inverted by PowSyBl UCTE importer
        For instance, the transformer with id "UCTNODE1 UCTNODE2 1" have :
            - terminal1 = UCTNODE2
            - terminal2 = UCTNODE1
        That's why the branch is inverted when from/to is aligned with what is defined in the UCTE file, and vice versa.
        This is note the case for the other type of Branch, where terminal 1 and 2 match the id.
         */

        // transformer with order code, from/to same as network
        IidmCnecHelper cnecHelper = new IidmCnecHelper("BBE2AA1  BBE3AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  BBE3AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecHelper("FFR1AA2  FFR1AA1  5", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR1AA2  FFR1AA1  5", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecHelper("BBE2AA2  BBE2AA1  2", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA2  BBE2AA1  2", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1200. * 220 / 380, cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1200., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    public void testInvalidTransformer() {

        // transformer exists but not with this order code
        assertFalse(new IidmCnecHelper("BBE2AA1  BBE3AA1  2", network).isValid());

        // transformer exists but not with this element name
        assertFalse(new IidmCnecHelper("FFR1AA2  FFR1AA1  COUCOU", network).isValid());
    }

    @Test
    public void testValidDanglingLine() {

        // dangling-line with order code
        IidmCnecHelper cnecHelper = new IidmCnecHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1250, cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1250, cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        cnecHelper = new IidmCnecHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("BBE2AA1  XBE2AL1  1", cnecHelper.getIdInNetwork());
    }

    @Test
    public void testInvalidDanglingLine() {
        // dangling-line exists but not with this order code
        assertFalse(new IidmCnecHelper("XBE2AL1  BBE2AA1  2", network).isValid());
    }

    @Test
    public void testValidHalfLine() {

        // if half-line is put in argument, the associated tie-line is recognized
        IidmCnecHelper cnecHelper = new IidmCnecHelper("FFR3AA1  XBEFR11  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertTrue(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());

        cnecHelper = new IidmCnecHelper("XBEFR11  BBE2AA1  1", network);
        assertTrue(cnecHelper.isValid());
        assertEquals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1", cnecHelper.getIdInNetwork());
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., cnecHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., cnecHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertTrue(cnecHelper.isHalfLine());
        assertFalse(cnecHelper.isInvertedInNetwork());
    }

    @Test
    public void testInvalidConstructor() {
        assertFalse(new IidmCnecHelper(null, network).isValid());
    }
}
