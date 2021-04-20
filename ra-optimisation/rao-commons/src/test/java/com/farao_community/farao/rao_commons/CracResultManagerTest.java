/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*


package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.AbstractRangeAction;
import com.farao_community.farao.data.crac_impl.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdImpl;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;

*/
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 *//*

public class CracResultManagerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private RaoData raoData;
    private LoopFlowResult loopFlowResult;

    @Before
    public void setUp() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();

        LoopFlowThresholdImpl cnecLoopFlowExtension1 = new LoopFlowThresholdImpl(100., Unit.MEGAWATT);
        LoopFlowThresholdImpl cnecLoopFlowExtension2 = new LoopFlowThresholdImpl(100., Unit.MEGAWATT);

        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, cnecLoopFlowExtension1);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, cnecLoopFlowExtension2);
        crac.getBranchCnec("cnec2basecase").setReliabilityMargin(20);

        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());

        loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(crac.getBranchCnec("cnec1basecase"), -252, 128., -124.);
        loopFlowResult.addCnecResult(crac.getBranchCnec("cnec2basecase"), 24., 45., 69.);
    }

    @Test
    public void testFillCracResultsWithLoopFlows() {
        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(loopFlowResult);
        String var = raoData.getWorkingVariantId();

        assertEquals(-252., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(24, raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100. - 20., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(128., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(45., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFillCracResultsWithLoopFlowApproximation() {

        SystematicSensitivityResult sensiResults = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getBranchCnec("cnec1basecase"))).thenReturn(-162.);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getBranchCnec("cnec2basecase"))).thenReturn(47.);

        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(loopFlowResult);
        raoData.setSystematicSensitivityResult(sensiResults);
        raoData.getCracResultManager().fillCnecResultsWithApproximatedLoopFlows();
        String var = raoData.getWorkingVariantId();

        assertEquals(-162 - 128., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(47. - 45., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100. - 20., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCopyCommercialFlowsBetweenVariants() {
        raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setCommercialFlowInMW(150.6);
        raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setCommercialFlowInMW(653.7);
        String var = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracResultManager().copyCommercialFlowsBetweenVariants(raoData.getPreOptimVariantId(), var);
        assertEquals(150.6, raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(653.7, raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
    }

    private void setUpPstTesting() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();
        crac.synchronize(network);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        raoData.setSystematicSensitivityResult(getMockSensiResult(crac));
    }

    private SystematicSensitivityResult getMockSensiResult(Crac crac) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(100.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(200.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(300.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2basecase"))).thenReturn(-400.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(-500.);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(-600.);

        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1basecase"))).thenReturn(10.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(-20.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(30.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2basecase"))).thenReturn(-40.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(50.);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(-60.);

        return sensisResults;
    }

    @Test
    public void testComputeMinMargins() {
        setUpPstTesting();
        Crac crac = raoData.getCrac();
        PstRangeAction pstRangeAction = (PstRangeAction) crac.getRangeAction("pst");
        CracResultManager cracResultManager = raoData.getCracResultManager();
        Pair<Double, Double> margins;

        // Margins on cnec1basecase
        // margin1 = 1500 - (100 + 3 * 10) = 1370
        // margin2 = margin1 - 10 = 1360
        margins = cracResultManager.computeMinMargins(pstRangeAction, List.of(crac.getBranchCnec("cnec1basecase")), 3, 4);
        assertEquals(1370, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(1360, margins.getRight(), DOUBLE_TOLERANCE);
        // margin1 = 1500 - (100 - 5 * 10) = 1450
        // margin2 = margin1 + 10 = 1460
        margins = cracResultManager.computeMinMargins(pstRangeAction, List.of(crac.getBranchCnec("cnec1basecase")), -5, -6);
        assertEquals(1450, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(1460, margins.getRight(), DOUBLE_TOLERANCE);

        // Margins on cnec2stateCurativeContingency2
        // margin1 = -600 + 3 * (-60) + 987 = 207
        // margin2 = -600 + 4 * (-60) + 987 = 147
        margins = cracResultManager.computeMinMargins(pstRangeAction, List.of(crac.getBranchCnec("cnec2stateCurativeContingency2")), 3, 4);
        assertEquals(207.3, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(147.3, margins.getRight(), DOUBLE_TOLERANCE);
        // margin1 = -600 + 5 * 60 + 987 = 687
        // margin2 = -600 + 6 * 60 + 987 = 747
        margins = cracResultManager.computeMinMargins(pstRangeAction, List.of(crac.getBranchCnec("cnec2stateCurativeContingency2")), -5, -6);
        assertEquals(687.3, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(747.3, margins.getRight(), DOUBLE_TOLERANCE);

        // Margins on all cnecs
        // cnec2stateCurativeContingency2 is the worst
        margins = cracResultManager.computeMinMargins(pstRangeAction, new ArrayList<>(crac.getFlowCnecs()), 3, 4);
        assertEquals(207.3, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(147.3, margins.getRight(), DOUBLE_TOLERANCE);
        // cnec2stateCurativeContingency1 is the worst
        // margin1 = -500 - 5 * 50 + 987 = 237
        // margin2 = -500 - 6 * 50 + 987 = 187
        margins = cracResultManager.computeMinMargins(pstRangeAction, new ArrayList<>(crac.getFlowCnecs()), -5, -6);
        assertEquals(237.3, margins.getLeft(), DOUBLE_TOLERANCE);
        assertEquals(187.3, margins.getRight(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMinMarginsForBestTapsAtLimits() {
        setUpPstTesting();
        Crac crac = raoData.getCrac();
        PstRangeAction pstRangeAction = (PstRangeAction) crac.getRangeAction("pst");
        CracResultManager cracResultManager = raoData.getCracResultManager();
        Map<Integer, Double> minMarginsForBestTaps;
        BranchCnec cnec = crac.getBranchCnec("cnec2stateCurativeContingency2");
        List<BranchCnec> cnecList = List.of(cnec);

        Mockito.when(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec)).thenReturn(1400.);
        Mockito.when(raoData.getSystematicSensitivityResult().getSensitivityOnFlow(pstRangeAction, cnec)).thenReturn(-100.);

        // PST at tap 15, close to 16 which would improve margin on cnec
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 6, cnecList);
        assertEquals(2, minMarginsForBestTaps.size());
        assertEquals(171.2, minMarginsForBestTaps.get(15), DOUBLE_TOLERANCE);
        assertEquals(210.0, minMarginsForBestTaps.get(16), DOUBLE_TOLERANCE);

        // PST at tap 16, close to limit (non existent 17 which would improve margin on cnec)
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 6.22764253616333, cnecList);
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(16), DOUBLE_TOLERANCE);

        Mockito.when(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec)).thenReturn(-1400.);

        // PST at tap -15, close to -16 which would improve margin on cnec
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, -6, cnecList);
        assertEquals(2, minMarginsForBestTaps.size());
        assertEquals(171.2, minMarginsForBestTaps.get(-15), DOUBLE_TOLERANCE);
        assertEquals(210.0, minMarginsForBestTaps.get(-16), DOUBLE_TOLERANCE);

        // PST at tap -16, close to limit (non existent -17 which would improve margin on cnec)
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, -6.22764253616333, cnecList);
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(-16), DOUBLE_TOLERANCE);

        Mockito.when(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec)).thenReturn(0.);
        Mockito.when(raoData.getSystematicSensitivityResult().getSensitivityOnFlow(pstRangeAction, cnec)).thenReturn(100.);

        // PST at tap 16, close to 15 which would improve margin on cnec
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 6.05, cnecList);
        assertEquals(2, minMarginsForBestTaps.size());
        assertEquals(403.4, minMarginsForBestTaps.get(15), DOUBLE_TOLERANCE);
        assertEquals(364.5, minMarginsForBestTaps.get(16), DOUBLE_TOLERANCE);

        Mockito.when(raoData.getSystematicSensitivityResult().getSensitivityOnFlow(pstRangeAction, cnec)).thenReturn(-100.);

        // PST at tap -16, close to -15 which would improve margin on cnec
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, -6.05, cnecList);
        assertEquals(2, minMarginsForBestTaps.size());
        assertEquals(403.4, minMarginsForBestTaps.get(-15), DOUBLE_TOLERANCE);
        assertEquals(364.5, minMarginsForBestTaps.get(-16), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeMinMarginsForBestTaps() {
        setUpPstTesting();
        Crac crac = raoData.getCrac();
        PstRangeAction pstRangeAction = (PstRangeAction) crac.getRangeAction("pst");
        CracResultManager cracResultManager = raoData.getCracResultManager();
        Map<Integer, Double> minMarginsForBestTaps;

        // between taps 3 and 4 for cnec1basecase, at tap 3, far from limit
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 1.18, List.of(crac.getBranchCnec("cnec1basecase")));
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(3), DOUBLE_TOLERANCE);
        // between taps 3 and 4 for cnec1basecase, at tap 4, far from limit
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 1.54, List.of(crac.getBranchCnec("cnec1basecase")));
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(4), DOUBLE_TOLERANCE);
        // between taps 3 and 4 for cnec1basecase, at tap 4, close to limit but margin increase at tap 3 is small
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 1.37, List.of(crac.getBranchCnec("cnec1basecase")));
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(4), DOUBLE_TOLERANCE);

        // between taps 11 and 12 for all cnecs, at tap 11, close to limit with tap 12
        // but tap 12 has a worse margin
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 4.46, new ArrayList<>(crac.getFlowCnecs()));
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(11), DOUBLE_TOLERANCE);
        // between taps 10 and 11 for all cnecs, at tap 11, close to limit with tap 10
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 4.147, new ArrayList<>(crac.getFlowCnecs()));
        assertEquals(2, minMarginsForBestTaps.size());
        assertEquals(153.6, minMarginsForBestTaps.get(10), DOUBLE_TOLERANCE);
        assertEquals(130.2, minMarginsForBestTaps.get(11), DOUBLE_TOLERANCE);
        // between taps 10 and 11 for all cnecs, at tap 11, far from limit with tap 10
        minMarginsForBestTaps = cracResultManager.computeMinMarginsForBestTaps(pstRangeAction, 4.148, new ArrayList<>(crac.getFlowCnecs()));
        assertEquals(1, minMarginsForBestTaps.size());
        assertEquals(Double.MAX_VALUE, minMarginsForBestTaps.get(11), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeBestTapPerPstGroup() {
        PstRangeAction pst1 = new PstRangeActionImpl("pst1", new NetworkElement("ne1"));
        PstRangeAction pst2 = new PstRangeActionImpl("pst2", new NetworkElement("ne2"));
        PstRangeAction pst3 = new PstRangeActionImpl("pst3", new NetworkElement("ne3"));
        PstRangeAction pst4 = new PstRangeActionImpl("pst4", new NetworkElement("ne4"));
        PstRangeAction pst5 = new PstRangeActionImpl("pst5", new NetworkElement("ne5"));
        PstRangeAction pst6 = new PstRangeActionImpl("pst6", new NetworkElement("ne6"));
        PstRangeAction pst7 = new PstRangeActionImpl("pst7", new NetworkElement("ne7"));

        ((AbstractRangeAction) pst2).setGroupId("group1");
        ((AbstractRangeAction) pst3).setGroupId("group1");
        ((AbstractRangeAction) pst4).setGroupId("group2");
        ((AbstractRangeAction) pst5).setGroupId("group2");
        ((AbstractRangeAction) pst6).setGroupId("group2");
        ((AbstractRangeAction) pst7).setGroupId("group2");

        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();
        minMarginPerTap.put(pst1, Map.of(3, 100., 4, 500.));

        minMarginPerTap.put(pst2, Map.of(3, 100., 4, 500.));
        minMarginPerTap.put(pst3, Map.of(3, 110., 4, 50.));

        minMarginPerTap.put(pst4, Map.of(-10, -30., -11, -80.));
        minMarginPerTap.put(pst5, Map.of(-10, -40., -11, -20.));
        minMarginPerTap.put(pst6, Map.of(-10, -70., -11, 200.));
        minMarginPerTap.put(pst7, Map.of(-11, Double.MAX_VALUE));

        Map<String, Integer> bestTapPerPstGroup = CracResultManager.computeBestTapPerPstGroup(minMarginPerTap);
        assertEquals(2, bestTapPerPstGroup.size());
        assertEquals(3, bestTapPerPstGroup.get("group1").intValue());
        assertEquals(-10, bestTapPerPstGroup.get("group2").intValue());
    }

    @Test
    public void testComputeBestTaps() {
        setUpPstTesting();
        Crac crac = raoData.getCrac();
        PstRangeAction pstRangeAction = (PstRangeAction) crac.getRangeAction("pst");
        CracResultManager cracResultManager = raoData.getCracResultManager();
        Map<PstRangeAction, Integer> bestTaps;

        MPVariable mockVariable = Mockito.mock(MPVariable.class);
        LinearProblem mockLp = Mockito.mock(LinearProblem.class);
        Mockito.when(mockLp.getRangeActionSetPointVariable(pstRangeAction)).thenReturn(mockVariable);

        Mockito.when(raoData.getSystematicSensitivityResult().getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(3000.);
        Mockito.when(raoData.getSystematicSensitivityResult().getSensitivityOnFlow(pstRangeAction, crac.getBranchCnec("cnec1basecase"))).thenReturn(-250.);
        Mockito.when(mockVariable.solutionValue()).thenReturn(6.);

        bestTaps = cracResultManager.computeBestTaps(mockLp);
        assertEquals(1, bestTaps.size());
        assertEquals(16, bestTaps.get(pstRangeAction).intValue());

        ((AbstractRangeAction) pstRangeAction).setGroupId("group1");
        bestTaps = cracResultManager.computeBestTaps(mockLp);
        assertEquals(1, bestTaps.size());
        assertEquals(16, bestTaps.get(pstRangeAction).intValue());
    }



    /*private void setUpForFillCracResults(boolean curativePst) {
        PstRangeAction rangeAction;
        if (curativePst) {
            rangeAction = new PstRangeActionImpl("idPstRa", new NetworkElement("BBE2AA1  BBE3AA1  1"));
            rangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getState("Contingency FR1 FR3", Instant.CURATIVE)));
        } else {
            rangeAction = new PstRangeActionImpl("idPstRa", new NetworkElement("BBE2AA1  BBE3AA1  1"));
            rangeAction.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        }
        crac = CommonCracCreation.create();
        crac.addRangeAction(rangeAction);
        crac.synchronize(network);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        cracResultManager = raoData.getCracResultManager();
        Mockito.when(linearProblemMock.getRangeActionSetPointVariable(rangeAction)).thenReturn(rangeActionSetPoint);
        Mockito.when(linearProblemMock.getAbsoluteRangeActionVariationVariable(rangeAction)).thenReturn(rangeActionAbsoluteVariation);
        Mockito.when(linearProblemMock.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE)).thenReturn(absoluteRangeActionVariationConstraint);
    }*/

    /*@Test
    public void fillPstResultWithNoActivationAndNeutralRangeAction() {
        setUpForFillCracResults(false);
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.0);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(0.0);

        cracResultManager.fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(0, pstRangeResult.getSetPoint(preventiveState), 0.1);
        assertTrue(pstRangeResult.isActivated(preventiveState));
    }

    @Test
    public void fillPstResultWithNegativeActivation() {
        setUpForFillCracResults(false);
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 - 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        cracResultManager.fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(Integer.valueOf(-12), pstRangeResult.getTap(preventiveState));
        Assert.assertEquals(0.39 - 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithPositiveActivation() {
        setUpForFillCracResults(false);
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 5.0);
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(5.0);

        cracResultManager.fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(Integer.valueOf(14), pstRangeResult.getTap(preventiveState));
        Assert.assertEquals(0.39 + 5, pstRangeResult.getSetPoint(preventiveState), ANGLE_TAP_APPROX_TOLERANCE);
    }

    @Test
    public void fillPstResultWithAngleTooHigh() {
        setUpForFillCracResults(false);
        Mockito.when(absoluteRangeActionVariationConstraint.lb()).thenReturn(0.39);
        Mockito.when(rangeActionSetPoint.solutionValue()).thenReturn(0.39 + 99.0); // value out of PST Range
        Mockito.when(rangeActionAbsoluteVariation.solutionValue()).thenReturn(99.0);

        try {
            cracResultManager.fillRangeActionResultsWithLinearProblem(linearProblemMock);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void fillCurativePstResults() {
        setUpForFillCracResults(true);

        cracResultManager.fillRangeActionResultsWithNetworkValues();
        raoData.getCracVariantManager().setWorkingVariant(raoData.getCracVariantManager().cloneWorkingVariant());
        cracResultManager.fillRangeActionResultsWithLinearProblem(linearProblemMock);

        String preventiveState = raoData.getCrac().getPreventiveState().getId();

        RangeActionResultExtension pstRangeResultMap = raoData.getCrac().getRangeAction("idPstRa").getExtension(RangeActionResultExtension.class);
        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getPreOptimVariantId());
        Assert.assertEquals(0, pstRangeResult.getSetPoint(preventiveState), 0.1);
        Assert.assertEquals(0, pstRangeResult.getTap(preventiveState), 0.1);

        pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
        Assert.assertEquals(0, pstRangeResult.getSetPoint(preventiveState), 0.1);
        Assert.assertEquals(0, pstRangeResult.getTap(preventiveState), 0.1);
    }
}
*/
