/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.swe_cne_exporter.xsd.CriticalNetworkElementMarketDocument;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCneTest {
    private Crac crac;
    private Network network;
    private RaoResult raoResult;
    private RaoParameters raoParameters;
    private SweCneExporterParameters exporterParameters;
    private CimCracCreationContext cracCreationContext;

    @Before
    public void setUp() {
        SweCneUtil.initUniqueIds();
        network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        crac = CracFactory.findDefault().create("test-crac");
        raoResult = Mockito.mock(RaoResult.class);
        raoParameters = new RaoParameters();
        cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        Mockito.when(cracCreationContext.getMonitoredSeriesCreationContexts()).thenReturn(new HashMap<>());
        Mockito.when(cracCreationContext.getRemedialActionSeriesCreationContexts()).thenReturn(new HashSet<>());
        Mockito.when(cracCreationContext.getTimeStamp()).thenReturn(OffsetDateTime.of(2021, 11, 15, 11, 50, 0, 0, ZoneOffset.of("+1")));
    }

    @Test
    public void testHeader() {
        exporterParameters = new SweCneExporterParameters("22XCORESO------S-20211115-F299v1", 2, "10YDOM-REGION-1V", SweCneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "22XCORESO------S", SweCneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, "17XTSO-CS------W", SweCneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "2021-10-30T22:00:00Z/2021-10-31T23:00:00Z");
        SweCne cne = new SweCne(crac, network, cracCreationContext, raoResult, raoParameters, exporterParameters);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        assertEquals("22XCORESO------S-20211115-F299v1", marketDocument.getMRID());
        assertEquals("2", marketDocument.getRevisionNumber());
        assertEquals("10YDOM-REGION-1V", marketDocument.getDomainMRID().getValue());
        assertEquals("A01", marketDocument.getDomainMRID().getCodingScheme());
        assertEquals("A48", marketDocument.getProcessProcessType());
        assertEquals("22XCORESO------S", marketDocument.getSenderMarketParticipantMRID().getValue());
        assertEquals("A01", marketDocument.getSenderMarketParticipantMRID().getCodingScheme());
        assertEquals("A44", marketDocument.getSenderMarketParticipantMarketRoleType());
        assertEquals("17XTSO-CS------W", marketDocument.getReceiverMarketParticipantMRID().getValue());
        assertEquals("A01", marketDocument.getReceiverMarketParticipantMRID().getCodingScheme());
        assertEquals("A36", marketDocument.getReceiverMarketParticipantMarketRoleType());
        assertEquals("2021-10-30T22:00Z", marketDocument.getTimePeriodTimeInterval().getStart());
        assertEquals("2021-10-31T23:00Z", marketDocument.getTimePeriodTimeInterval().getEnd());
        assertEquals("CNE_RAO_CASTOR-TimeSeries-1", marketDocument.getTimeSeries().get(0).getMRID());
    }

}
