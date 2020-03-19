/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultPostProcessorTest {

    private static final double DOUBLE_TOLERANCE = 0.05;
    private static final double ANGLE_TAP_APPROX_TOLERANCE = 0.5;

    private LinearRaoData linearRaoData;
    private LinearRaoProblem linearRaoProblem;
    private MPVariable rangeActionSetPoint;
    private MPVariable rangeActionAbsoluteVariation;
    private MPConstraint absoluteRangeActionVariationConstraint;

    @Before
    public void setUp() {
        // arrange input data
        SimpleCrac crac = new SimpleCrac("cracName");
        PstRange rangeAction = new PstWithRange("idPstRa", new NetworkElement("BBE2AA1  BBE3AA1  1"));
        crac.addRangeAction(rangeAction);
        crac.addState(new SimpleState(Optional.empty(), new Instant("preventive", 0)));

        Network network = NetworkImportsUtil.import12NodesNetwork();
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        crac.synchronize(network);

        ResultVariantManager variantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, variantManager);
        variantManager.createVariant("test-variant");

        // Arrange linearRaoData
        linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);

        // Arrange linearRaoProblem
        linearRaoProblem = Mockito.mock(LinearRaoProblem.class);
        rangeActionSetPoint = Mockito.mock(MPVariable.class);
        rangeActionAbsoluteVariation = Mockito.mock(MPVariable.class);
        absoluteRangeActionVariationConstraint = Mockito.mock(MPConstraint.class); // the "current value" of the range action is "stored" in the lb of this constraint
        Mockito.when(linearRaoProblem.getRangeActionSetPointVariable(rangeAction)).thenReturn(rangeActionSetPoint);
        Mockito.when(linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction)).thenReturn(rangeActionAbsoluteVariation);
        Mockito.when(linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE)).thenReturn(absoluteRangeActionVariationConstraint);
    }

    @Test
    public void fillPstResultWithNoActivationAndNeutralRangeAction() {
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.0);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(0.0);

        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        new RaoResultPostProcessor().process(linearRaoProblem, linearRaoData, result, "test-variant");

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        PstRangeResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(PstRangeResultExtension.class);
        PstRangeResult pstRangeResult = pstRangeResultMap.getVariant("test-variant");
        assertTrue(Double.isNaN(pstRangeResult.getSetPoint(preventiveState)));
        assertFalse(pstRangeResult.isActivated(preventiveState));

        assertTrue(result.getPreContingencyResult().getRemedialActionResults().isEmpty());
    }

    @Test
    public void fillPstResultWithNoActivationAndInitialRangeActionSetPoint() {
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(5.0);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(0.0);

        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        new RaoResultPostProcessor().process(linearRaoProblem, linearRaoData, result, "test-variant");

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        PstRangeResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(PstRangeResultExtension.class);
        PstRangeResult pstRangeResult = pstRangeResultMap.getVariant("test-variant");
        assertTrue(Double.isNaN(pstRangeResult.getSetPoint(preventiveState)));
        assertFalse(pstRangeResult.isActivated(preventiveState));

        assertTrue(result.getPreContingencyResult().getRemedialActionResults().isEmpty());
    }

    @Test
    public void fillPstResultWithNegativeActivation() {
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 - 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        new RaoResultPostProcessor().process(linearRaoProblem, linearRaoData, result, "test-variant");

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        PstRangeResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(PstRangeResultExtension.class);
        PstRangeResult pstRangeResult = pstRangeResultMap.getVariant("test-variant");
        assertEquals(-12, pstRangeResult.getTap(preventiveState));
        assertEquals(0.39 - 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);

        assertEquals(1, result.getPreContingencyResult().getRemedialActionResults().size());
        assertEquals("idPstRa", result.getPreContingencyResult().getRemedialActionResults().get(0).getId());
        assertTrue(result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0) instanceof PstElementResult);
        assertEquals(1, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPreOptimisationTapPosition());
        assertEquals(-12, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPostOptimisationTapPosition());
        assertEquals(0.39, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPreOptimisationAngle(), DOUBLE_TOLERANCE);
        assertEquals(0.39 - 5.00, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPostOptimisationAngle(), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithPositiveActivation() {
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        new RaoResultPostProcessor().process(linearRaoProblem, linearRaoData, result, "test-variant");

        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        PstRangeResultExtension pstRangeResultMap = linearRaoData.getCrac().getRangeAction("idPstRa").getExtension(PstRangeResultExtension.class);
        PstRangeResult pstRangeResult = pstRangeResultMap.getVariant("test-variant");
        assertEquals(14, pstRangeResult.getTap(preventiveState));
        assertEquals(0.39 + 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);

        assertEquals(1, result.getPreContingencyResult().getRemedialActionResults().size());
        assertEquals("idPstRa", result.getPreContingencyResult().getRemedialActionResults().get(0).getId());
        assertTrue(result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0) instanceof PstElementResult);
        assertEquals(1, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPreOptimisationTapPosition());
        assertEquals(14, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPostOptimisationTapPosition());
        assertEquals(0.39, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPreOptimisationAngle(), DOUBLE_TOLERANCE);
        assertEquals(0.39 + 5.00, ((PstElementResult) result.getPreContingencyResult().getRemedialActionResults().get(0).getRemedialActionElementResults().get(0)).getPostOptimisationAngle(), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithAngleTooHigh() {
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 99.0); // value out of PST Range
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(99.0);

        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        try {
            new RaoResultPostProcessor().process(linearRaoProblem, linearRaoData, result, "");
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
