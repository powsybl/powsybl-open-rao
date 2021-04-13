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
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginObjectiveFunctionTest {
    static final double DOUBLE_TOLERANCE = 0.1;
    Crac crac;
    double commonThreshold = 100.;
    Unit unit;
    SystematicSensitivityResult sensiResult;
    MnecViolationCostEvaluator mnecViolationCostEvaluator;
    MinMarginEvaluator minMarginEvaluator;
    MinMarginEvaluator minRelativeMarginEvaluator;
    MinMarginObjectiveFunction minRelativeMarginObjectiveFunction;
    Set<BranchCnec> cnecs;
    Map<BranchCnec, Double> prePerimeterMargins;
    Map<BranchCnec, Double> initialPtdfSums;
    SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    private void setUp(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost, RaoParameters.ObjectiveFunction objectiveFunction) {
        double ptdfSumLowerBound = 0.02;
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(sensiResult);

        cnecs = crac.getBranchCnecs();
        prePerimeterMargins = new HashMap<>();
        initialPtdfSums = new HashMap<>();

        this.unit = unit;
        minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, unit, null, false);
        minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, unit, null, true, ptdfSumLowerBound);
        mnecViolationCostEvaluator = new MnecViolationCostEvaluator(cnecs, new HashMap<>(), unit, mnecAcceptableMarginDiminution, mnecViolationCost);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setMnecAcceptableMarginDiminution(mnecAcceptableMarginDiminution);
        raoParameters.setMnecViolationCost(mnecViolationCost);
        raoParameters.setObjectiveFunction(objectiveFunction);
        raoParameters.setPtdfSumLowerBound(ptdfSumLowerBound);

        minRelativeMarginObjectiveFunction = new MinMarginObjectiveFunction(cnecs, new HashSet<>(), prePerimeterMargins, initialPtdfSums, new HashMap<>(), new HashMap<>(), raoParameters, null);
        crac.newBranchCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FR-BE").add()
                .newThreshold().setMin(-commonThreshold).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(commonThreshold).setUnit(unit).add()
                .optimized().monitored()
                .setInstant(Instant.PREVENTIVE)
                .add();

        crac.desynchronize();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        Random rand = new Random();
        crac.getBranchCnecs().forEach(cnec -> initialPtdfSums.put(cnec, rand.nextDouble()));

    }

    private void testCost(double initMargin, double newMargin) {
        crac.getBranchCnecs().forEach(cnec ->
                prePerimeterMargins.put(cnec, initMargin)
        );

        double newFlow = commonThreshold - newMargin;
        if (unit == Unit.MEGAWATT) {
            Mockito.when(sensiResult.getReferenceFlow(Mockito.any())).thenReturn(newFlow);
        } else {
            Mockito.when(sensiResult.getReferenceIntensity(Mockito.any())).thenReturn(newFlow);
        }
        double absoluteFunctionalCost = minMarginEvaluator.computeCost(sensitivityAndLoopflowResults);
        double expectedFunctionalCost = (absoluteFunctionalCost > 0) ? absoluteFunctionalCost : minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults);
        double expectedVirtualCost = mnecViolationCostEvaluator.computeCost(sensitivityAndLoopflowResults);
        assertEquals(expectedFunctionalCost, minRelativeMarginObjectiveFunction.computeFunctionalCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        assertEquals(expectedVirtualCost, minRelativeMarginObjectiveFunction.computeVirtualCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        assertEquals(expectedFunctionalCost + expectedVirtualCost, minRelativeMarginObjectiveFunction.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMW() {
        setUp(Unit.MEGAWATT, 20, 5, MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        testCost(-100, 0);
        testCost(-100, -50);
        testCost(-100, -150);
        testCost(-100, -200);
        testCost(-100, -250);
        testCost(30, 0);
        testCost(30, -20);
        testCost(30, -50);
        testCost(200, 200);
        testCost(200, 100);
        testCost(200, 0);
        testCost(200, -10);
    }

    @Test
    public void testA() {
        setUp(Unit.AMPERE, 37, 15, MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        testCost(-100, 0);
        testCost(-100, -50);
        testCost(-100, -150);
        testCost(-100, -200);
        testCost(-100, -250);
        testCost(30, 0);
        testCost(30, -20);
        testCost(30, -50);
        testCost(200, 200);
        testCost(200, 100);
        testCost(200, 0);
        testCost(200, -10);
        testCost(0, 1000);
        testCost(0, 1500);
        testCost(0, 2000);
    }
}
