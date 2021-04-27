/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_api.parameters.MnecParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void testVirtualCostComputationInMW() {
        setUp(Unit.MEGAWATT);
        testCost(-100, 0, 0, 0);
        testCost(-100, -50, 0, 0);
        testCost(-100, -150, 0, 60);
        testCost(-100, -200, 500, 160);
        testCost(-100, -250, 1000, 260);
        testCost(30, 0, 0, 0);
        testCost(30, -20, 0, 40);
        testCost(30, -50, 300, 100);
        testCost(200, 200, 0, 0);
        testCost(200, 100, 0, 0);
        testCost(200, 0, 0, 0);
        testCost(200, -10, 100, 20);
    }

    @Test
    public void testVirtualCostComputationInA() {
        setUp(Unit.AMPERE);
        testCost(-100, 0, 0, 0);
        testCost(-100, -50, 0, 0);
        testCost(-100, -150, 0, 42.3);
        testCost(-100, -200, 278.3, 142.3);
        testCost(-100, -250, 778.3, 242.3);
        testCost(30, 0, 0, 0);
        testCost(30, -20, 0, 40);
        testCost(30, -50, 78.3, 100);
        testCost(200, 200, 0, 0);
        testCost(200, 100, 0, 0);
        testCost(200, 0, 0, 0);
        testCost(200, -10, 100, 20);
    }

    private void setUp(Unit unit) {
        this.unit = unit;

        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getVoltageLevels().forEach(v -> v.setNominalV(400.));

        Crac crac = CommonCracCreation.create();

        mnec = crac.newFlowCnec()
            .withId("MNEC1 - initial-instant - preventive")
            .withNetworkElement("FFR2AA1  FFR3AA1  1")
            .newThreshold().withMin(-MNEC_THRESHOLD).withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(MNEC_THRESHOLD).withUnit(unit).add()
            .withOptimized(true)
            .withMonitored(true)
            .withInstant(Instant.PREVENTIVE)
            .withNominalVoltage(400.)
            .withIMax(5000.)
            .add();
        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setInitialVariantId(TEST_VARIANT);

        cnecs = crac.getBranchCnecs();
        initialFlows = new HashMap<>();
        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(sensiResult);

        evaluator1 = new MnecViolationCostEvaluator(cnecs, initialFlows, unit, new MnecParameters(
                50, 10, 1));
        evaluator2 = new MnecViolationCostEvaluator(cnecs, initialFlows, unit, new MnecParameters(
                20, 2, 1));
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

    private void testCost(double initMargin, double newMargin, double expectedCostWithEval1, double expectedCostWithEval2) {
        double initFlow = MNEC_THRESHOLD - initMargin;
        double newFlow = MNEC_THRESHOLD - newMargin;

        testCost(initFlow, newFlow, evaluator1, expectedCostWithEval1);
        testCost(-initFlow, -newFlow, evaluator1, expectedCostWithEval1);

        testCost(initFlow, newFlow, evaluator2, expectedCostWithEval2);
        testCost(-initFlow, -newFlow, evaluator2, expectedCostWithEval2);
    }

    @Test(expected = NotImplementedException.class)
    public void testCrash() {
        new MnecViolationCostEvaluator(cnecs, initialFlows, Unit.KILOVOLT, new MnecParameters(
                50, 10, 0));
    }
}
