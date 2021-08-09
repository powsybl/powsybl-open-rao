/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelperTest {

    /*

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Network network;
    private UcteNetworkHelper networkHelper;

    private void setUp(String networkFile, boolean completeSmallBusIdsWithWildcards) {
        network = Importers.loadNetwork(networkFile, getClass().getResourceAsStream("/" + networkFile));
        UcteNetworkHelperProperties.BusIdMatchPolicy matchPolicy = completeSmallBusIdsWithWildcards ? UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS : UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES;
        networkHelper = new UcteNetworkHelper(network, new UcteNetworkHelperProperties(matchPolicy));
    }

    @Test
    public void testGetNetwork() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);
        assertSame(network, networkHelper.getNetwork());
    }

    @Test
    public void testInternalBranchNoInversion() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", false);

        // internal branch with order code, from/to same as network
        Pair<Identifiable<?>, UcteConnectable.MatchResult> result = networkHelper.findNetworkElement("BBE1AA1 ", "BBE2AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE2AA1  1"), result.getLeft());

        // internal branch with element name, from/to same as network
        result = networkHelper.findNetworkElement("FFR1AA1 ", "FFR3AA1 ", "BR FR1FR3");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("FFR1AA1  FFR3AA1  2"), result.getLeft());

        // internal branch with order code, from/to different from network
        result = networkHelper.findNetworkElement("BBE2AA1 ", "BBE1AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE2AA1  1"), result.getLeft());

        // internal branch with element name, from/to different from network
        result = networkHelper.findNetworkElement("BBE3AA1 ", "BBE1AA1 ", "BR BE1BE3");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE3AA1  1"), result.getLeft());
    }

    @Test
    public void testInvalidInternalBranch() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // unknown from
        assertNull(networkHelper.findNetworkElement("UNKNOW1 ", "BBE1AA1", "1"));

        // unknown to
        assertNull(networkHelper.findNetworkElement("BBE3AA1 ", "UNKNOW1 ", "1"));

        // branch exists but not with this order code
        assertNull(networkHelper.findNetworkElement("BBE1AA1 ", "BBE2AA1 ", "4"));

        // branch exists but not with this element name
        assertNull(networkHelper.findNetworkElement("BBE1AA1 ", "BBE3AA1 ", "COUCOU"));
    }

    @Test
    public void testValidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        /* note that transformers from/to node are systematically inverted by PowSyBl UCTE importer
        For instance, the transformer with id "UCTNODE1 UCTNODE2 1" have :
            - terminal1 = UCTNODE2
            - terminal2 = UCTNODE1
        That's why the branch is inverted when from/to is aligned with what is defined in the UCTE file, and vice versa.
        This is note the case for the other type of Branch, where terminal 1 and 2 match the id.
         */

        // transformer with order code, from/to same as network
        /*
        Pair<Identifiable<?>, UcteConnectable.MatchResult> result = networkHelper.findNetworkElement("BBE2AA1 ", "BBE3AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("FFR1AA2 ", "FFR1AA1 ", "5");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("FFR1AA2  FFR1AA1  5"), result.getLeft());

        // transformer with element name, from/to same as network
        result = networkHelper.findNetworkElement("BBE2AA1 ", "BBE3AA1 ", "PST BE");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("BBE1AA1 ", "BBE1AA2 ", "TR BE1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE1AA2  1"), result.getLeft());

        // transformer with order code, from/to different from network
        result = networkHelper.findNetworkElement("BBE3AA1 ", "BBE2AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("BBE2AA1 ", "BBE2AA2 ", "2");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA2  BBE2AA1  2"), result.getLeft());

        // transformer with element name, from/to different from network
        result = networkHelper.findNetworkElement("BBE3AA1 ", "BBE2AA1 ", "PST BE");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("FFR1AA1 ", "FFR1AA2 ", "TR FR1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("FFR1AA2  FFR1AA1  5"), result.getLeft());
    }

    @Test
    public void testInvalidTransformer() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // transformer exists but not with this order code
        assertNull(networkHelper.findNetworkElement("BBE2AA1 ", "BBE3AA1 ", "2"));

        // transformer exists but not with this element name
        assertNull(networkHelper.findNetworkElement("FFR1AA2 ", "FFR1AA1 ", "COUCOU"));
    }

    @Test
    public void testValidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // tie-line with order code
        Pair<Identifiable<?>, UcteConnectable.MatchResult> result = networkHelper.findNetworkElement("XFRDE11 ", "DDE3AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("DDE3AA1 ", "XFRDE11 ", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("XFRDE11 ", "FFR2AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_TWO, result.getRight());
        assertSame(network.getIdentifiable("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("FFR2AA1 ", "XFRDE11 ", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_TWO, result.getRight());
        assertSame(network.getIdentifiable("XFRDE11  DDE3AA1  1 + XFRDE11  FFR2AA1  1"), result.getLeft());

        // tie-line with element name
        result = networkHelper.findNetworkElement("NNL2AA1 ", "XNLBE11 ", "TL NL2X");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("XNLBE11 ", "NNL2AA1 ", "TL NL2X");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("XNLBE11 ", "BBE3AA1 ", "TL BE3X");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_TWO, result.getRight());
        assertSame(network.getIdentifiable("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("BBE3AA1 ", "XNLBE11 ", "TL BE3X");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_TWO, result.getRight());
        assertSame(network.getIdentifiable("NNL2AA1  XNLBE11  1 + XNLBE11  BBE3AA1  1"), result.getLeft());
    }

    @Test
    public void testInvalidTieLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // tie-line exists but not with this order code
        assertNull(networkHelper.findNetworkElement("XFRDE11 ", "FFR2AA1 ", "7"));

        // tie-line exists but not with this element name
        assertNull(networkHelper.findNetworkElement("NNL2AA1 ", "XNLBE11 ", "COUCOU"));
    }

    @Test
    public void testValidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // dangling-line with order code
        Pair<Identifiable<?>, UcteConnectable.MatchResult> result = networkHelper.findNetworkElement("BBE2AA1 ", "XBE2AL1 ", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  XBE2AL1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("XBE2AL1 ", "BBE2AA1 ", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE2AA1  XBE2AL1  1"), result.getLeft());

        // dangling-line with element name
        result = networkHelper.findNetworkElement("XDE2AL1 ", "DDE2AA1 ", "DL AL");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("XDE2AL1  DDE2AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("DDE2AA1 ", "XDE2AL1 ", "DL AL");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("XDE2AL1  DDE2AA1  1"), result.getLeft());
    }

    @Test
    public void testInvalidDanglingLine() {
        setUp("TestCase_severalVoltageLevels_Xnodes.uct", true);

        // dangling-line exists but not with this order code
        assertNull(networkHelper.findNetworkElement("XBE2AL1 ", "BBE2AA1 ", "2"));

        // dangling-line exists but not with this element name
        assertNull(networkHelper.findNetworkElement("DDE2AA1 ", "XDE2AL1 ", "COUCOU"));
    }

    @Test
    public void testSwitch() {
        setUp("TestCase16Nodes_with_different_imax.uct", true);

        Pair<Identifiable<?>, UcteConnectable.MatchResult> result = networkHelper.findNetworkElement("BBE1AA1", "BBE4AA1", "1");
        assertEquals(UcteConnectable.MatchResult.MATCHED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE4AA1  1"), result.getLeft());

        result = networkHelper.findNetworkElement("BBE4AA1", "BBE1AA1", "1");
        assertEquals(UcteConnectable.MatchResult.INVERTED_ON_SIDE_ONE, result.getRight());
        assertSame(network.getIdentifiable("BBE1AA1  BBE4AA1  1"), result.getLeft());
    }

    @Test
    public void testTooManyMatches() {
        setUp("TestCase16Nodes_with_different_imax.uct", true);
        assertThrows(IllegalArgumentException.class, () -> networkHelper.findNetworkElement("BBE", "BBE", "1"));
    }


    */
}
