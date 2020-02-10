/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoOptimizerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRaoTest.class);

    private LinearRaoOptimizer linearRaoOptimizer;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private Crac crac;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

        computationManager = LocalComputationManager.getDefault();
        raoParameters = RaoParameters.load(platformConfig);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);

        crac = create();

        Network network = Importers.loadNetwork(
                "TestCase12Nodes.uct",
                getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecMarginMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMarginMap.put(cnec, 1.0));
        Map<Cnec, Double> cnecMaxThresholdMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMaxThresholdMap.put(cnec, 500.));
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = new SystematicSensitivityAnalysisResult(stateSensiMap, cnecMarginMap, cnecMaxThresholdMap);

        LinearRaoProblem linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);

        linearRaoOptimizer = new LinearRaoOptimizer(crac, network, systematicSensitivityAnalysisResult, raoParameters, linearRaoProblemMock);
    }

    @Test
    public void testUpdate() {
        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecMarginMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMarginMap.put(cnec, 3.0));
        Map<Cnec, Double> cnecMaxThresholdMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMaxThresholdMap.put(cnec, 700.0));
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = new SystematicSensitivityAnalysisResult(stateSensiMap, cnecMarginMap, cnecMaxThresholdMap);
        linearRaoOptimizer.update(systematicSensitivityAnalysisResult);
        assertEquals(697, linearRaoOptimizer.getLinearRaoData().getReferenceFlow(crac.getCnecs().iterator().next()), 0.1);
    }

    private static Crac create() {
        Crac crac = new SimpleCrac("idSimpleCracTestUS", "nameSimpleCracTestUS");

        // Instant
        Instant basecase = new Instant("initial", 0);

        //NetworkElement
        NetworkElement monitoredElement1 = new NetworkElement("BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1 name");

        // State
        State stateBasecase = new SimpleState(Optional.empty(), basecase);

        // Thresholds
        AbsoluteFlowThreshold thresholdAbsFlow = new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.IN, 1500);

        // CNECs
        SimpleCnec cnec1basecase = new SimpleCnec("cnec1basecase", "", monitoredElement1, null, stateBasecase);
        cnec1basecase.setThreshold(thresholdAbsFlow);
        crac.addCnec(cnec1basecase);

        // RAs
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstRange("BBE2AA1  BBE3AA1  1", pstElement);
        pstRange.addRange(new Range(-5, 5, RangeType.ABSOLUTE_FIXED, RangeDefinition.CENTERED_ON_ZERO));
        crac.addRangeAction(pstRange);

        return crac;
    }
}
