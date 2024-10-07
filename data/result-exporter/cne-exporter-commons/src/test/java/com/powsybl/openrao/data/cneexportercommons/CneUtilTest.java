/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cneexportercommons;

import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static com.powsybl.openrao.data.cneexportercommons.CneUtil.getParametersFromProperties;
import static com.powsybl.openrao.data.cneexportercommons.CneUtil.getRaoParametersFromProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CneUtilTest {
    @Test
    void testGetRaoParametersFromProperties() {
        Properties emptyProperties = new Properties();
        RaoParameters defaultRaoParameters = getRaoParametersFromProperties(emptyProperties);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, defaultRaoParameters.getObjectiveFunctionParameters().getType());
        assertFalse(defaultRaoParameters.hasExtension(LoopFlowParametersExtension.class));
        assertFalse(defaultRaoParameters.hasExtension(MnecParametersExtension.class));

        Properties propertiesAmpere = new Properties();
        propertiesAmpere.setProperty("objective-function-type", "max-min-margin-in-ampere");
        propertiesAmpere.setProperty("with-loop-flows", "true");
        propertiesAmpere.setProperty("mnec-acceptable-margin-diminution", "0");
        RaoParameters raoParametersAmpere = getRaoParametersFromProperties(propertiesAmpere);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE, raoParametersAmpere.getObjectiveFunctionParameters().getType());
        assertTrue(raoParametersAmpere.hasExtension(LoopFlowParametersExtension.class));
        assertFalse(raoParametersAmpere.hasExtension(MnecParametersExtension.class));

        Properties propertiesMegawatt = new Properties();
        propertiesMegawatt.setProperty("objective-function-type", "max-min-margin-in-megawatt");
        propertiesMegawatt.setProperty("with-loop-flows", "true");
        propertiesMegawatt.setProperty("mnec-acceptable-margin-diminution", "10");
        RaoParameters raoParametersMegawatt = getRaoParametersFromProperties(propertiesMegawatt);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT, raoParametersMegawatt.getObjectiveFunctionParameters().getType());
        assertTrue(raoParametersMegawatt.hasExtension(LoopFlowParametersExtension.class));
        assertTrue(raoParametersMegawatt.hasExtension(MnecParametersExtension.class));
        assertEquals(10d, raoParametersMegawatt.getExtension(MnecParametersExtension.class).getAcceptableMarginDecrease());

        Properties propertiesRelativeAmpere = new Properties();
        propertiesRelativeAmpere.setProperty("objective-function-type", "max-min-relative-margin-in-ampere");
        propertiesRelativeAmpere.setProperty("with-loop-flows", "true");
        propertiesRelativeAmpere.setProperty("mnec-acceptable-margin-diminution", "20.5");
        RaoParameters raoParametersRelativeAmpere = getRaoParametersFromProperties(propertiesRelativeAmpere);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, raoParametersRelativeAmpere.getObjectiveFunctionParameters().getType());
        assertTrue(raoParametersRelativeAmpere.hasExtension(LoopFlowParametersExtension.class));
        assertTrue(raoParametersRelativeAmpere.hasExtension(MnecParametersExtension.class));
        assertEquals(20.5, raoParametersRelativeAmpere.getExtension(MnecParametersExtension.class).getAcceptableMarginDecrease());
    }

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
}
