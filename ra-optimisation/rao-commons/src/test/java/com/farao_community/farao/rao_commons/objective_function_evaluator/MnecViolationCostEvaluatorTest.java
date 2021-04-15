/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.ParametersProvider;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluatorTest {
    private final static double MNEC_THRESHOLD = 1000.;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private BranchCnec mnec;
    private Unit unit;
    private SystematicSensitivityResult sensiResult;
    private MnecViolationCostEvaluator evaluator1;
    private MnecViolationCostEvaluator evaluator2;
    private static final String TEST_VARIANT = "test-variant";
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private Map<BranchCnec, Double> initialFlows;
    private Set<BranchCnec> cnecs;

    @Test
    public void testVirtualCostComputationInMWForEvaluator1() {
        ParametersProvider.getCoreParameters().setOperatorsNotToOptimize(Collections.emptySet());
        ParametersProvider.getCoreParameters().setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        ParametersProvider.getMnecParameters().setMnecAcceptableMarginDiminution(50);
        ParametersProvider.getMnecParameters().setMnecViolationCost(10);
        setUp(Unit.MEGAWATT);
        evaluator1 = new MnecViolationCostEvaluator(cnecs, initialFlows);
        testCost(-100, 0, 0, evaluator1);
        testCost(-100, -50, 0, evaluator1);
        testCost(-100, -150, 0, evaluator1);
        testCost(-100, -200, 500, evaluator1);
        testCost(-100, -250, 1000, evaluator1);
        testCost(30, 0, 0, evaluator1);
        testCost(30, -20, 0, evaluator1);
        testCost(30, -50, 300, evaluator1);
        testCost(200, 200, 0, evaluator1);
        testCost(200, 100, 0, evaluator1);
        testCost(200, 0, 0, evaluator1);
        testCost(200, -10, 100, evaluator1);
    }

    @Test
    public void testVirtualCostComputationInAForEvaluator1() {
        ParametersProvider.getCoreParameters().setOperatorsNotToOptimize(Collections.emptySet());
        ParametersProvider.getCoreParameters().setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        ParametersProvider.getMnecParameters().setMnecAcceptableMarginDiminution(50);
        ParametersProvider.getMnecParameters().setMnecViolationCost(10);
        setUp(Unit.AMPERE);
        evaluator1 = new MnecViolationCostEvaluator(cnecs, initialFlows);
        testCost(-100, 0, 0, evaluator1);
        testCost(-100, -50, 0, evaluator1);
        testCost(-100, -150, 0, evaluator1);
        testCost(-100, -200, 278.3, evaluator1);
        testCost(-100, -250, 778.3, evaluator1);
        testCost(30, 0, 0, evaluator1);
        testCost(30, -20, 0, evaluator1);
        testCost(30, -50, 78.3, evaluator1);
        testCost(200, 200, 0, evaluator1);
        testCost(200, 100, 0, evaluator1);
        testCost(200, 0, 0, evaluator1);
        testCost(200, -10, 100, evaluator1);
    }

    @Test
    public void testVirtualCostComputationInMWForEvaluator2() {
        ParametersProvider.getCoreParameters().setOperatorsNotToOptimize(Collections.emptySet());
        ParametersProvider.getCoreParameters().setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        ParametersProvider.getMnecParameters().setMnecAcceptableMarginDiminution(20);
        ParametersProvider.getMnecParameters().setMnecViolationCost(2);
        setUp(Unit.MEGAWATT);
        evaluator2 = new MnecViolationCostEvaluator(cnecs, initialFlows);
        testCost(-100, 0, 0, evaluator2);
        testCost(-100, -50, 0, evaluator2);
        testCost(-100, -150, 60, evaluator2);
        testCost(-100, -200, 160, evaluator2);
        testCost(-100, -250, 260, evaluator2);
        testCost(30, 0, 0, evaluator2);
        testCost(30, -20, 40, evaluator2);
        testCost(30, -50, 100, evaluator2);
        testCost(200, 200, 0, evaluator2);
        testCost(200, 100, 0, evaluator2);
        testCost(200, 0, 0, evaluator2);
        testCost(200, -10, 20, evaluator2);
    }

    @Test
    public void testVirtualCostComputationInAForEvaluator2() {
        ParametersProvider.getCoreParameters().setOperatorsNotToOptimize(Collections.emptySet());
        ParametersProvider.getCoreParameters().setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        ParametersProvider.getMnecParameters().setMnecAcceptableMarginDiminution(20);
        ParametersProvider.getMnecParameters().setMnecViolationCost(2);
        setUp(Unit.AMPERE);
        evaluator2 = new MnecViolationCostEvaluator(cnecs, initialFlows);
        testCost(-100, 0, 0, evaluator2);
        testCost(-100, -50, 0, evaluator2);
        testCost(-100, -150, 42.3, evaluator2);
        testCost(-100, -200, 142.3, evaluator2);
        testCost(-100, -250, 242.3, evaluator2);
        testCost(30, 0, 0, evaluator2);
        testCost(30, -20, 40, evaluator2);
        testCost(30, -50, 100, evaluator2);
        testCost(200, 200, 0, evaluator2);
        testCost(200, 100, 0, evaluator2);
        testCost(200, 0, 0, evaluator2);
        testCost(200, -10, 20, evaluator2);
    }

    private void setUp(Unit unit) {
        this.unit = unit;

        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getVoltageLevels().forEach(v -> v.setNominalV(400.));

        Crac crac = CommonCracCreation.create();

        mnec = crac.newBranchCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FFR2AA1  FFR3AA1  1").add()
                .newThreshold().setMin(-MNEC_THRESHOLD).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(MNEC_THRESHOLD).setUnit(unit).add()
                .optimized().monitored()
                .setInstant(Instant.PREVENTIVE)
                .add();
        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setInitialVariantId(TEST_VARIANT);

        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        cnecs = crac.getBranchCnecs();
        initialFlows = new HashMap<>();
        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(sensiResult);

        evaluator2 = new MnecViolationCostEvaluator(cnecs, initialFlows);
    }

    private void testCost(double initFlow, double newFlow, MnecViolationCostEvaluator evaluator, double expectedCost) {
        initialFlows.put(mnec, initFlow);
        if (unit == Unit.MEGAWATT) {
            Mockito.when(sensiResult.getReferenceFlow(mnec)).thenReturn(newFlow);
        } else {
            Mockito.when(sensiResult.getReferenceIntensity(mnec)).thenReturn(newFlow);
        }
        assertEquals(expectedCost, evaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    private void testCost(double initMargin, double newMargin, double expectedCostWithEval, MnecViolationCostEvaluator evaluator) {
        double initFlow = MNEC_THRESHOLD - initMargin;
        double newFlow = MNEC_THRESHOLD - newMargin;

        testCost(initFlow, newFlow, evaluator, expectedCostWithEval);
        testCost(-initFlow, -newFlow, evaluator, expectedCostWithEval);
    }
}
