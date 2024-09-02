/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.corecneexporter;

import com.powsybl.openrao.data.cneexportercommons.CneExporterParameters;
import com.powsybl.openrao.data.cneexportercommons.CneUtil;
import com.powsybl.openrao.data.corecneexporter.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CoreCneTest {
    private Crac crac;
    private Network network;
    private RaoResult raoResult;
    private RaoParameters raoParameters;
    private CneExporterParameters exporterParameters;
    private UcteCracCreationContext cracCreationContext;

    @BeforeEach
    public void setUp() {
        CneUtil.initUniqueIds();
        network = Network.read("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        crac = CracFactory.findDefault().create("test-crac");
        raoResult = Mockito.mock(RaoResult.class);
        raoParameters = new RaoParameters();
        cracCreationContext = Mockito.mock(UcteCracCreationContext.class);
        Mockito.when(cracCreationContext.getBranchCnecCreationContexts()).thenReturn(new ArrayList<>());
        Mockito.when(cracCreationContext.getRemedialActionCreationContexts()).thenReturn(new ArrayList<>());
        Mockito.when(cracCreationContext.getTimeStamp()).thenReturn(OffsetDateTime.of(2021, 11, 15, 11, 50, 0, 0, ZoneOffset.of("+1")));
    }

    @Test
    void testHeader() {
        exporterParameters = new CneExporterParameters("22XCORESO------S-20211115-F299v1", 2, "10YDOM-REGION-1V", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "22XCORESO------S", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, "17XTSO-CS------W", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "2021-10-30T22:00:00Z/2021-10-31T23:00:00Z");
        CoreCne cne = new CoreCne(crac, network, cracCreationContext, raoResult, raoParameters, exporterParameters);
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
