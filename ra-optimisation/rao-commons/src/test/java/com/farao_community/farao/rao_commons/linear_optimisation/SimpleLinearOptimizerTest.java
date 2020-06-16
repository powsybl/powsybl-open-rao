/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.mocks.MPSolverMock;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblem;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LinearProblem.class)
public class SimpleLinearOptimizerTest {
    private static final double ANGLE_TAP_APPROX_TOLERANCE = 0.5;

    private SimpleLinearOptimizer simpleLinearOptimizer;
    private LinearProblem linearProblemMock;
    private Network network;
    private SimpleCrac crac;
    private PstRange rangeAction;
    private RaoData raoData;
    private MPVariable rangeActionSetPoint;
    private MPVariable rangeActionAbsoluteVariation;
    private MPConstraint absoluteRangeActionVariationConstraint;

    @Before
    public void setUp() {
        simpleLinearOptimizer = Mockito.spy(new SimpleLinearOptimizer(new RaoParameters()));

        linearProblemMock = Mockito.mock(LinearProblem.class);
        Mockito.when(linearProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);
        Mockito.when(linearProblemMock.addMinimumMarginConstraint(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.addFlowConstraint(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.getFlowConstraint(Mockito.any())).thenReturn(Mockito.mock(MPConstraint.class));
        Mockito.when(linearProblemMock.getFlowVariable(Mockito.any())).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearProblemMock.getMinimumMarginVariable()).thenReturn(Mockito.mock(MPVariable.class));
        Mockito.when(linearProblemMock.getObjective()).thenReturn(Mockito.mock(MPObjective.class));
        Mockito.doReturn(linearProblemMock).when(simpleLinearOptimizer).createLinearRaoProblem();

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);

        rangeAction = new PstWithRange("idPstRa", new NetworkElement("BBE2AA1  BBE3AA1  1"));

        raoData = new RaoData(network, crac);
        raoData = Mockito.spy(raoData);

        SystematicSensitivityAnalysisResult result = createSystematicAnalysisResult();
        raoData.setSystematicSensitivityAnalysisResult(result);

        rangeActionSetPoint = Mockito.mock(MPVariable.class);
        rangeActionAbsoluteVariation = Mockito.mock(MPVariable.class);
        absoluteRangeActionVariationConstraint = Mockito.mock(MPConstraint.class);
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
        simpleLinearOptimizer.optimize(raoData);
        assertNotNull(raoData);
        simpleLinearOptimizer.optimize(raoData);
        assertNotNull(raoData);
    }

    @Test
    public void testNonOptimal() {
        Mockito.when(linearProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.ABNORMAL);
        try {
            simpleLinearOptimizer.optimize(raoData);
        } catch (LinearOptimisationException e) {
            assertEquals("Solving of the linear problem failed failed with MPSolver status ABNORMAL", e.getCause().getMessage());
        }
    }

    @Test
    public void testFillerError() {
        Mockito.when(linearProblemMock.getObjective()).thenReturn(null);
        try {
            simpleLinearOptimizer.optimize(raoData);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when building the problem.", e.getMessage());
        }
    }

    @Test
    public void testUpdateError() {
        simpleLinearOptimizer.optimize(raoData);
        Mockito.when(linearProblemMock.getFlowConstraint(Mockito.any())).thenReturn(null);
        try {
            simpleLinearOptimizer.optimize(raoData);
            fail();
        } catch (LinearOptimisationException e) {
            assertEquals("Linear optimisation failed when updating the problem.", e.getMessage());
        }
    }

    private void setUpForFillCracResults() {
        crac = CommonCracCreation.create();
        crac.addRangeAction(rangeAction);
        crac.synchronize(network);
        raoData = new RaoData(network, crac);
        Mockito.when(linearProblemMock.getRangeActionSetPointVariable(rangeAction)).thenReturn(rangeActionSetPoint);
        Mockito.when(linearProblemMock.getAbsoluteRangeActionVariationVariable(rangeAction)).thenReturn(rangeActionAbsoluteVariation);
        Mockito.when(linearProblemMock.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE)).thenReturn(absoluteRangeActionVariationConstraint);
    }

    @Test
    public void fillPstResultWithNoActivationAndNeutralRangeAction() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.0);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(0.0);

        raoData.getRaoDataManager().fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(0, pstRangeResult.getSetPoint(preventiveState), 0.1);
        assertTrue(pstRangeResult.isActivated(preventiveState));
    }

    @Test
    public void fillPstResultWithNegativeActivation() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 - 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        raoData.getRaoDataManager().fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(-12, pstRangeResult.getTap(preventiveState));
        Assert.assertEquals(0.39 - 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithPositiveActivation() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        raoData.getRaoDataManager().fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(14, pstRangeResult.getTap(preventiveState));
        Assert.assertEquals(0.39 + 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithAngleTooHigh() {
        setUpForFillCracResults();
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 99.0); // value out of PST Range
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(99.0);

        try {
            raoData.getRaoDataManager().fillRangeActionResultsWithLinearProblem(linearProblemMock);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
