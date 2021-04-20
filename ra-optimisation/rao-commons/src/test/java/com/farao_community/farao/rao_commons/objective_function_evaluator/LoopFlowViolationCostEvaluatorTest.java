/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LoopFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluatorTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    private Crac crac;
    private Set<BranchCnec> loopflowCnecs;
    private Map<BranchCnec, Double> initialLoopFlows;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private LoopFlowParameters parameters;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        crac.addExtension(ResultVariantManager.class, new ResultVariantManager());
        crac.getExtension(ResultVariantManager.class).createVariant("initial");
        crac.getExtension(ResultVariantManager.class).createVariant("current");
        crac.getExtension(ResultVariantManager.class).setInitialVariantId("initial");

        loopflowCnecs = new HashSet<>();
        loopflowCnecs.add(crac.getBranchCnec("cnec1basecase"));
        loopflowCnecs.add(crac.getBranchCnec("cnec2basecase"));

        initialLoopFlows = new HashMap<>();
        sensitivityAndLoopflowResults = Mockito.mock(SensitivityAndLoopflowResults.class);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator1() {
        // no loop-flow violation for both cnecs
        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec1basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec1basecase"))).thenReturn(10.);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec2basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec2basecase"))).thenReturn(100.);

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF, 10, 50, 10);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator2() {
        // 90 MW loop-flow violation for cnec1
        // no loop-flow violation for cnec2
        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec1basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec1basecase"))).thenReturn(190.);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec2basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec2basecase"))).thenReturn(9.);

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 0, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 15, 0);
        assertEquals(15. * 90., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 95, 0);
        assertEquals(95. * 90., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator3() {
        // no loop-flow violation for cnec1
        // 10 MW of loop-flow violation for cnec2

        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec1basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec1basecase"))).thenReturn(99.);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec2basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec2basecase"))).thenReturn(-110.);

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 0, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 15, 0);
        assertEquals(15. * 10., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 95, 0);
        assertEquals(95. * 10., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator4() {
        // 20 MW no loop-flow violation for cnec1, loopflow = initialLoopFlow + acceptableAugmentation (50) + 20
        // 10 MW of loop-flow violation for cnec2, constrained by threshold and not initial loop-flow
        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec1basecase", 200.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec1basecase"))).thenReturn(270.);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(100., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec2basecase", 0.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec2basecase"))).thenReturn(-110.);

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 0, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 15, 0);
        assertEquals(15. * 30., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 95, 0);
        assertEquals(95. * 30., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator5() {
        // 0 MW no loop-flow violation for cnec1, loop flow below initial loopFlow + acceptable augmentation (50)
        // 10 MW of loop-flow violation for cnec2, loopflow = initialLoopFlow + acceptableAugmentation (50) + 10
        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(230., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec1basecase", 200.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec1basecase"))).thenReturn(-245.);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, new CnecLoopFlowExtension(50., Unit.MEGAWATT));
        addLoopFlowInitialResult("cnec2basecase", 100.);
        Mockito.when(sensitivityAndLoopflowResults.getLoopflow(crac.getBranchCnec("cnec2basecase"))).thenReturn(-160.);

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 0, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 15, 0);
        assertEquals(15. * 10., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                50, 95, 0);
        assertEquals(95. * 10., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator6() {
        // no cnec with LF extension
        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);

        loopflowCnecs = new HashSet<>();

        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 15, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        parameters = new LoopFlowParameters(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                0, 95, 0);
        assertEquals(0., new LoopFlowViolationCostEvaluator(loopflowCnecs, initialLoopFlows, parameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    private void addLoopFlowInitialResult(String cnecId, double loopFlowInMW) {
        initialLoopFlows.put(crac.getBranchCnec(cnecId), loopFlowInMW);
    }
}
