/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CoreCneExporterParametersTest {
    @Test
    public void basicTest() {
        SweCneExporterParameters params = new SweCneExporterParameters(
            "a", 3, "b", SweCneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "c", SweCneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
            "e", SweCneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "timeInterval");

        assertEquals("a", params.getDocumentId());
        assertEquals(3, params.getRevisionNumber());
        assertEquals("b", params.getDomainId());

        assertEquals(SweCneExporterParameters.ProcessType.DAY_AHEAD_CC, params.getProcessType());
        assertEquals("A48", params.getProcessType().getCode());
        assertEquals("A48", params.getProcessType().toString());

        assertEquals("c", params.getSenderId());
        assertEquals(SweCneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, params.getSenderRole());
        assertEquals("A44", params.getSenderRole().getCode());
        assertEquals("A44", params.getSenderRole().toString());

        assertEquals("e", params.getReceiverId());
        assertEquals(SweCneExporterParameters.RoleType.CAPACITY_COORDINATOR, params.getReceiverRole());
        assertEquals("A36", params.getReceiverRole().getCode());
        assertEquals("A36", params.getReceiverRole().toString());

        assertEquals("timeInterval", params.getTimeInterval());
    }
}
