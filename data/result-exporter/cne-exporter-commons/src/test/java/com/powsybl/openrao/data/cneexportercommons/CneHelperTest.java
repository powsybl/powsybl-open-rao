/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CneHelperTest {
    @Test
    void testInitFromProperties() {
        Crac crac = Mockito.mock(Crac.class);
        RaoResult raoResult = Mockito.mock(RaoResult.class);

        Properties properties = new Properties();
        properties.setProperty("relative-positive-margins", "true");
        properties.setProperty("with-loop-flows", "false");
        properties.setProperty("mnec-acceptable-margin-diminution", "50");
        properties.setProperty("document-id", "documentId");
        properties.setProperty("revision-number", "1");
        properties.setProperty("domain-id", "domainId");
        properties.setProperty("process-type", "Z01");
        properties.setProperty("sender-id", "senderId");
        properties.setProperty("sender-role", "A04");
        properties.setProperty("receiver-id", "receiverId");
        properties.setProperty("receiver-role", "A36");
        properties.setProperty("time-interval", "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties);

        assertEquals(crac, cneHelper.getCrac());
        assertEquals(raoResult, cneHelper.getRaoResult());
        assertTrue(cneHelper.isRelativePositiveMargins());
        assertFalse(cneHelper.isWithLoopFlows());
        assertEquals(50d, cneHelper.getMnecAcceptableMarginDiminution());
        assertEquals("documentId", cneHelper.getDocumentId());
        assertEquals(1, cneHelper.getRevisionNumber());
        assertEquals("Z01", cneHelper.getProcessType());
        assertEquals("senderId", cneHelper.getSenderId());
        assertEquals("A04", cneHelper.getSenderRole());
        assertEquals("receiverId", cneHelper.getReceiverId());
        assertEquals("A36", cneHelper.getReceiverRole());
        assertEquals("2021-04-02T12:00:00Z/2021-04-02T13:00:00Z", cneHelper.getTimeInterval());
    }
}
