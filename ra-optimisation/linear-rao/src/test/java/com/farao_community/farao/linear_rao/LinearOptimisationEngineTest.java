/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LinearRaoProblem.class)
public class LinearOptimisationEngineTest {
    private static final double ANGLE_TAP_APPROX_TOLERANCE = 0.5;

    private LinearOptimisationEngine linearOptimisationEngine;
    private LinearRaoProblem linearRaoProblemMock;
    private Network network;
    private SimpleCrac crac;
    private PstRange rangeAction;
    private LinearRaoData linearRaoData;
    private MPVariable rangeActionSetPoint;
    private MPVariable rangeActionAbsoluteVariation;
    private MPConstraint absoluteRangeActionVariationConstraint;
    private LinearRaoParameters linearRaoParameters;

    @Before
    public void setUp() {
        // RaoParameters
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);

        linearOptimisationEngine = Mockito.spy(new LinearOptimisationEngine(raoParameters));

        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);
        Mockito.when(linearRaoProblemMock.addMinimumMarginConstraint(Mockito.anyDouble(), Mockito.anyDouble(), any(), any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.addFlowConstraint(Mockito.anyDouble(), Mockito.anyDouble(), any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.getFlowConstraint(any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearRaoProblemMock.getFlowVariable(any())).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearRaoProblemMock.getMinimumMarginVariable()).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearRaoProblemMock.getObjective()).thenReturn(Mockito.mock(MPObjective.class));
        Mockito.doReturn(linearRaoProblemMock).when(linearOptimisationEngine).createLinearRaoProblem();

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);

        rangeAction = new PstWithRange("idPstRa", new NetworkElement("BBE2AA1  BBE3AA1  1"));

        linearRaoData = new LinearRaoData(network, crac);
        linearRaoData = Mockito.spy(linearRaoData);

        SystematicSensitivityAnalysisResult result = createSystematicAnalysisResult();
        linearRaoData.setSystematicSensitivityAnalysisResult(result);

        rangeActionSetPoint = Mockito.mock(MPVariable.class);
        rangeActionAbsoluteVariation = Mockito.mock(MPVariable.class);
        absoluteRangeActionVariationConstraint = Mockito.mock(MPConstraint.class);
        linearRaoParameters = new LinearRaoParameters();
    }

    private SystematicSensitivityAnalysisResult createSystematicAnalysisResult() {
        SystematicSensitivityAnalysisResult result = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        Mockito.when(result.isSuccess()).thenReturn(true);
        crac.getCnecs().forEach(cnec -> {
            Mockito.when(result.getReferenceFlow(cnec)).thenReturn(499.);
            Mockito.when(result.getReferenceIntensity(cnec)).thenReturn(11.);
            crac.getRangeActions().forEach(rangeAction -> {
                Mockito.when(result.getSensitivityOnFlow(rangeAction, cnec)).thenReturn(42.);
                Mockito.when(result.getSensitivityOnIntensity(rangeAction, cnec)).thenReturn(-42.);
            });
        });
        return result;
    }

    @Test
    public void testOptimalAndUpdate() {
        linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
        assertNotNull(linearRaoData);
        linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
        assertNotNull(linearRaoData);
    }

    @Test
    public void testNonOptimal() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.ABNORMAL);
        try {
            linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
        } catch (LinearOptimisationException e) {
            assertEquals("Solving of the linear problem failed failed with MPSolver status ABNORMAL", e.getCause().getMessage());
        }
    }

    @Test
    public void testFillerError() {
        Mockito.when(linearRaoProblemMock.getObjective()).thenReturn(null);
        try {
            linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when building the problem.", e.getMessage());
        }
    }

    @Test
    public void testUpdateError() {
        linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
        Mockito.when(linearRaoProblemMock.getFlowConstraint(any())).thenReturn(null);
        try {
            linearOptimisationEngine.run(linearRaoData, linearRaoParameters);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when updating the problem.", e.getMessage());
        }
    }

    private void setUpForFillCracResults() {
        crac = CommonCracCreation.create();
        crac.addRangeAction(rangeAction);
        crac.synchronize(network);
        linearRaoData = new LinearRaoData(network, crac);
        Mockito.when(linearRaoProblemMock.getRangeActionSetPointVariable(rangeAction)).thenReturn(rangeActionSetPoint);
        Mockito.when(linearRaoProblemMock.getAbsoluteRangeActionVariationVariable(rangeAction)).thenReturn(rangeActionAbsoluteVariation);
        Mockito.when(linearRaoProblemMock.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE)).thenReturn(absoluteRangeActionVariationConstraint);
    }

    @Test
    public void fillPstResultWithNoActivationAndNeutralRangeAction() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.0);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(0.0);

        LinearOptimisationEngine.fillCracResults(linearRaoProblemMock, linearRaoData);

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(linearRaoData.getWorkingVariantId());
        assertEquals(0, pstRangeResult.getSetPoint(preventiveState), 0.1);
        assertTrue(pstRangeResult.isActivated(preventiveState));
    }

    @Test
    public void fillPstResultWithNegativeActivation() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 - 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        LinearOptimisationEngine.fillCracResults(linearRaoProblemMock, linearRaoData);

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(linearRaoData.getWorkingVariantId());
        assertEquals(-12, pstRangeResult.getTap(preventiveState));
        assertEquals(0.39 - 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithPositiveActivation() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        LinearOptimisationEngine.fillCracResults(linearRaoProblemMock, linearRaoData);

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(linearRaoData.getWorkingVariantId());
        assertEquals(14, pstRangeResult.getTap(preventiveState));
        assertEquals(0.39 + 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithAngleTooHigh() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 99.0); // value out of PST Range
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(99.0);

        try {
            LinearOptimisationEngine.fillCracResults(linearRaoProblemMock, linearRaoData);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void fillVirtualCostTest() {
        setUpForFillCracResults();
        for (Cnec cnec : linearRaoData.getCrac().getCnecs(linearRaoData.getCrac().getPreventiveState())) {
            CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
            cnecLoopFlowExtension.setLoopFlowConstraint(100.0);
            cnec.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        }

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoParameters.setLoopflowViolationCost(10.0);
        linearRaoParameters.setExtendable(raoParameters);

        MPVariable variableMock = Mockito.mock(MPVariable.class);
        Mockito.when(variableMock.solutionValue()).thenReturn(2.0);
        Mockito.when(linearRaoProblemMock.getLoopflowBreachVariable(any())).thenReturn(variableMock);

        LinearOptimisationEngine.fillVirtualCostInCracResult(linearRaoProblemMock, linearRaoData, linearRaoParameters);
        assertEquals(40.0, linearRaoData.getCracResult().getVirtualCost(), 0.1);
    }
}
