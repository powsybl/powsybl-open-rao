/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracio.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.cracio.cim.parameters.RangeActionSpeed;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoringResult;
import com.powsybl.openrao.monitoring.anglemonitoring.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.anglemonitoring.json.AngleMonitoringResultImporter;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.Set;

import static com.powsybl.openrao.data.swecneexporter.SweCneTest.compareCneFiles;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SweCneDivergentAngleMonitoringTest {
    private Crac crac;
    private CracCreationContext cracCreationContext;
    private Network network;
    private RaoResultWithAngleMonitoring raoResultWithAngleMonitoring;

    @BeforeEach
    public void setUp() throws IOException {
        network = Network.read(new File(SweCneTest.class.getResource("/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
        InputStream is = getClass().getResourceAsStream("/CIM_CRAC.xml");

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2), new RangeActionSpeed("PRA_1", 3));
        CimCracCreationParameters cimCracCreationParameters = new CimCracCreationParameters();
        cimCracCreationParameters.setRemedialActionSpeed(rangeActionSpeeds);
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setCracFactoryName("CracImplFactory");
        cracCreationParameters.addExtension(CimCracCreationParameters.class, cimCracCreationParameters);

        cracCreationContext = Crac.readWithContext("CIM_CRAC.xml", is, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), cracCreationParameters);
        crac = cracCreationContext.getCrac();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/RaoResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RaoResult raoResult = RaoResult.read(inputStream, crac);
        InputStream inputStream2 = null;
        try {
            inputStream2 = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/AngleMonitoringDivergentResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(inputStream2, crac);
        raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
    }

    @Test
    void testExport() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("document-id", "documentId");
        properties.setProperty("revision-number", "1");
        properties.setProperty("domain-id", "domainId");
        properties.setProperty("process-type", "Z01");
        properties.setProperty("sender-id", "senderId");
        properties.setProperty("sender-role", "A04");
        properties.setProperty("receiver-id", "receiverId");
        properties.setProperty("receiver-role", "A36");
        properties.setProperty("time-interval", "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
        new SweCneExporter().exportData(raoResultWithAngleMonitoring, cracCreationContext, properties, outputStream);
        try {
            InputStream inputStream = new FileInputStream(SweCneDivergentAngleMonitoringTest.class.getResource("/SweCNEDivergentAngleMonitoring_Z01.xml").getFile());
            compareCneFiles(inputStream, new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            Assertions.fail();
        }
    }
}
