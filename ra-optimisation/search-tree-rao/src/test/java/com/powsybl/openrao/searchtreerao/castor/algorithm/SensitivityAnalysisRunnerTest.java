/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.AbsolutePtdfSumsComputation;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionResult;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SensitivityAnalysisRunnerTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network network;
    private Crac crac;
    private FlowCnec cnec;
    private ToolProvider toolProvider;
    private RaoParameters raoParameters;
    private SensitivityAnalysisRunner sensitivityAnalysisRunner;
    private OptimizationResult optimizationResult;
    private RangeActionResult rangeActionResult;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        outageInstant = crac.getInstant("outage");
        raoParameters = new RaoParameters();
        cnec = Mockito.mock(FlowCnec.class);

        optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(Map.of(cnec, Map.of(Side.LEFT, 0.1)));
        when(optimizationResult.getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(150.);

        rangeActionResult = Mockito.mock(RangeActionResult.class);

        rangeActionResult = Mockito.mock(RangeActionResult.class);

        toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowComputation()).thenReturn(Mockito.mock(LoopFlowComputation.class));
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(Map.of(cnec, Map.of(Side.LEFT, 0.987)));
        when(toolProvider.getAbsolutePtdfSumsComputation()).thenReturn(absolutePtdfSumsComputation);

        sensitivityAnalysisRunner = new SensitivityAnalysisRunner(crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider);
    }

    private void mockSystematicSensitivityInterface(boolean withPtdf, boolean withLf) {
        SystematicSensitivityResult sensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityInterface sensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        when(sensitivityInterface.run(network)).thenReturn(sensitivityResult);
        when(sensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        when(toolProvider.getSystematicSensitivityInterface(any(), any(), eq(withPtdf), eq(withLf), any(), any())).thenReturn(sensitivityInterface);
    }

    @Test
    void testRunNoPtdfNoLf() {
        assertNotNull(sensitivityAnalysisRunner);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result);
    }

    @Test
    void testRunWithPtdf() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, false);
        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result);
    }

    @Test
    void testRunWithLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result);
    }

    @Test
    void testRunWithPtdfAndLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result);
    }

    @Test
    void testRunWithFixedPtdfAndLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result);
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.1)), result.getPtdfZonalSums());
        assertEquals(150., result.getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testRunAndRecomputePtdf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PerimeterResultWithCnecs result = sensitivityAnalysisRunner.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result);
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.987)), result.getPtdfZonalSums());
    }
}
