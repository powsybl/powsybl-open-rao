/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.loopflowcomputation.LoopFlowResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.AbsolutePtdfSumsComputation;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
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
class PrePerimeterSensitivityAnalysisTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network network;
    private Crac crac;
    private FlowCnec cnec;
    private ToolProvider toolProvider;
    private RaoParameters raoParameters;
    private PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis;
    private OptimizationResult optimizationResult;
    private Map<FlowCnec, Map<TwoSides, Double>> ptdfPerCnec;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        raoParameters = new RaoParameters();
        cnec = Mockito.mock(FlowCnec.class);

        optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(Map.of(cnec, Map.of(TwoSides.ONE, 0.1)));
        when(optimizationResult.getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(150.);

        toolProvider = Mockito.mock(ToolProvider.class);
        LoopFlowComputation loopFlowComputation = Mockito.mock(LoopFlowComputation.class);
        when(toolProvider.getLoopFlowComputation()).thenReturn(loopFlowComputation);
        LoopFlowResult loopFlowResult = Mockito.mock(LoopFlowResult.class);
        when(loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(any(), any(), any())).thenReturn(loopFlowResult);
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        ptdfPerCnec = new HashMap<>();
        crac.getFlowCnecs().forEach(flowCnec -> ptdfPerCnec.put(flowCnec, Map.of(TwoSides.ONE, 0.0, TwoSides.TWO, 0.0)));
        ptdfPerCnec.put(cnec, Map.of(TwoSides.ONE, 0.987));
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(ptdfPerCnec);
        when(toolProvider.getAbsolutePtdfSumsComputation()).thenReturn(absolutePtdfSumsComputation);

        prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider);
    }

    private void mockSystematicSensitivityInterface(boolean withPtdf, boolean withLf) {
        SystematicSensitivityResult sensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityInterface sensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        when(sensitivityInterface.run(network)).thenReturn(sensitivityResult);
        when(sensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        when(sensitivityResult.getStatus(any())).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        when(toolProvider.getSystematicSensitivityInterface(any(), any(), eq(withPtdf), eq(withLf), any(), any())).thenReturn(sensitivityInterface);
    }

    @Test
    void testRunNoPtdfNoLf() {
        assertNotNull(prePerimeterSensitivityAnalysis);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result.getSensitivityResult());

        result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result.getSensitivityResult());
    }

    @Test
    void testRunWithPtdf() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, false);
        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result.getSensitivityResult());
    }

    @Test
    void testRunWithLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result.getSensitivityResult());
    }

    @Test
    void testRunWithPtdfAndLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result.getSensitivityResult());
    }

    @Test
    void testRunWithFixedPtdfAndLf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result.getSensitivityResult());
        assertEquals(Map.of(cnec, Map.of(TwoSides.ONE, 0.1)), result.getFlowResult().getPtdfZonalSums());
        assertEquals(150., result.getFlowResult().getCommercialFlow(cnec, TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testRunAndRecomputePtdf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result.getSensitivityResult());
        assertEquals(ptdfPerCnec, result.getFlowResult().getPtdfZonalSums());
    }
}
