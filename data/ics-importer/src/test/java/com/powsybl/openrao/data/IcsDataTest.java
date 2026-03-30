/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.powsybl.openrao.data.IcsDataImporterTest.generateOffsetDateTimeList;
import static com.powsybl.openrao.data.IcsUtil.MAX_GRADIENT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IcsDataTest {
    private static final double DOUBLE_EPSILON = 1e-6;
    private Crac crac1;
    private Crac crac2;
    private Network network1;
    private Network network2;
    private TemporalData<Network> networkTemporalData;
    private TemporalData<Crac> cracTemporalData;

    @BeforeEach
    void setUp() throws IOException {
        // we need to import twice the network to avoid variant names conflicts on the same network object
        String networkFilePath1 = "2Nodes2ParallelLinesPST_0030.uct";
        String networkFilePath2 = "2Nodes2ParallelLinesPST_0130.uct";
        network1 = Network.read(networkFilePath1, IcsDataTest.class.getResourceAsStream("/network/" + networkFilePath1));
        network2 = Network.read(networkFilePath2, IcsDataTest.class.getResourceAsStream("/network/" + networkFilePath2));

        crac1 = Crac.read("/crac/crac-0030.json", getClass().getResourceAsStream("/crac/crac-0030.json"), network1);
        crac2 = Crac.read("/crac/crac-0130.json", getClass().getResourceAsStream("/crac/crac-0130.json"), network2);

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 13, 0, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 13, 1, 30, 0, 0, ZoneOffset.UTC);

        networkTemporalData = new TemporalDataImpl<>(
            Map.of(
                timestamp1, network1,
                timestamp2, network2
            ));

        cracTemporalData = new TemporalDataImpl<>(
            Map.of(
                timestamp1, crac1,
                timestamp2, crac2
            ));
    }

    // Test Create Generator Constraint

    @Test
    void testCreateGeneratorConstraintRaDefinedOnANode() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on the node "BBE1AA1"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));

        // Check generator constraint creation
        Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1")) );

        assertEquals(1, generatorConstraintsSet.size());
        GeneratorConstraints generatorConstraints = generatorConstraintsSet.iterator().next();
        assertEquals("Redispatching_RA_BBE1AA1_GENERATOR", generatorConstraints.getGeneratorId());
        assertTrue(generatorConstraints.getDownwardPowerGradient().isPresent());
        assertEquals(-20., generatorConstraints.getDownwardPowerGradient().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraints.getUpwardPowerGradient().isPresent());
        assertEquals(20., generatorConstraints.getUpwardPowerGradient().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraints.getLeadTime().isPresent());
        assertEquals(1.0, generatorConstraints.getLeadTime().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraints.getLagTime().isPresent());
        assertEquals(1.0, generatorConstraints.getLagTime().get(), DOUBLE_EPSILON);
        assertFalse(generatorConstraints.isShutDownAllowed());
        assertFalse(generatorConstraints.isStartUpAllowed());
    }

    @Test
    void testCreateGeneratorConstraintRaDefinedOnAGSK() throws IOException {
        // one remedial action "Redispatching_RA" defined on a GSK "GSK_NAME"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));

        Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"), Map.of("BBE1AA1", "Redispatching_RA_BBE1AA1_GENERATOR", "FFR1AA1", "Redispatching_RA_FFR1AA1_GENERATOR"));

        assertEquals(2, generatorConstraintsSet.size());
        GeneratorConstraints generatorConstraintsBE = generatorConstraintsSet.stream()
            .filter(gc -> gc.getGeneratorId().contains("BE"))
            .findFirst().orElseThrow();
        assertEquals("Redispatching_RA_BBE1AA1_GENERATOR", generatorConstraintsBE.getGeneratorId());
        assertEquals(-12., generatorConstraintsBE.getDownwardPowerGradient().orElseThrow(), DOUBLE_EPSILON); // 20*0.6
        assertEquals(12., generatorConstraintsBE.getUpwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);
        assertTrue(generatorConstraintsBE.getLeadTime().isPresent());
        assertEquals(1.0, generatorConstraintsBE.getLeadTime().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraintsBE.getLagTime().isPresent());
        assertEquals(1.0, generatorConstraintsBE.getLagTime().get(), DOUBLE_EPSILON);
        assertFalse(generatorConstraintsBE.isShutDownAllowed());
        assertFalse(generatorConstraintsBE.isStartUpAllowed());
        GeneratorConstraints generatorConstraintsFR = generatorConstraintsSet.stream()
            .filter(gc -> gc.getGeneratorId().contains("FR"))
            .findFirst().orElseThrow();
        assertEquals("Redispatching_RA_FFR1AA1_GENERATOR", generatorConstraintsFR.getGeneratorId());
        assertEquals(-8., generatorConstraintsFR.getDownwardPowerGradient().orElseThrow(), DOUBLE_EPSILON); //20*0.4
        assertEquals(8., generatorConstraintsFR.getUpwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);
        assertTrue(generatorConstraintsFR.getLeadTime().isPresent());
        assertEquals(1.0, generatorConstraintsFR.getLeadTime().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraintsFR.getLagTime().isPresent());
        assertEquals(1.0, generatorConstraintsFR.getLagTime().get(), DOUBLE_EPSILON);
        assertFalse(generatorConstraintsFR.isShutDownAllowed());
        assertFalse(generatorConstraintsFR.isStartUpAllowed());
    }

    @Test
    void testCreateGeneratorConstraintRaDefinedOnANodeMissingGradientLeadTimeAndLagTime() throws IOException {
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;Node;BBE1AA1;50;Coal;2;2;;;;;FALSE;FALSE
            """;
        IcsData icsData =   IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));
        Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1")) );
        assertEquals(1, generatorConstraintsSet.size());
        GeneratorConstraints generatorConstraints = generatorConstraintsSet.iterator().next();
        assertEquals("Redispatching_RA_BBE1AA1_GENERATOR", generatorConstraints.getGeneratorId());

        // Missing gradient value
        assertTrue(generatorConstraints.getDownwardPowerGradient().isPresent());
        assertEquals(-MAX_GRADIENT, generatorConstraints.getDownwardPowerGradient().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraints.getUpwardPowerGradient().isPresent());
        assertEquals(MAX_GRADIENT, generatorConstraints.getUpwardPowerGradient().get(), DOUBLE_EPSILON);

        // Missing lead time
        assertFalse(generatorConstraints.getLeadTime().isPresent());
        // Missing lag time
        assertFalse(generatorConstraints.getLagTime().isPresent());
    }

    @ParameterizedTest
    @MethodSource("startUpAndShutDownAllowedCsvCases")
    void testCreateGeneratorConstraintShutDownAndStartUpAllowed(String staticCsv, String expectedLogMessage) throws IOException {
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));

        if (expectedLogMessage == null) {
            Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1")) );
            assertEquals(1, generatorConstraintsSet.size());
            GeneratorConstraints generatorConstraints = generatorConstraintsSet.iterator().next();
            assertTrue(generatorConstraints.isShutDownAllowed());
            assertTrue(generatorConstraints.isStartUpAllowed());
        } else {
            Assertions.assertThatExceptionOfType(OpenRaoException.class)
                .isThrownBy(() -> icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 0.6, "FFR1AA1", 0.4), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1"), "FFR1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "FFR1AA1"))))
                .withMessage(expectedLogMessage);
        }
    }


    private static Stream<Arguments> startUpAndShutDownAllowedCsvCases() {
        String baseHeader = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            """;

        String shutDownAndStartUpTrue = baseHeader + """
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME;50;Coal;2;2;20;20;1;1;TRUE;TRUE
            """;

        String noShutDown = baseHeader + """
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME;50;Coal;2;2;20;20;1;1;FALSE;;
            """;

        String shutDownWrongValue = baseHeader + """
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME;50;Coal;2;2;20;20;1;1;FALSE;wrongValue
            """;

        String noStartUp = baseHeader + """
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME;50;Coal;2;2;20;20;1;1;;FALSE;
            """;

        String startUpWrongValue = baseHeader + """
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME;50;Coal;2;2;20;20;1;1;wrongValue;FALSE
            """;

        return Stream.of(
            Arguments.of(shutDownAndStartUpTrue,
                null),
            Arguments.of(noShutDown,
                "Could not parse shutDownAllowed value for raId Redispatching_RA: "),
            Arguments.of(shutDownWrongValue,
                "Could not parse shutDownAllowed value for raId Redispatching_RA: wrongValue"),
            Arguments.of(noStartUp,
                "Could not parse startUpAllowed value for raId Redispatching_RA: "),
            Arguments.of(startUpWrongValue,
                "Could not parse startUpAllowed value for raId Redispatching_RA: wrongValue")
        );
    }

    // Test createGeneratorAndLoadAndUpdateNetworks

    @Test
    void testCreateGeneratorAndLoadAndUpdateNetworksOnANode() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on the node "BBE1AA1"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(2));

        icsData.createGeneratorAndLoadAndUpdateNetworks(networkTemporalData, "Redispatching_RA", Map.of("BBE1AA1", 1.0));
        Generator generator1 = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116., generator1.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0, generator1.getMinP(), DOUBLE_EPSILON);
        Generator generator2 = network2.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(120., generator2.getTargetP(), DOUBLE_EPSILON);
        assertEquals(15.0, generator2.getMinP(), DOUBLE_EPSILON);
    }
    @Test
    void testCreateGeneratorAndLoadAndUpdateNetworksOnAGsk() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on a GSK "GSK_NAME"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(2));
        Map<String, String> generatorIdPerNodeId = icsData.createGeneratorAndLoadAndUpdateNetworks(networkTemporalData, "Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"));
        assertEquals(Map.of("BBE1AA1", "Redispatching_RA_BBE1AA1_GENERATOR", "FFR1AA1", "Redispatching_RA_FFR1AA1_GENERATOR"), generatorIdPerNodeId);
        Generator generatorBE = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116. * 0.6, generatorBE.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0 * 0.6, generatorBE.getMinP(), DOUBLE_EPSILON);
        Generator generatorFR = network1.getGenerator("Redispatching_RA_FFR1AA1_GENERATOR");
        assertEquals(116. * 0.4, generatorFR.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0 * 0.4, generatorFR.getMinP(), DOUBLE_EPSILON);
    }

    // Test createInjectionRangeActionsAndUpdateCracs
    @Test
    void testCreateInjectionRangeActionsAndUpdateCracsOnANode() throws IOException {
        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(2));


        // Test injection range action creation in crac
        icsData.createInjectionRangeActionsAndUpdateCracs(cracTemporalData, "Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1")), 5., 5.);
        assertEquals(1, crac1.getInjectionRangeActions().size());
        InjectionRangeAction ra1 = crac1.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra1.getId());
        assertEquals(116., ra1.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);

        assertEquals(1, crac2.getInjectionRangeActions().size());
        InjectionRangeAction ra2 = crac2.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra2.getId());
        assertEquals(120., ra2.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra2.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra2.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra2.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra2.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);
    }

     @Test
     void testCreateInjectionRangeActionsAndUpdateCracsOnAGsk() throws IOException {

         // Read ICS Data
         IcsData icsData = IcsDataImporter.read(
             getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
             getClass().getResourceAsStream("/ics/series.csv"),
             getClass().getResourceAsStream("/glsk/gsk.csv"),
             generateOffsetDateTimeList(2));

         icsData.createInjectionRangeActionsAndUpdateCracs(cracTemporalData, "Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"), Map.of("BBE1AA1", "Redispatching_RA_BBE1AA1_GENERATOR", "FFR1AA1", "Redispatching_RA_FFR1AA1_GENERATOR"), 5., 5.);

         assertEquals(1, crac1.getInjectionRangeActions().size());
         InjectionRangeAction ra1 = crac1.getInjectionRangeActions().iterator().next();
         assertEquals("Redispatching_RA_RD", ra1.getId());
         assertEquals(116., ra1.getInitialSetpoint(), DOUBLE_EPSILON);
         assertTrue(ra1.getVariationCost(VariationDirection.UP).isPresent());
         assertEquals(5., ra1.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
         assertTrue(ra1.getVariationCost(VariationDirection.DOWN).isPresent());
         assertEquals(5., ra1.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);
     }

    // Test Getter and Setter
    @Test
    void testIcsDataReadOkNode() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on the node "BBE1AA1"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));

        assertEquals(1, icsData.getStaticConstraintPerId().size());
        assertEquals(4, icsData.getTimeseriesPerIdAndType().get("Redispatching_RA").size());
    }

    @Test
    void testIcsImporterWithGSK() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on a GSK "GSK_NAME"

        // Read ICS Data
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));

        assertEquals(1, icsData.getStaticConstraintPerId().size());
        assertEquals(4, icsData.getTimeseriesPerIdAndType().get("Redispatching_RA").size());
        assertEquals(1, icsData.getWeightPerNodePerGsk().size());
        assertEquals(2, icsData.getWeightPerNodePerGsk().get("GSK_NAME").size());
        assertEquals(Map.of("BBE1AA1", 0.6, "FFR1AA1", 0.4), icsData.getWeightPerNodePerGsk().get("GSK_NAME"));

    }

}