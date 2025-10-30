/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.util;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.RaoInputWithNetworkPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class IcsImporterTest {
    private static final double DOUBLE_EPSILON = 1e-6;
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir") + File.separator;
    private String networkFilePathPostIcsImport1;
    private String networkFilePathPostIcsImport2;
    private InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths;
    private Network network1;
    private Network network2;
    private Crac crac1;
    private Crac crac2;

    @BeforeEach
    void setUp() throws IOException {
        // we need to import twice the network to avoid variant names conflicts on the same network object
        String networkFilePath1 = "2Nodes2ParallelLinesPST_0030.uct";
        String networkFilePath2 = "2Nodes2ParallelLinesPST_0130.uct";
        Network network1 = Network.read(networkFilePath1, IcsImporterTest.class.getResourceAsStream("/network/" + networkFilePath1));
        Network network2 = Network.read(networkFilePath2, IcsImporterTest.class.getResourceAsStream("/network/" + networkFilePath2));

        // Create postIcsNetwork:
        networkFilePathPostIcsImport1 = TMP_DIR + networkFilePath1.split(".uct")[0].concat("_modified.jiidm");
        networkFilePathPostIcsImport2 = TMP_DIR + networkFilePath2.split(".uct")[0].concat("_modified.jiidm");

        crac1 = Crac.read("/crac/crac-0030.json", IcsImporterTest.class.getResourceAsStream("/crac/crac-0030.json"), network1);
        crac2 = Crac.read("/crac/crac-0130.json", IcsImporterTest.class.getResourceAsStream("/crac/crac-0130.json"), network2);

        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 13, 0, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 13, 1, 30, 0, 0, ZoneOffset.UTC);

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>(
            Map.of(
                timestamp1, RaoInputWithNetworkPaths.build(getResourcePath("network/" + networkFilePath1), networkFilePathPostIcsImport1, crac1).build(),
                timestamp2, RaoInputWithNetworkPaths.build(getResourcePath("network/" + networkFilePath2), networkFilePathPostIcsImport2, crac2).build()
            ));

        interTemporalRaoInputWithNetworkPaths = new InterTemporalRaoInputWithNetworkPaths(raoInputs, new IntertemporalConstraints());
    }

    private String getResourcePath(String resourcePath) {
        return "src/test/resources/" + resourcePath;
    }

    @AfterEach
    void tearDown() {
        // Clean created networks
        File file = new File(Path.of(networkFilePathPostIcsImport1).toUri());
        if (file.exists()) {
            file.delete();
        }
        file = new File(Path.of(networkFilePathPostIcsImport2).toUri());
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testIcsImporterOneAction() throws IOException {
        double cost = 5.;
        InputStream staticInputStream = IcsImporterTest.class.getResourceAsStream("/ics/static.csv");
        InputStream seriesInputStream = IcsImporterTest.class.getResourceAsStream("/ics/series.csv");
        InputStream gskInputStream = IcsImporterTest.class.getResourceAsStream("/glsk/gsk.csv");
        IcsImporter.populateInputWithICS(interTemporalRaoInputWithNetworkPaths, staticInputStream, seriesInputStream, gskInputStream, cost, cost);

        assertEquals(1, interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().size());
        GeneratorConstraints generatorConstraints = interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().iterator().next();
        assertEquals("Redispatching_RA_BBE1AA1_GENERATOR", generatorConstraints.getGeneratorId());
        assertTrue(generatorConstraints.getDownwardPowerGradient().isPresent());
        assertEquals(-10., generatorConstraints.getDownwardPowerGradient().get(), DOUBLE_EPSILON);
        assertTrue(generatorConstraints.getUpwardPowerGradient().isPresent());
        assertEquals(10., generatorConstraints.getUpwardPowerGradient().get(), DOUBLE_EPSILON);

        assertEquals(1, crac1.getInjectionRangeActions().size());
        InjectionRangeAction ra1 = crac1.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra1.getId());
        assertEquals(116., ra1.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);
        Network network1 = Network.read(networkFilePathPostIcsImport1);
        Generator generator1 = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116., generator1.getTargetP(), DOUBLE_EPSILON);

        assertEquals(1, crac2.getInjectionRangeActions().size());
        InjectionRangeAction ra2 = crac2.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra2.getId());
        assertEquals(120., ra2.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra2.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra2.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra2.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra2.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);
        Network network2 = Network.read(networkFilePathPostIcsImport2);
        Generator generator2 = network2.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(120., generator2.getTargetP(), DOUBLE_EPSILON);
    }

    @Test
    void testIcsImporterGradientNotOk() throws IOException {
        double cost = 5.;
        InputStream staticInputStream = IcsImporterTest.class.getResourceAsStream("/ics/static.csv");
        InputStream seriesInputStream = IcsImporterTest.class.getResourceAsStream("/ics/series_gradient_not_ok.csv");
        InputStream gskInputStream = IcsImporterTest.class.getResourceAsStream("/glsk/gsk.csv");
        IcsImporter.populateInputWithICS(interTemporalRaoInputWithNetworkPaths, staticInputStream, seriesInputStream, gskInputStream, cost, cost);

        assertEquals(0, interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().size());
        assertEquals(0, crac1.getInjectionRangeActions().size());
        assertEquals(0, crac2.getInjectionRangeActions().size());
    }

    @Test
    void testIcsImporterWithGSK() throws IOException {
        double cost = 5.;
        InputStream staticInputStream = IcsImporterTest.class.getResourceAsStream("/ics/static_with_gsk.csv");
        InputStream seriesInputStream = IcsImporterTest.class.getResourceAsStream("/ics/series.csv");
        InputStream gskInputStream = IcsImporterTest.class.getResourceAsStream("/glsk/gsk.csv");
        IcsImporter.populateInputWithICS(interTemporalRaoInputWithNetworkPaths, staticInputStream, seriesInputStream, gskInputStream, cost, cost);

        assertEquals(2, interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().size());
        GeneratorConstraints generatorConstraintsBE = interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().stream().filter(gc -> gc.getGeneratorId().contains("BE")).findFirst().orElseThrow();
        assertEquals("Redispatching_RA_BBE1AA1_GENERATOR", generatorConstraintsBE.getGeneratorId());
        assertEquals(-6., generatorConstraintsBE.getDownwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);
        assertEquals(6., generatorConstraintsBE.getUpwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);
        GeneratorConstraints generatorConstraintsFR = interTemporalRaoInputWithNetworkPaths.getIntertemporalConstraints().getGeneratorConstraints().stream().filter(gc -> gc.getGeneratorId().contains("FR")).findFirst().orElseThrow();
        assertEquals("Redispatching_RA_FFR1AA1_GENERATOR", generatorConstraintsFR.getGeneratorId());
        assertEquals(-4., generatorConstraintsFR.getDownwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);
        assertEquals(4., generatorConstraintsFR.getUpwardPowerGradient().orElseThrow(), DOUBLE_EPSILON);

        assertEquals(1, crac1.getInjectionRangeActions().size());
        InjectionRangeAction ra1 = crac1.getInjectionRangeActions().iterator().next();
        assertEquals("Redispatching_RA_RD", ra1.getId());
        assertEquals(116., ra1.getInitialSetpoint(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.UP).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.UP).get(), DOUBLE_EPSILON);
        assertTrue(ra1.getVariationCost(VariationDirection.DOWN).isPresent());
        assertEquals(5., ra1.getVariationCost(VariationDirection.DOWN).get(), DOUBLE_EPSILON);

        Network network1 = Network.read(networkFilePathPostIcsImport1);
        Generator generatorBE = network1.getGenerator("Redispatching_RA_BBE1AA1_GENERATOR");
        assertEquals(116. * 0.6, generatorBE.getTargetP(), DOUBLE_EPSILON);
        Generator generatorFR = network1.getGenerator("Redispatching_RA_FFR1AA1_GENERATOR");
        assertEquals(116. * 0.4, generatorFR.getTargetP(), DOUBLE_EPSILON);
    }

}
