/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteBranchHelperTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;
    private UcteNetworkHelper networkHelper;

    private void setUp(String networkFile) {
        network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/" + networkFile));
        networkHelper = new UcteNetworkHelper(network, new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));
    }

    @Test
    public void testValidInternalBranch() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");
        // internal branch with order code, from/to same as network
        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR1AA1 ", "FFR3AA1 ", "2", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to same as network
        branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteBranchHelper("FFR1AA1 ", "FFR3AA1 ", null, "BR FR1FR3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        // internal branch with order code, from/to different from network
        branchHelper = new UcteBranchHelper("BBE2AA2 ", "BBE1AA2 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA2  BBE2AA2  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR3AA1 ", "FFR1AA1 ", "2", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        // internal branch with element name, from/to different from network
        branchHelper = new UcteBranchHelper("BBE3AA1 ", "BBE1AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteBranchHelper("FFR3AA1 ", "FFR1AA1 ", null, "BR FR1FR3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
    }

    @Test
    public void testInvalidInternalBranch() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // unknown from
        assertFalse(new UcteBranchHelper("UNKNOW1 ", "BBE1AA1", "1", null, networkHelper).isBranchValid());

        // unknown to
        assertFalse(new UcteBranchHelper("BBE3AA1 ", "UNKNOW1 ", "1", null, networkHelper).isBranchValid());

        // branch exists but not with this order code
        assertFalse(new UcteBranchHelper("BBE1AA1 ", "BBE2AA1 ", "4", null, networkHelper).isBranchValid());

        // branch exists but not with this element name
        assertFalse(new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", null, "COUCOU", networkHelper).isBranchValid());

    }

    @Test
    public void testValidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /* note that transformers from/to node are systematically inverted by PowSyBl UCTE importer
        For instance, the transformer with id "UCTNODE1 UCTNODE2 1" have :
            - terminal1 = UCTNODE2
            - terminal2 = UCTNODE1
        That's why the branch is inverted when from/to is aligned with what is defined in the UCTE file, and vice versa.
        This is note the case for the other type of Branch, where terminal 1 and 2 match the id.
         */

        // transformer with order code, from/to same as network
        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE2AA1 ", "BBE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR1AA2 ", "FFR1AA1 ", "5", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name, from/to same as network
        branchHelper = new UcteBranchHelper("BBE2AA1 ", "BBE3AA1 ", null, "PST BE", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE1AA2 ", null, "TR BE1", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE1AA2  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000. * 380 / 220, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with order code, from/to different from network
        branchHelper = new UcteBranchHelper("BBE3AA1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("BBE2AA1 ", "BBE2AA2 ", "2", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA2  BBE2AA1  2", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1200. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1200., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name, from/to different from network
        branchHelper = new UcteBranchHelper("BBE3AA1 ", "BBE2AA1 ", null, "PST BE", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR1AA1 ", "FFR1AA2 ", null, "TR FR1", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    public void testInvalidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // transformer exists but not with this order code
        assertFalse(new UcteBranchHelper("BBE2AA1 ", "BBE3AA1 ", "2", null, networkHelper).isBranchValid());

        // transformer exists but not with this element name
        assertFalse(new UcteBranchHelper("FFR1AA2 ", "FFR1AA1 ", null, "COUCOU", networkHelper).isBranchValid());
    }

    @Test
    public void testValidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // tie-line with order code
        UcteBranchHelper branchHelper = new UcteBranchHelper("XFRDE11 ", "DDE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.ONE, branchHelper.getTieLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4800., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4400., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("DDE3AA1 ", "XFRDE11 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.ONE, branchHelper.getTieLineSide());

        branchHelper = new UcteBranchHelper("XFRDE11 ", "FFR2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.TWO, branchHelper.getTieLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4800., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4400., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR2AA1 ", "XFRDE11 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.TWO, branchHelper.getTieLineSide());

        // tie-line with element name
        branchHelper = new UcteBranchHelper("NNL2AA1 ", "XNLBE11 ", null, "TL NL2X", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.ONE, branchHelper.getTieLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(3200., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(2800., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("XNLBE11 ", "NNL2AA1 ", null, "TL NL2X", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.ONE, branchHelper.getTieLineSide());

        branchHelper = new UcteBranchHelper("XNLBE11 ", "BBE3AA1 ", null, "TL BE3X", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.TWO, branchHelper.getTieLineSide());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(3200., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(2800., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("BBE3AA1 ", "XNLBE11 ", null, "TL BE3X", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertTrue(branchHelper.isTieLine());
        assertEquals(Branch.Side.TWO, branchHelper.getTieLineSide());
    }

    @Test
    public void testInvalidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // tie-line exists but not with this order code
        assertFalse(new UcteBranchHelper("XFRDE11 ", "FFR2AA1 ", "7", null, networkHelper).isBranchValid());

        // tie-line exists but not with this element name
        assertFalse(new UcteBranchHelper("NNL2AA1 ", "XNLBE11 ", null, "COUCOU", networkHelper).isBranchValid());
    }

    @Test
    public void testValidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // dangling-line with order code
        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE2AA1 ", "XBE2AL1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("XBE2AL1 ", "BBE2AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());

        // dangling-line with element name
        branchHelper = new UcteBranchHelper("XDE2AL1 ", "DDE2AA1 ", null, "DL AL", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("DDE2AA1 ", "XDE2AL1 ", null, "DL AL", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
    }

    @Test
    public void testInvalidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // dangling-line exists but not with this order code
        assertFalse(new UcteBranchHelper("XBE2AL1 ", "BBE2AA1 ", "2", null, networkHelper).isBranchValid());

        // dangling-line exists but not with this element name
        assertFalse(new UcteBranchHelper("DDE2AA1 ", "XDE2AL1 ", null, "COUCOU", networkHelper).isBranchValid());
    }

    @Test
    public void testWithSevenCharacters() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /*
        if the from/to node contains less that 8 characters, the missing characters will be
        replaced by blank spaces at the end of the missing UCTE node id
         */

        // internal branch with order code, 7 characters in from and to
        UcteBranchHelper branchReader = new UcteBranchHelper("BBE1AA1", "BBE2AA1", "1", null, networkHelper);
        assertTrue(branchReader.isBranchValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchReader.getBranchIdInNetwork());

        // tie-line with element name, 7 characters in to
        branchReader = new UcteBranchHelper("NNL2AA1 ", "XNLBE11", null, "TL NL2X", networkHelper);
        assertTrue(branchReader.isBranchValid());
        assertEquals("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1", branchReader.getBranchIdInNetwork());

        // transformer with order code, 7 characters in from
        branchReader = new UcteBranchHelper("FFR1AA2", "FFR1AA1 ", "5", null, networkHelper);
        assertTrue(branchReader.isBranchValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchReader.getBranchIdInNetwork());

        // dangling line with element name, 7 characters in from and to
        branchReader = new UcteBranchHelper("DDE2AA1", "XDE2AL1", null, "DL AL", networkHelper);
        assertTrue(branchReader.isBranchValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchReader.getBranchIdInNetwork());
    }

    @Test
    public void testOtherConstructors() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // element name
        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", null, "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // order code
        branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // suffix
        branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", "BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        branchHelper = new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", "1", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // id
        branchHelper = new UcteBranchHelper("BBE1AA1  BBE3AA1  BR BE1BE3", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals("BBE1AA1 ", branchHelper.getOriginalFrom());
        assertEquals("BBE3AA1 ", branchHelper.getOriginalTo());
        assertEquals("BR BE1BE3", branchHelper.getSuffix());

        branchHelper = new UcteBranchHelper("BBE1AA1  BBE3AA1  1", networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals("BBE1AA1 ", branchHelper.getOriginalFrom());
        assertEquals("BBE3AA1 ", branchHelper.getOriginalTo());
        assertEquals("1", branchHelper.getSuffix());
    }

    @Test
    public void testInvalidConstructor() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        // no from
        assertFalse(new UcteBranchHelper(null, "BBE3AA1 ", "1", networkHelper).isBranchValid());
        assertFalse(new UcteBranchHelper(null, "BBE3AA1 ", "1", null, networkHelper).isBranchValid());

        // no to
        assertFalse(new UcteBranchHelper("BBE1AA1 ", null, "1", networkHelper).isBranchValid());
        assertFalse(new UcteBranchHelper("BBE1AA1 ", null, "1", null, networkHelper).isBranchValid());

        // no order code, no element name
        assertFalse(new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", "", "", networkHelper).isBranchValid());
        assertFalse(new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", null, null, networkHelper).isBranchValid());
        assertFalse(new UcteBranchHelper("BBE1AA1 ", "BBE3AA1 ", null, networkHelper).isBranchValid());

        //no id
        assertFalse(new UcteBranchHelper(null, networkHelper).isBranchValid());
    }

    @Test
    public void testInvalidBranchIdConstructor() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct");

        /*
         The presence of 'NODE1ID_ NODE2_ID SUFFIX' in the invalid reason message is checked, as the messages
         related to problems in ids should contain id
         */

        // wrong size of node id or suffix
        UcteBranchHelper branchHelper = new UcteBranchHelper("7_CHARA 7_CHARA E_NAME", networkHelper);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        branchHelper = new UcteBranchHelper("9_CHARACT 9_CHARACT E_NAME", networkHelper);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        branchHelper = new UcteBranchHelper("8_CHARAC 8_CHARAC ELEMENT_NAME_WITH_MORE_THAN_12_CHARAC", networkHelper);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        // no suffix
        branchHelper = new UcteBranchHelper("8_CHARAC 8_CHARAC", networkHelper);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("NODE1ID_ NODE2_ID SUFFIX"));

        // no blank space between ids and suffix
        branchHelper = new UcteBranchHelper("8_CHARAC_8_CHARAC_1", networkHelper);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("NODE1ID_ NODE2_ID SUFFIX"));
    }

    @Test
    public void testValidBranchesWithWildCard() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        // internal branch with order code, from/to same as network
        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE1AA1*", "BBE2AA1*", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA11 BBE2AA11 1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());
        assertFalse(branchHelper.isTieLine());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new UcteBranchHelper("FFR3AA1*", "XBEFR11*", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR3AA11 XBEFR112 1 + XBEFR112 BBE2AA11 1", branchHelper.getBranchIdInNetwork());

        branchHelper = new UcteBranchHelper("XDENL11*", "DDE2AA1*", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("DDE2AA11 XDENL111 1 + NNL3AA11 XDENL111 1", branchHelper.getBranchIdInNetwork());
    }

    @Test
    public void testInvalidBranchesWithWildCard() {
        setUp("TestCase_severalVoltageLevels_Xnodes_8characters.uct");

        // multiple matches
        UcteBranchHelper branchHelper = new UcteBranchHelper("DDE1AA1*", "DDE2AA1*", "1", null, networkHelper);
        assertFalse(branchHelper.isBranchValid());
    }

    @Test
    public void testSwitch() {
        setUp("TestCase16Nodes_with_different_imax.uct");

        UcteBranchHelper branchHelper = new UcteBranchHelper("BBE1AA1", "BBE4AA1", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE4AA1  1", branchHelper.getBranchIdInNetwork());
        assertFalse(branchHelper.isInvertedInNetwork());

        branchHelper = new UcteBranchHelper("BBE4AA1", "BBE1AA1", "1", null, networkHelper);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE4AA1  1", branchHelper.getBranchIdInNetwork());
        assertTrue(branchHelper.isInvertedInNetwork());
    }
}
