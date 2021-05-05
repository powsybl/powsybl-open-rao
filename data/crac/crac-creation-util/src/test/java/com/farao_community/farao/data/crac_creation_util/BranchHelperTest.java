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
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class BranchHelperTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
        UcteAliasesCreation.createAliases(network);
    }

    @Test
    public void testValidInternalBranch() {

        // internal branch with order code, from/to same as network
        BranchHelper branchHelper = new BranchHelper("BBE1AA1  BBE2AA1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE2AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(5000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("FFR1AA1  FFR3AA1  2", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());

        // internal branch with element name, from/to same as network
        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  BR BE1BE3", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        branchHelper = new BranchHelper("FFR1AA1  FFR3AA1  BR FR1FR3", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA1  FFR3AA1  2", branchHelper.getBranchIdInNetwork());

        // internal branch with order code, from/to different from network
        branchHelper = new BranchHelper("BBE2AA2  BBE1AA2  1", network);
        assertFalse(branchHelper.isBranchValid());

        branchHelper = new BranchHelper("FFR3AA1  FFR1AA1  2", network);
        assertFalse(branchHelper.isBranchValid());

        // internal branch with element name, from/to different from network
        branchHelper = new BranchHelper("BBE3AA1  BBE1AA1  BR BE1BE3", network);
        assertFalse(branchHelper.isBranchValid());

        branchHelper = new BranchHelper("FFR3AA1  FFR1AA1  BR FR1FR3", network);
        assertFalse(branchHelper.isBranchValid());
    }

    @Test
    public void testInvalidInternalBranch() {

        // unknown from
        assertFalse(new BranchHelper("UNKNOW1 BBE1AA1 1", network).isBranchValid());

        // unknown to
        assertFalse(new BranchHelper("BBE3AA1 UNKNOW1 1", network).isBranchValid());

        // branch exists but not with this order code
        assertFalse(new BranchHelper("BBE1AA1  BBE2AA1  4", network).isBranchValid());

        // branch exists but not with this element name
        assertFalse(new BranchHelper("BBE1AA1  BBE3AA1  COUCOU", network).isBranchValid());

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
        BranchHelper branchHelper = new BranchHelper("BBE2AA1  BBE3AA1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("FFR1AA2  FFR1AA1  5", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name, from/to same as network
        branchHelper = new BranchHelper("BBE2AA1  BBE3AA1  PST BE", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("BBE1AA1  BBE1AA2  TR BE1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE1AA2  1", branchHelper.getBranchIdInNetwork());
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1000. * 380 / 220, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1000., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("BBE2AA2  BBE2AA1  2", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA2  BBE2AA1  2", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1200. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1200., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        // transformer with element name
        branchHelper = new BranchHelper("BBE2AA1  BBE3AA1  PST BE", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(4500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("FFR1AA2  FFR1AA1  TR FR1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("FFR1AA2  FFR1AA1  5", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(220., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1500. * 220 / 380, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1500., branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);
    }

    @Test
    public void testInvalidTransformer() {

        // transformer exists but not with this order code
        assertFalse(new BranchHelper("BBE2AA1  BBE3AA1  2", network).isBranchValid());

        // transformer exists but not with this element name
        assertFalse(new BranchHelper("FFR1AA2  FFR1AA1  COUCOU", network).isBranchValid());
    }

    @Test
    public void testValidDanglingLine() {

        // dangling-line with order code
        BranchHelper branchHelper = new BranchHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1250, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("BBE2AA1  XBE2AL1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE2AA1  XBE2AL1  1", branchHelper.getBranchIdInNetwork());

        // dangling-line with element name
        branchHelper = new BranchHelper("XDE2AL1  DDE2AA1  DL AL", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getBranchIdInNetwork());
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(380., branchHelper.getNominalVoltage(Branch.Side.TWO), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.ONE), DOUBLE_TOLERANCE);
        assertEquals(1245, branchHelper.getCurrentLimit(Branch.Side.TWO), DOUBLE_TOLERANCE);

        branchHelper = new BranchHelper("XDE2AL1  DDE2AA1  DL AL", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("XDE2AL1  DDE2AA1  1", branchHelper.getBranchIdInNetwork());
    }

    @Test
    public void testInvalidDanglingLine() {

        // dangling-line exists but not with this order code
        assertFalse(new BranchHelper("XBE2AL1  BBE2AA1  2", network).isBranchValid());

        // dangling-line exists but not with this element name
        assertFalse(new BranchHelper("DDE2AA1  XDE2AL1  COUCOU", network).isBranchValid());
    }

    @Test
    public void testOtherConstructors() {

        // element name
        BranchHelper branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  BR BE1BE3", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // order code
        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // suffix
        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  BR BE1BE3", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        // id
        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  BR BE1BE3", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());

        branchHelper = new BranchHelper("BBE1AA1  BBE3AA1  1", network);
        assertTrue(branchHelper.isBranchValid());
        assertEquals("BBE1AA1  BBE3AA1  1", branchHelper.getBranchIdInNetwork());
    }

    @Test
    public void testInvalidConstructor() {
        assertFalse(new BranchHelper(null, network).isBranchValid());
    }

    @Test
    public void testInvalidBranchIdConstructor() {

        /*
         The presence of 'NODE1ID_ NODE2_ID SUFFIX' in the invalid reason message is checked, as the messages
         related to problems in ids should contain id
         */

        // wrong size of node id or suffix
        BranchHelper branchHelper = new BranchHelper("7_CHARA 7_CHARA E_NAME", network);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("was not found in the Network"));

        branchHelper = new BranchHelper("9_CHARACT 9_CHARACT E_NAME", network);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("was not found in the Network"));

        branchHelper = new BranchHelper("8_CHARAC 8_CHARAC ELEMENT_NAME_WITH_MORE_THAN_12_CHARAC", network);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("was not found in the Network"));

        // no suffix
        branchHelper = new BranchHelper("8_CHARAC 8_CHARAC", network);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("was not found in the Network"));

        // no blank space between ids and suffix
        branchHelper = new BranchHelper("8_CHARAC_8_CHARAC_1", network);
        assertFalse(branchHelper.isBranchValid());
        assertTrue(branchHelper.getInvalidBranchReason().contains("was not found in the Network"));
    }
}
