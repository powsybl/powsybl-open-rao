/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static com.powsybl.openrao.data.cneexportercommons.CneUtil.getParametersFromProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CneUtilTest {

    @Test
    void testGetParametersFromProperties() {
        Properties propertiesDayAheadCc = createDefaultProperties();
        propertiesDayAheadCc.setProperty("process-type", "A48");
        propertiesDayAheadCc.setProperty("sender-role", "A36");
        propertiesDayAheadCc.setProperty("receiver-role", "A44");
        CneExporterParameters parametersDayAheadCc = getParametersFromProperties(propertiesDayAheadCc);
        checkCommonCneExporterParametersData(parametersDayAheadCc);
        assertEquals(CneExporterParameters.ProcessType.DAY_AHEAD_CC, parametersDayAheadCc.getProcessType());
        assertEquals(CneExporterParameters.RoleType.CAPACITY_COORDINATOR, parametersDayAheadCc.getSenderRole());
        assertEquals(CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, parametersDayAheadCc.getReceiverRole());

        Properties propertiesZ01 = createDefaultProperties();
        propertiesZ01.setProperty("process-type", "Z01");
        propertiesZ01.setProperty("sender-role", "A36");
        propertiesZ01.setProperty("receiver-role", "A04");
        CneExporterParameters parametersZ01 = getParametersFromProperties(propertiesZ01);
        checkCommonCneExporterParametersData(parametersZ01);
        assertEquals(CneExporterParameters.ProcessType.Z01, parametersZ01.getProcessType());
        assertEquals(CneExporterParameters.RoleType.CAPACITY_COORDINATOR, parametersZ01.getSenderRole());
        assertEquals(CneExporterParameters.RoleType.SYSTEM_OPERATOR, parametersZ01.getReceiverRole());
    }

    @Test
    void testGetParametersFromPropertiesWithUnknownEnumFields() {
        Properties propertiesUnknownProcessType = createDefaultProperties();
        propertiesUnknownProcessType.setProperty("process-type", "???");
        propertiesUnknownProcessType.setProperty("sender-role", "A36");
        propertiesUnknownProcessType.setProperty("receiver-role", "A44");
        OpenRaoException exceptionUnknownProcessType = assertThrows(OpenRaoException.class, () -> getParametersFromProperties(propertiesUnknownProcessType));
        assertEquals("Unknown ProcessType ???", exceptionUnknownProcessType.getMessage());

        Properties propertiesUnknownRoleType = createDefaultProperties();
        propertiesUnknownRoleType.setProperty("process-type", "A48");
        propertiesUnknownRoleType.setProperty("sender-role", "???");
        propertiesUnknownRoleType.setProperty("receiver-role", "A44");
        OpenRaoException exceptionUnknownRoleType = assertThrows(OpenRaoException.class, () -> getParametersFromProperties(propertiesUnknownRoleType));
        assertEquals("Unknown RoleType ???", exceptionUnknownRoleType.getMessage());
    }

    private static Properties createDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty("document-id", "documentId");
        properties.setProperty("revision-number", "1");
        properties.setProperty("domain-id", "domainId");
        properties.setProperty("sender-id", "senderId");
        properties.setProperty("receiver-id", "receiverId");
        properties.setProperty("time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");
        return properties;
    }

    private static void checkCommonCneExporterParametersData(CneExporterParameters cneExporterParameters) {
        assertEquals("documentId", cneExporterParameters.getDocumentId());
        assertEquals(1, cneExporterParameters.getRevisionNumber());
        assertEquals("domainId", cneExporterParameters.getDomainId());
        assertEquals("senderId", cneExporterParameters.getSenderId());
        assertEquals("receiverId", cneExporterParameters.getReceiverId());
        assertEquals("2021-10-30T22:00Z/2021-10-31T23:00Z", cneExporterParameters.getTimeInterval());
    }

    @Test
    void testGetParametersFromMissingProperties() {
        Properties properties = createDefaultProperties();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> getParametersFromProperties(properties));
        assertEquals("Could not parse CNE exporter parameters because mandatory property process-type is missing.", exception.getMessage());
    }
}
