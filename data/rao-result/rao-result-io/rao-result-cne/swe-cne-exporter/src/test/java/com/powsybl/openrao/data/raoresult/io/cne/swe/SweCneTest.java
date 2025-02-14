/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.iidm.network.Network;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class SweCneTest {
    private Crac crac;
    private CracCreationContext cracCreationContext;
    private Network network;
    private RaoResultWithAngleMonitoring raoResultWithAngle;
    private RaoResultWithAngleMonitoring raoResultFailureWithAngle;
    private Properties properties;

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
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC));
        cracCreationContext = Crac.readWithContext("CIM_CRAC.xml", is, network, cracCreationParameters);
        crac = cracCreationContext.getCrac();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(SweCneTest.class.getResource("/RaoResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RaoResult raoResult = RaoResult.read(inputStream, crac);

        InputStream inputStream3 = null;
        try {
            inputStream3 = new FileInputStream(SweCneTest.class.getResource("/RaoResultWithFailure.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RaoResult raoResultWithFailure = RaoResult.read(inputStream3, crac);

        MonitoringResult monitoringResult = new MonitoringResult(PhysicalParameter.ANGLE,
            Set.of(new CnecResult(crac.getCnec("ac1"), Unit.DEGREE, new AngleCnecValue(4.0), 2., Cnec.SecurityStatus.SECURE)),
            Map.of(crac.getState("Co-1", crac.getInstant(InstantKind.CURATIVE)), Set.of(crac.getRemedialAction("na1"))),
            Cnec.SecurityStatus.SECURE);

        raoResultWithAngle = new RaoResultWithAngleMonitoring(raoResult, monitoringResult);
        raoResultFailureWithAngle = new RaoResultWithAngleMonitoring(raoResultWithFailure, monitoringResult);

        properties = new Properties();
        properties.setProperty("rao-result.export.swe-cne.document-id", "documentId");
        properties.setProperty("rao-result.export.swe-cne.revision-number", "1");
        properties.setProperty("rao-result.export.swe-cne.domain-id", "domainId");
        properties.setProperty("rao-result.export.swe-cne.process-type", "Z01");
        properties.setProperty("rao-result.export.swe-cne.sender-id", "senderId");
        properties.setProperty("rao-result.export.swe-cne.sender-role", "A04");
        properties.setProperty("rao-result.export.swe-cne.receiver-id", "receiverId");
        properties.setProperty("rao-result.export.swe-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.swe-cne.time-interval", "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
    }

    @Test
    void testExport() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SweCneExporter().exportData(raoResultWithAngle, cracCreationContext, properties, outputStream);
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNE_Z01.xml").getFile());
            compareCneFiles(inputStream, new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    @Test
    void testExportWithFailure() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SweCneExporter().exportData(raoResultFailureWithAngle, cracCreationContext, properties, outputStream);
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNEWithFailure_Z01.xml").getFile());
            compareCneFiles(inputStream, new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    @Test
    void testValidateSchemaOk() {
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNE.xml").getFile());
            assertTrue(SweCneExporter.validateCNESchema(new String(inputStream.readAllBytes())));
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    @Test
    void testValidateSchemaNok() {
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNE_wrong.xml").getFile());
            assertFalse(SweCneExporter.validateCNESchema(new String(inputStream.readAllBytes())));
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    public static void compareCneFiles(InputStream expectedCneInputStream, InputStream actualCneInputStream) throws AssertionError {
        DiffBuilder db = DiffBuilder
            .compare(Input.fromStream(expectedCneInputStream))
            .withTest(Input.fromStream(actualCneInputStream))
            .ignoreComments()
            .withNodeFilter(SweCneTest::shouldCompareNode);
        Diff d = db.build();

        if (d.hasDifferences()) {
            DefaultComparisonFormatter formatter = new DefaultComparisonFormatter();
            StringBuffer buffer = new StringBuffer();
            for (Difference ds : d.getDifferences()) {
                buffer.append(formatter.getDescription(ds.getComparison()) + "\n");
            }
            throw new AssertionError("There are XML differences in CNE files\n" + buffer);
        }
        assertFalse(d.hasDifferences());
    }

    private static boolean shouldCompareNode(Node node) {
        if (node.getNodeName().equals("mRID")) {
            // For the following fields, mRID is generated randomly as per the CNE specifications
            // We should not compare them with the test file
            return !node.getParentNode().getNodeName().equals("TimeSeries")
                && !node.getParentNode().getNodeName().equals("Constraint_Series");
        } else {
            return !(node.getNodeName().equals("createdDateTime"));
        }
    }
}
