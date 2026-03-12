/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.CriticalNetworkElementMarketDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CoreCneTest {
    private Crac crac;
    private RaoResult raoResult;
    private UcteCracCreationContext cracCreationContext;

    @BeforeEach
    public void setUp() {
        CneUtil.initUniqueIds();
        crac = CracFactory.findDefault().create("test-crac");
        raoResult = Mockito.mock(RaoResult.class);
        cracCreationContext = Mockito.mock(UcteCracCreationContext.class);
        Mockito.when(cracCreationContext.getCrac()).thenReturn(crac);
        Mockito.when(cracCreationContext.getBranchCnecCreationContexts()).thenReturn(new ArrayList<>());
        Mockito.when(cracCreationContext.getRemedialActionCreationContexts()).thenReturn(new ArrayList<>());
        Mockito.when(cracCreationContext.getTimeStamp()).thenReturn(OffsetDateTime.of(2021, 11, 15, 11, 50, 0, 0, ZoneOffset.of("+1")));
    }

    @Test
    void testHeader() {
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "true");
        properties.setProperty("rao-result.export.core-cne.document-id", "22XCORESO------S-20211115-F299v1");
        properties.setProperty("rao-result.export.core-cne.revision-number", "2");
        properties.setProperty("rao-result.export.core-cne.domain-id", "10YDOM-REGION-1V");
        properties.setProperty("rao-result.export.core-cne.process-type", "A48");
        properties.setProperty("rao-result.export.core-cne.sender-id", "22XCORESO------S");
        properties.setProperty("rao-result.export.core-cne.sender-role", "A44");
        properties.setProperty("rao-result.export.core-cne.receiver-id", "17XTSO-CS------W");
        properties.setProperty("rao-result.export.core-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.core-cne.time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");

        CoreCne cne = new CoreCne(cracCreationContext, raoResult, properties);
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
