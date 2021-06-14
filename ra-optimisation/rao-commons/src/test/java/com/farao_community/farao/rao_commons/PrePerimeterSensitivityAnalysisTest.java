/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.*;
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
    private FlowCnec cnec;
    private ToolProvider toolProvider;
    private RaoParameters raoParameters;
    private PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis;
    private OptimizationResult optimizationResult;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        raoParameters = new RaoParameters();
        raoParameters.setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF);

        cnec = Mockito.mock(FlowCnec.class);

        optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(Map.of(cnec, 0.1));
        when(optimizationResult.getCommercialFlow(cnec, Unit.MEGAWATT)).thenReturn(150.);

        toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowComputation()).thenReturn(Mockito.mock(LoopFlowComputation.class));
        when(toolProvider.getAbsolutePtdfSumsComputation()).thenReturn(Mockito.mock(AbsolutePtdfSumsComputation.class));

        prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(crac.getRangeActions(), crac.getFlowCnecs(), toolProvider, raoParameters);
    }

    private void mockSystematicSensitivityInterface(boolean withPtdf, boolean withLf) {
        SystematicSensitivityResult sensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityInterface sensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        when(sensitivityInterface.run(network)).thenReturn(sensitivityResult);
        when(toolProvider.getSystematicSensitivityInterface(any(), any(), eq(withPtdf), eq(withLf))).thenReturn(sensitivityInterface);
    }

    @Test
    public void testRunNoPtdfNoLf() {
        assertNotNull(prePerimeterSensitivityAnalysis);
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.run(network);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());

        result = prePerimeterSensitivityAnalysis.runBasedOn(network, optimizationResult);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
    }

    @Test
    public void testRunWithPtdf() {
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.run(network);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
    }

    @Test
    public void testRunWithLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.run(network);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
    }

    @Test
    public void testRunWithPtdfAndLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(true, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.run(network);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
    }

    @Test
    public void testRunWithFixedPtdfAndLf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, false);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOn(network, optimizationResult);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
        assertEquals(Map.of(cnec, 0.1), ((PrePerimeterSensitivityOutput) result).getBranchResult().getPtdfZonalSums());
        assertEquals(150., ((PrePerimeterSensitivityOutput) result).getBranchResult().getCommercialFlow(cnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRunWithFixedPtdf() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO);
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        mockSystematicSensitivityInterface(false, true);

        PrePerimeterResult result = prePerimeterSensitivityAnalysis.runBasedOn(network, optimizationResult);
        assertNotNull(((PrePerimeterSensitivityOutput) result).getSensitivityResult());
        assertEquals(Map.of(cnec, 0.1), ((PrePerimeterSensitivityOutput) result).getBranchResult().getPtdfZonalSums());
    }
}
