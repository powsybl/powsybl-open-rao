/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.RangeActionSpeed;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SweCneDivergentAngleMonitoringTest {

    @Test
    void testExport() throws IOException {
        Network network = Network.read(new File(SweCneTest.class.getResource("/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
        InputStream is = getClass().getResourceAsStream("/CIM_CRAC.xml");

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2), new RangeActionSpeed("PRA_1", 3));
        CimCracCreationParameters cimCracCreationParameters = new CimCracCreationParameters();
        cimCracCreationParameters.setRemedialActionSpeed(rangeActionSpeeds);
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setCracFactoryName("CracImplFactory");
        cracCreationParameters.addExtension(CimCracCreationParameters.class, cimCracCreationParameters);
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC));
        CracCreationContext cracCreationContext = Crac.readWithContext("CIM_CRAC.xml", is, network, cracCreationParameters);
        Crac crac = cracCreationContext.getCrac();
        InputStream inputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/RaoResult.json").getFile());
        RaoResult raoResult = RaoResult.read(inputStream, crac);

        MonitoringResult monitoringResult = new MonitoringResult(PhysicalParameter.ANGLE,
            Set.of(new CnecResult(crac.getCnec("ac1"), Unit.DEGREE, new AngleCnecValue(4.0), 2., Cnec.SecurityStatus.FAILURE)),
            Map.of(crac.getState("Co-1", crac.getInstant(InstantKind.CURATIVE)), Set.of(crac.getRemedialAction("na1"))),
            Cnec.SecurityStatus.FAILURE);

        RaoResultWithAngleMonitoring raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, monitoringResult);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.swe-cne.document-id", "documentId");
        properties.setProperty("rao-result.export.swe-cne.revision-number", "1");
        properties.setProperty("rao-result.export.swe-cne.domain-id", "domainId");
        properties.setProperty("rao-result.export.swe-cne.process-type", "Z01");
        properties.setProperty("rao-result.export.swe-cne.sender-id", "senderId");
        properties.setProperty("rao-result.export.swe-cne.sender-role", "A04");
        properties.setProperty("rao-result.export.swe-cne.receiver-id", "receiverId");
        properties.setProperty("rao-result.export.swe-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.swe-cne.time-interval", "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
        new SweCneExporter().exportData(raoResultWithAngleMonitoring, cracCreationContext, properties, outputStream);
        try {
            InputStream expectedCneInputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/SweCNEDivergentAngleMonitoring_Z01.xml").getFile());
            SweCneTest.compareCneFiles(expectedCneInputStream, new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            Assertions.fail();
        }
    }
}

