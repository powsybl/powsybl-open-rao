/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.impl.utils.ExhaustiveRaoResultCreation;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class RaoResultSerializerTest {

    @Test
    void testSerialize() throws IOException {
        // Basic RaoResult serialization test

        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithFastRaoExtension() throws IOException {
        // Test Serialization with FastRAO extension

        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        raoResult.addExtension(CriticalCnecsResult.class, new CriticalCnecsResult(crac.getFlowCnecs().stream().map(FlowCnec::getId).collect(Collectors.toSet())));

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result-fastrao-extension.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithoutMonitoringResult() throws IOException {
        // Test that if we have no indication that the RAO went through the monitoring in the executionDetails
        // => no voltage or angle CNEC results should be serialized

        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // Change the execution details
        raoResult.setExecutionDetails("The RAO only went through first preventive");

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result-no-monitoring.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithIntermediateInstantMonitoredCnec() throws IOException {
        // Add a voltage and angle cnec defined on auto state => The result should not be serialized

        Crac crac = ExhaustiveCracCreation.create();
        crac.newVoltageCnec().withId("voltageCnecIdAuto")
            .withName("voltageCnecNameAuto")
            .withNetworkElement("voltageCnecNeId", "voltageCnecNeName")
            .withInstant("auto")
            .withContingency("contingency1Id")
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(380.).add()
            .withReliabilityMargin(1.)
            .withMonitored()
            .add();
        crac.newAngleCnec().withId("angleCnecIdAuto")
            .withName("angleCnecNameAuto")
            .withExportingNetworkElement("eneId", "eneName")
            .withImportingNetworkElement("ineId", "ineName")
            .withInstant("auto")
            .withContingency("contingency1Id")
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-100.).withMax(100.).add()
            .withReliabilityMargin(10.)
            .withMonitored()
            .add();

        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithMissingResultMonitoring() throws IOException {
        // Add a voltage and angle cnec defined on curative instant but no results were associated in the raoResult
        // => CNECs should not be serialized

        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        crac.newVoltageCnec().withId("voltageCnecId2")
            .withName("voltageCnecName2")
            .withNetworkElement("voltageCnecNeId", "voltageCnecNeName")
            .withInstant("curative")
            .withContingency("contingency1Id")
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(380.).add()
            .withReliabilityMargin(1.)
            .withMonitored()
            .add();
        crac.newAngleCnec().withId("angleCnecId2")
            .withName("angleCnecName2")
            .withExportingNetworkElement("eneId", "eneName")
            .withImportingNetworkElement("ineId", "ineName")
            .withInstant("curative")
            .withContingency("contingency1Id")
            .withOperator("operator1")
            .newThreshold().withUnit(Unit.DEGREE).withMin(-100.).withMax(100.).add()
            .withReliabilityMargin(10.)
            .withMonitored()
            .add();

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

}
