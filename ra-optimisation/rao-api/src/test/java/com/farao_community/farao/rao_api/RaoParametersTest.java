/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.google.auto.service.AutoService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersTest {

    private PlatformConfig config;
    private InMemoryPlatformConfig platformCfg;
    private FileSystem fileSystem;

    @Before
    public void setUp() {
        config = Mockito.mock(PlatformConfig.class);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    public void testExtensions() {
        RaoParameters parameters = new RaoParameters();
        DummyExtension dummyExtension = new DummyExtension();
        parameters.addExtension(DummyExtension.class, dummyExtension);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensions().contains(dummyExtension));
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void testNoExtensions() {
        RaoParameters parameters = new RaoParameters();

        assertEquals(0, parameters.getExtensions().size());
        assertFalse(parameters.getExtensions().contains(new DummyExtension()));
        assertFalse(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkConfig() {

        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("rao-with-loop-flow-limitation", Boolean.toString(false));
        moduleConfig.setStringProperty("loopflow-approximation", Boolean.toString(true));
        moduleConfig.setStringProperty("loopflow-constraint-adjustment-coefficient", Objects.toString(0.0));
        moduleConfig.setStringProperty("loopflow-violation-cost", Objects.toString(0.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertFalse(parameters.isRaoWithLoopFlowLimitation());
    }

    @Test
    public void testExtensionFromConfig() {
        RaoParameters parameters = RaoParameters.load(config);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkMnecConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("mnec-acceptable-margin-diminution", Objects.toString(100.0));
        moduleConfig.setStringProperty("mnec-violation-cost", Objects.toString(5.0));
        moduleConfig.setStringProperty("mnec-constraint-adjustment-coefficient", Objects.toString(0.1));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(100, parameters.getMnecAcceptableMarginDiminution(), 1e-6);
        assertEquals(5, parameters.getMnecViolationCost(), 1e-6);
        assertEquals(0.1, parameters.getMnecConstraintAdjustmentCoefficient(), 1e-6);
    }

    @Test
    public void checkPtdfConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringListProperty("ptdf-boundaries", new ArrayList<>(Arrays.asList("FR:ES", "ES:PT")));
        moduleConfig.setStringProperty("ptdf-sum-lower-bound", Objects.toString(5.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(5, parameters.getPtdfSumLowerBound(), 1e-6);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.FR), new EICode(Country.ES))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.ES), new EICode(Country.PT))));
    }

    @Test
    public void checkRelativeMarginConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("negative-margin-objective-coefficient", Objects.toString(100.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(100, parameters.getNegativeMarginObjectiveCoefficient(), 1e-6);
    }

    @Test
    public void checkLoopFlowConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("loop-flow-approximation", "UPDATE_PTDF_WITH_TOPO");
        moduleConfig.setStringProperty("loop-flow-constraint-adjustment-coefficient", Objects.toString(5.0));
        moduleConfig.setStringProperty("loop-flow-violation-cost", Objects.toString(20.6));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO, parameters.getLoopFlowApproximationLevel());
        assertEquals(5, parameters.getLoopFlowConstraintAdjustmentCoefficient(), 1e-6);
        assertEquals(20.6, parameters.getLoopFlowViolationCost(), 1e-6);
    }

    @Test
    public void checkPerimetersParallelConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("perimeters-in-parallel", Objects.toString(10));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(10, parameters.getPerimetersInParallel());
    }

    @Test
    public void testUpdatePtdfWithTopo() {
        assertFalse(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithTopologicalChange());
    }

    @Test
    public void testUpdatePtdfWithPst() {
        assertFalse(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF.shouldUpdatePtdfWithPstChange());
        assertFalse(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithPstChange());
        assertTrue(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithPstChange());
    }

    private static class DummyExtension extends AbstractExtension<RaoParameters> {

        @Override
        public String getName() {
            return "dummyExtension";
        }
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public static class DummyLoader implements RaoParameters.ConfigLoader<DummyExtension> {

        @Override
        public DummyExtension load(PlatformConfig platformConfig) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummyExtension";
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

    @Test
    public void testSetBoundariesFromCountryCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR:ES", "ES:PT"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.FR), new EICode(Country.ES))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.ES), new EICode(Country.PT))));
    }

    @Test
    public void testSetBoundariesFromEiCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("10YBE----------2:10YFR-RTE------C", "10YBE----------2:22Y201903144---9"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getRelativeMarginPtdfBoundaries().size());
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode("10YBE----------2"), new EICode("10YFR-RTE------C"))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode("10YBE----------2"), new EICode("22Y201903144---9"))));
    }

    @Test
    public void testSetBoundariesFromMixOfCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("BE:FR", "BE:22Y201903144---9", "22Y201903145---4:DE", "22Y201903144---9:22Y201903145---4"));
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
        assertEquals(4, parameters.getRelativeMarginPtdfBoundaries().size());
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.BE), new EICode(Country.FR))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode(Country.BE), new EICode("22Y201903144---9"))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode("22Y201903145---4"), new EICode(Country.DE))));
        assertTrue(parameters.getRelativeMarginPtdfBoundaries().contains(new ImmutablePair<>(new EICode("22Y201903144---9"), new EICode("22Y201903145---4"))));
    }

    private void testWrongBoundary(String boundary) {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = Arrays.asList(boundary);
        parameters.setRelativeMarginPtdfBoundariesFromString(stringBoundaries);
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException1() {
        testWrongBoundary("FRANCE:SPAIN");
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException2() {
        testWrongBoundary("FR:ES:");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBoundariesFromCountryCodesException3() {
        testWrongBoundary("FR:YY");
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException4() {
        testWrongBoundary("FR-BE");
    }

    @Test
    public void testGetBoundariesFromString() {
        RaoParameters parameters = new RaoParameters();
        List<Pair<EICode, EICode>> countryBoundaries = new ArrayList<>(Arrays.asList(new ImmutablePair<>(
                    new EICode(Country.BE), new EICode(Country.FR)),
                    new ImmutablePair<>(new EICode(Country.DE), new EICode("22Y201903145---4")),
                    new ImmutablePair<>(new EICode("22Y201903144---9"), new EICode("22Y201903145---4"))
                    ));

        parameters.setRelativeMarginPtdfBoundaries(countryBoundaries);
        assertEquals(3, parameters.getRelativeMarginPtdfBoundaries().size());
        assertTrue(parameters.getRelativeMarginPtdfBoundariesAsString().contains("10YBE----------2:10YFR-RTE------C"));
        assertTrue(parameters.getRelativeMarginPtdfBoundariesAsString().contains("10YCB-GERMANY--8:22Y201903145---4"));
        assertTrue(parameters.getRelativeMarginPtdfBoundariesAsString().contains("22Y201903144---9:22Y201903145---4"));
    }

    @Test
    public void testRelativePositiveMargins() {
        assertTrue(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertTrue(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT.relativePositiveMargins());
        assertFalse(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertFalse(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT.relativePositiveMargins());
    }
}
