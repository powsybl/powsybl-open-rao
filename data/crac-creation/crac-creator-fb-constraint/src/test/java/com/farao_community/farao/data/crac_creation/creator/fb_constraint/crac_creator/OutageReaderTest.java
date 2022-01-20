/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.OutageType;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzer;
import com.farao_community.farao.data.crac_creation.util.ucte.UcteNetworkAnalyzerProperties;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class OutageReaderTest {

    private Crac crac;
    private OutageType outageType;
    private Network network;
    private UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private OutageType.Branch validBranch1;
    private OutageType.Branch validBranch2;
    private OutageType.Branch invalidBranch;
    private OutageType.HvdcVH validHvdc;
    private OutageType.HvdcVH invalidHvdc;

    @Before
    public void setUp() {
        network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/network/TestCase_severalVoltageLevels_Xnodes.uct"));
        ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES));
        crac = CracFactory.findDefault().create("cracId");

        outageType = new OutageType();
        outageType.setId("outageId");
        outageType.setName("outageName");
        outageType.setLocation("outageLocation");

        validBranch1 = new OutageType.Branch();
        validBranch1.setFrom("XBEFR11 ");
        validBranch1.setTo("BBE2AA1 ");
        validBranch1.setElementName("TL BE2X");

        validBranch2 = new OutageType.Branch();
        validBranch2.setFrom("FFR1AA1 ");
        validBranch2.setTo("FFR1AA2 ");
        validBranch2.setElementName("5");

        invalidBranch = new OutageType.Branch();
        invalidBranch.setFrom("XBEFR22 ");
        invalidBranch.setTo("BBE2AA1 ");
        invalidBranch.setOrder("8");

        validHvdc = new OutageType.HvdcVH();
        validHvdc.setFrom("XBE2AL1 ");
        validHvdc.setTo("XDE2AL1 ");

        invalidHvdc = new OutageType.HvdcVH();
        invalidHvdc.setFrom("XBE2AL1 ");
        invalidHvdc.setTo("XUNKNOWN");
    }

    @Test
    public void testNeitherBranchNorHvdcVh() {
        assertFalse(new OutageReader(outageType, ucteNetworkAnalyzer).isOutageValid());
    }

    @Test
    public void testOneValidBranch() {
        outageType.getBranch().add(validBranch1);
        OutageReader outageReader = new OutageReader(outageType, ucteNetworkAnalyzer);
        outageReader.addContingency(crac);

        assertTrue(outageReader.isOutageValid());
        assertNotNull(crac.getContingency("outageId"));
        assertEquals("outageName", crac.getContingency("outageId").getName());
        assertEquals(1, crac.getContingency("outageId").getNetworkElements().size());
        assertTrue(crac.getContingency("outageId").getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1")));
    }

    @Test
    public void testTwoValidBranches() {
        outageType.getBranch().add(validBranch1);
        outageType.getBranch().add(validBranch2);
        OutageReader outageReader = new OutageReader(outageType, ucteNetworkAnalyzer);
        outageReader.addContingency(crac);

        assertTrue(outageReader.isOutageValid());
        assertNotNull(crac.getContingency("outageId"));
        assertEquals(2, crac.getContingency("outageId").getNetworkElements().size());
        assertTrue(crac.getContingency("outageId").getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("FFR3AA1  XBEFR11  1 + XBEFR11  BBE2AA1  1")));
        assertTrue(crac.getContingency("outageId").getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("FFR1AA2  FFR1AA1  5")));
    }

    @Test
    public void testOneInvalidBranch() {
        outageType.getBranch().add(invalidBranch);
        assertFalse(new OutageReader(outageType, ucteNetworkAnalyzer).isOutageValid());
    }

    @Test
    public void testOneValidAndOneInvalidBranch() {
        outageType.getBranch().add(invalidBranch);
        outageType.getBranch().add(validBranch1);
        assertFalse(new OutageReader(outageType, ucteNetworkAnalyzer).isOutageValid());
    }

    @Test
    public void testOneValidHvdc() {
        outageType.getHvdcVH().add(validHvdc);
        OutageReader outageReader = new OutageReader(outageType, ucteNetworkAnalyzer);
        outageReader.addContingency(crac);

        assertTrue(outageReader.isOutageValid());
        assertNotNull(crac.getContingency("outageId"));
        assertEquals("outageName", crac.getContingency("outageId").getName());
        assertEquals(2, crac.getContingency("outageId").getNetworkElements().size());
        assertTrue(crac.getContingency("outageId").getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XBE2AL1  1")));
        assertTrue(crac.getContingency("outageId").getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("XDE2AL1  DDE2AA1  1")));
    }

    @Test
    public void testOneInvalidHvdc() {
        outageType.getHvdcVH().add(invalidHvdc);
        assertFalse(new OutageReader(outageType, ucteNetworkAnalyzer).isOutageValid());
    }

    @Test
    public void testOneValidAndOneInvalidHvdc() {
        outageType.getHvdcVH().add(invalidHvdc);
        outageType.getHvdcVH().add(validHvdc);
        assertFalse(new OutageReader(outageType, ucteNetworkAnalyzer).isOutageValid());
    }
}
