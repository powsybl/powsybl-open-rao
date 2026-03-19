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
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IcsDataTest {
    private static final double DOUBLE_EPSILON = 1e-6;
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir") + File.separator;
    private String networkFilePathPostIcsImport1;
    private String networkFilePathPostIcsImport2;
    private Crac crac1;
    private Crac crac2;
    private Network network1;
    private Network network2;
    private TimeCoupledRaoInputWithNetworkPaths timeCoupledRaoInputWithNetworkPaths;
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

    public static List<OffsetDateTime> generateOffsetDateTimeList() {
        List<OffsetDateTime> dateTimes = new ArrayList<>();

        OffsetDateTime start = OffsetDateTime.of(2025, 2, 13, 0, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2025, 2, 13, 23, 30, 0, 0, ZoneOffset.UTC);

        OffsetDateTime current = start;
        while (!current.isAfter(end)) {
            dateTimes.add(current);
            current = current.plusHours(1);
        }

        return dateTimes;
    }

    @Test
    void testIcsDataReadOkNode() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on the node "BBE1AA1"

        // Read ICS Data
        IcsData icsData = IcsData.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList());

        assertEquals(1, icsData.getStaticConstraintPerId().size());
        assertEquals(4, icsData.getTimeseriesPerIdAndType().get("Redispatching_RA").size());

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

        // Test generator creation in network
        Map<String, String> generatorIdPerNodeId = icsData.createGeneratorAndLoadAndUpdateNetworks(networkTemporalData, "Redispatching_RA", Map.of("BBE1AA1", 1.0));
        Generator generator1 = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116., generator1.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0, generator1.getMinP(), DOUBLE_EPSILON);
        Generator generator2 = network2.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(120., generator2.getTargetP(), DOUBLE_EPSILON);
        assertEquals(15.0, generator2.getMinP(), DOUBLE_EPSILON);

        // Test injection range action creation in crac
        icsData.createInjectionRangeActionsAndUpdateCracs(cracTemporalData, "Redispatching_RA", Map.of("BBE1AA1", 1.0), generatorIdPerNodeId, 5., 5.);
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
    void testIcsImporterWithGSK() throws IOException {
        // Test generic case without any error
        // one remedial action "Redispatching_RA" defined on a GSK "GSK_NAME"

        // Read ICS Data
        IcsData icsData = IcsData.read(
            getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList());

        assertEquals(1, icsData.getStaticConstraintPerId().size());
        assertEquals(4, icsData.getTimeseriesPerIdAndType().get("Redispatching_RA").size());
        assertEquals(1, icsData.getWeightPerNodePerGsk().size());
        assertEquals(2, icsData.getWeightPerNodePerGsk().get("GSK_NAME").size());
        assertEquals(Map.of("BBE1AA1", 0.6, "FFR1AA1", 0.4), icsData.getWeightPerNodePerGsk().get("GSK_NAME"));

        Map<String, String> generatorIdPerNodeId = icsData.createGeneratorAndLoadAndUpdateNetworks(networkTemporalData, "Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"));
        assertEquals(Map.of("BBE1AA1", "Redispatching_RA_BBE1AA1_GENERATOR", "FFR1AA1", "Redispatching_RA_FFR1AA1_GENERATOR"), generatorIdPerNodeId);

        Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"), generatorIdPerNodeId);

        icsData.createInjectionRangeActionsAndUpdateCracs(cracTemporalData, "Redispatching_RA", icsData.getWeightPerNodePerGsk().get("GSK_NAME"), generatorIdPerNodeId, 5., 5.);

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

        assertEquals(1, crac1.getInjectionRangeActions().size());
        InjectionRangeAction ra1 = crac1.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra1.getId());
        assertEquals(116., ra1.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);

        Generator generatorBE = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116. * 0.6, generatorBE.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0 * 0.6, generatorBE.getMinP(), DOUBLE_EPSILON);
        Generator generatorFR = network1.getGenerator("Redispatching_RA_FFR1AA1_GENERATOR");
        assertEquals(116. * 0.4, generatorFR.getTargetP(), DOUBLE_EPSILON);
        assertEquals(10.0 * 0.4, generatorFR.getMinP(), DOUBLE_EPSILON);
    }

    @Test
    void testIcsDataReadNok() {
        // Test reader, where some remedial action need to be filtered out
    }

    @Test
    void testIcsImporterShutDownAndStartUpTrue() throws IOException {
        // Check that we take into account the shutDownAllowed and startUpAllowed flag in the ICS file
        IcsData icsData = IcsData.read(
            getClass().getResourceAsStream("/ics/static_shutdown_startup_true.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList());

        Set<GeneratorConstraints> generatorConstraintsSet = icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1")) );

        assertEquals(1, generatorConstraintsSet.size());
        GeneratorConstraints generatorConstraints = generatorConstraintsSet.iterator().next();
        assertTrue(generatorConstraints.isShutDownAllowed());
        assertTrue(generatorConstraints.isStartUpAllowed());
    }

    @Test
    void testIcsImporterNoShutDown() throws IOException {
        // Check that an error is thrown if the shutDownAllowed flag is not present

        IcsData icsData = IcsData.read(
            getClass().getResourceAsStream("/ics/static_no_shutdown.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList());

        Assertions.assertThatExceptionOfType(OpenRaoException.class)
            .isThrownBy(() -> icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1"))))
            .withMessage("Could not parse shutDownAllowed value for raId Redispatching_RA: ");
    }

    @Test
    void testIcsImporterWrongShutDown() throws IOException {
        IcsData icsData = IcsData.read(
            getClass().getResourceAsStream("/ics/static_wrong_shutdown.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList());

        Assertions.assertThatExceptionOfType(OpenRaoException.class)
            .isThrownBy(() -> icsData.createGeneratorConstraints("Redispatching_RA", Map.of("BBE1AA1", 1.0), Map.of("BBE1AA1", icsData.getGeneratorIdFromRaIdAndNodeId("Redispatching_RA", "BBE1AA1"))))
            .withMessage("Could not parse shutDownAllowed value for raId Redispatching_RA: wrongValue");
    }


    @Test
    void testIcsImporterNoStartUp() {
        double cost = 5.;
        InputStream staticInputStream = IcsImporterTest.class.getResourceAsStream("/ics/static_no_startup.csv");
        InputStream seriesInputStream = IcsImporterTest.class.getResourceAsStream("/ics/series.csv");
        InputStream gskInputStream = IcsImporterTest.class.getResourceAsStream("/glsk/gsk.csv");

        Assertions.assertThatExceptionOfType(OpenRaoException.class)
            .isThrownBy(() -> IcsImporter.populateInputWithICS(timeCoupledRaoInputWithNetworkPaths, staticInputStream, seriesInputStream, gskInputStream, cost, cost))
            .withMessage("Could not parse startUpAllowed value  for raId Redispatching_RA");
    }

    @Test
    void testIcsImporterWrongStartUp() {
        double cost = 5.;
        InputStream staticInputStream = IcsImporterTest.class.getResourceAsStream("/ics/static_wrong_startup.csv");
        InputStream seriesInputStream = IcsImporterTest.class.getResourceAsStream("/ics/series.csv");
        InputStream gskInputStream = IcsImporterTest.class.getResourceAsStream("/glsk/gsk.csv");

        Assertions.assertThatExceptionOfType(OpenRaoException.class)
            .isThrownBy(() -> IcsImporter.populateInputWithICS(timeCoupledRaoInputWithNetworkPaths, staticInputStream, seriesInputStream, gskInputStream, cost, cost))
            .withMessage("Could not parse startUpAllowed value wrongValue for raId Redispatching_RA");
    }
}