/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.PtdfApproximation;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
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
class PrePerimeterSensitivityAnalysisTest {
    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final String OUTAGE_INSTANT_ID = "outage";

    private Network network;
    private Crac crac;
    private FlowCnec cnec;
    private ToolProvider toolProvider;
    private RaoParameters raoParameters;
    private PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis;
    private OptimizationResult optimizationResult;
    private RangeActionSetpointResult rangeActionSetpointResult;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        raoParameters = new RaoParameters();
        cnec = Mockito.mock(FlowCnec.class);

        optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(Map.of(cnec, Map.of(Side.LEFT, 0.1)));
        when(optimizationResult.getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(150.);

        rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);

        rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);

        toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowComputation()).thenReturn(Mockito.mock(LoopFlowComputation.class));
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = Mockito.mock(AbsolutePtdfSumsComputation.class);
        when(absolutePtdfSumsComputation.computeAbsolutePtdfSums(any(), any())).thenReturn(Map.of(cnec, Map.of(Side.LEFT, 0.987)));
        when(toolProvider.getAbsolutePtdfSumsComputation()).thenReturn(absolutePtdfSumsComputation);

        prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider);
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
        assertNotNull(prePerimeterSensitivityAnalysis);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(result.getSensitivityResult());

        result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
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

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result.getSensitivityResult());
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.1)), result.getFlowResult().getPtdfZonalSums());
        assertEquals(150., result.getFlowResult().getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testRunAndRecomputePtdf() {
        raoParameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        raoParameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(result.getSensitivityResult());
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.987)), result.getFlowResult().getPtdfZonalSums());
    }
}
