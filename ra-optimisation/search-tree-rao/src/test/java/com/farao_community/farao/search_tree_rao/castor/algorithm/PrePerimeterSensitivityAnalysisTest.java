/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.AbsolutePtdfSumsComputation;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.PrePerimeterSensitivityResultImpl;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PrePerimeterSensitivityAnalysisTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network network;
    private Crac crac;
    private FlowCnec cnec;
    private ToolProvider toolProvider;
    private RaoParameters raoParameters;
    private PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis;
    private OptimizationResult optimizationResult;
    private RangeActionSetpointResult rangeActionSetpointResult;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        raoParameters.setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF);

        cnec = Mockito.mock(FlowCnec.class);

        optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(Map.of(cnec, Map.of(Side.LEFT, 0.1)));
        when(optimizationResult.getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(150.);

        rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);

        rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);

        toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowComputation()).thenReturn(Mockito.mock(LoopFlowComputation.class));
        when(toolProvider.getAbsolutePtdfSumsComputation()).thenReturn(Mockito.mock(AbsolutePtdfSumsComputation.class));

        prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider);
    }

    private void mockSystematicSensitivityInterface(boolean withPtdf, boolean withLf) {
        SystematicSensitivityResult sensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityInterface sensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        when(sensitivityInterface.run(network)).thenReturn(sensitivityResult);
        when(sensitivityResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        when(toolProvider.getSystematicSensitivityInterface(any(), any(), eq(withPtdf), eq(withLf), any())).thenReturn(sensitivityInterface);
    }

    @Test
    public void testRunNoPtdfNoLf() {
        assertNotNull(prePerimeterSensitivityAnalysis);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());

        result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
    }

    @Test
    public void testRunWithPtdf() {
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
    }

    @Test
    public void testRunWithLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
    }

    @Test
    public void testRunWithPtdfAndLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, crac);
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
    }

    @Test
    public void testRunWithFixedPtdfAndLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.1)), ((PrePerimeterSensitivityResultImpl) result).getFlowResult().getPtdfZonalSums());
        assertEquals(150., ((PrePerimeterSensitivityResultImpl) result).getFlowResult().getCommercialFlow(cnec, Side.LEFT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRunWithFixedPtdf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, optimizationResult, rangeActionSetpointResult, Collections.emptySet(), new AppliedRemedialActions());
        assertNotNull(((PrePerimeterSensitivityResultImpl) result).getSensitivityResult());
        assertEquals(Map.of(cnec, Map.of(Side.LEFT, 0.1)), ((PrePerimeterSensitivityResultImpl) result).getFlowResult().getPtdfZonalSums());
    }
}
