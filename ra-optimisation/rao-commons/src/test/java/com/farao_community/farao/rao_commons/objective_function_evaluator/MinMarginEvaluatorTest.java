/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_api.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.rao_api.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.compress.utils.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 1;

    private Crac crac;
    private SystematicSensitivityResult systematicSensitivityResult;
    private Network network;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private Set<BranchCnec> cnecs;
    private Map<BranchCnec, Double> prePerimeterMargins;
    Map<BranchCnec, Double> initialPtdfSums;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        network = NetworkImportsUtil.import12NodesNetwork();

        initialPtdfSums = new HashMap<>();
        setPtdfSum("cnec1basecase", 0.5);
        setPtdfSum("cnec1stateCurativeContingency1", 0.95);
        setPtdfSum("cnec1stateCurativeContingency2", 0.95);
        setPtdfSum("cnec2basecase", 0.4);
        setPtdfSum("cnec2stateCurativeContingency1", 0.6);
        setPtdfSum("cnec2stateCurativeContingency2", 0.6);

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);

        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase")))
                .thenReturn(100.);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec2basecase")))
                .thenReturn(200.);

        Mockito.when(systematicSensitivityResult.getReferenceIntensity(any())).thenReturn(Double.NaN);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1basecase")))
                .thenReturn(30.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2basecase")))
                .thenReturn(60.);

        cnecs = crac.getBranchCnecs(crac.getPreventiveState());
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult);
        prePerimeterMargins = new HashMap<>();
    }

    private void setPtdfSum(String cnecId, double ptdfSum) {
        initialPtdfSums.put(crac.getBranchCnec(cnecId), ptdfSum);
    }

    @Test
    public void getCostInMegawatt() {
        LinearOptimizerParameters linearOptimizerParameters1 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters1);
        assertEquals(-787, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        LinearOptimizerParameters linearOptimizerParameters2 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters2);
        assertEquals(-787 / 0.4, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInMegawattSkipOperatorsNotToOptimize() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();

        // cnec1 has a margin of 1400 MW "after optim"
        // cnec2 has a margin of 787 MW "after optim"
        BranchCnec cnec1 = crac.getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = crac.getBranchCnec("cnec2basecase");

        // If operator 2 doesn't share RA
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
        //cnec1.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(100.0);
        prePerimeterMargins.put(cnec1, 1400.0);

        // case 0 : margin on cnec2 is the same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        prePerimeterMargins.put(cnec2, 787.0);
        assertEquals(-1400, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 687.0);
        assertEquals(-1400, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is the slightly improved => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 786.0);
        assertEquals(-1400, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 887.0);
        assertEquals(-787, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 887.0);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(1000.);
        assertEquals(-500, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInMegawattSkipOperatorsNotToOptimizeInRelative() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();

        // cnec1 has a margin of 1400 MW "after optim"
        // cnec2 has a margin of 787 MW "after optim"
        BranchCnec cnec1 = crac.getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = crac.getBranchCnec("cnec2basecase");

        // If operator 2 doesn't share RA
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
        //cnec1.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(100.0);
        prePerimeterMargins.put(cnec1, 1400.0);

        // case 0 : margin on cnec2 is the same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        prePerimeterMargins.put(cnec2, 787.0);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 687.0);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is the slightly improved => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 786.0);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 887.0);
        assertEquals(-787 / 0.4, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 887.0);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(1000.);
        assertEquals(-500 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithMissingValues() {
        LinearOptimizerParameters linearOptimizerParameters1 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters1);
        assertEquals(-1440, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        LinearOptimizerParameters linearOptimizerParameters2 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters2);
        assertEquals(-3600, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithNoMissingValues() {
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1stateCurativeContingency1")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1stateCurativeContingency2")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2stateCurativeContingency1")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2stateCurativeContingency2")))
                .thenReturn(10.);

        LinearOptimizerParameters linearOptimizerParameters1 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters1);
        assertEquals(-1440, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        LinearOptimizerParameters linearOptimizerParameters2 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters2);
        assertEquals(-3600, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereSkipOperatorsNotToOptimize() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();

        // cnec1 has a margin of 2249 A "after optim"
        // cnec2 has a margin of 1440 A "after optim"
        BranchCnec cnec1 = crac.getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = crac.getBranchCnec("cnec2basecase");

        // If operator 2 doesn't share RA
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
        prePerimeterMargins.put(cnec1, 1400.0);

        // case 0 : margin on cnec2 is same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        prePerimeterMargins.put(cnec2, 787.0);
        assertEquals(-2249, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 687.0);
        assertEquals(-2249, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is slightly improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 786.0);
        assertEquals(-2249, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 887.0);
        assertEquals(-1440, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 887.0);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1basecase"))).thenReturn(1300.);
        assertEquals(-979, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereSkipOperatorsNotToOptimizeInRelative() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();

        // cnec1 has a margin of 2249 A "after optim"
        // cnec2 has a margin of 1440 A "after optim"
        BranchCnec cnec1 = crac.getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = crac.getBranchCnec("cnec2basecase");

        // If operator 2 doesn't share RA
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
        prePerimeterMargins.put(cnec1, 1400.0);

        // case 0 : margin on cnec2 is same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        prePerimeterMargins.put(cnec2, 787.0);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 687.0);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is slightly improved => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 786.0);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        prePerimeterMargins.put(cnec2, 887.0);
        assertEquals(-1440 / 0.4, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        prePerimeterMargins.put(cnec2, 887.0);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1basecase"))).thenReturn(1300.);
        assertEquals(-979 / 0.5, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testIgnoreMnecs() {

        crac.newFlowCnec()
            .withId("mnec1basecase")
            .withNetworkElement("DDE2AA1  NNL3AA1  1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(300.).withMin(-300.).withUnit(Unit.MEGAWATT).add()
            .withOptimized().withMonitored()
            .withInstant(Instant.PREVENTIVE)
            .add();

        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("mnec1basecase")))
                .thenReturn(200.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("mnec1basecase")))
                .thenReturn(60.);

        LinearOptimizerParameters linearOptimizerParameters1 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters1);
        assertEquals(-787, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        LinearOptimizerParameters linearOptimizerParameters2 = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        minMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters2);
        assertEquals(-1440, minMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMinimumPtdfSum() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.02))
                .withPstSensitivityThreshold(0.01)
                .build();

        setPtdfSum("cnec1basecase", 0.005);
        setPtdfSum("cnec1stateCurativeContingency1", 0.0095);
        setPtdfSum("cnec1stateCurativeContingency2", 0.0095);
        setPtdfSum("cnec2basecase", 0.004);
        setPtdfSum("cnec2stateCurativeContingency1", 0.006);
        setPtdfSum("cnec2stateCurativeContingency2", 0.006);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
        assertEquals(-39363, minRelativeMarginEvaluator.computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testRequirePtdfSumLb() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();
        new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters);
    }

    @Test
    public void testMarginsInAmpereFromMegawattConversion() {
        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.001))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();
        Map<BranchCnec, Double> margins = new MinMarginEvaluator(cnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters).getMarginsInAmpereFromMegawattConversion(systematicSensitivityResult);
        assertEquals(2, margins.keySet().size());
        assertEquals(2990, margins.get(crac.getBranchCnec("cnec2basecase")), DOUBLE_TOLERANCE);
        assertEquals(4254, margins.get(crac.getBranchCnec("cnec1basecase")), DOUBLE_TOLERANCE);
    }

    private Set<BranchCnec> setUpMockCnecs(boolean optimized, boolean monitored) {
        // CNEC 1 : margin of 1000 MW / 100 A, sum of PTDFs = 1
        CnecResult result1 = Mockito.mock(CnecResult.class);
        Mockito.when(result1.getAbsolutePtdfSum()).thenReturn(1.0);
        CnecResultExtension resultExtension1 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension1.getVariant(Mockito.anyString())).thenReturn(result1);

        BranchCnec cnec1 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec1.getId()).thenReturn("cnec1");
        Mockito.when(cnec1.isOptimized()).thenReturn(optimized);
        Mockito.when(cnec1.isMonitored()).thenReturn(monitored);
        Mockito.when(cnec1.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension1);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(1000.);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(100.);

        // CNEC 2 : margin of 600 MW / 60 A, sum of PTDFs = 0.5
        CnecResult result2 = Mockito.mock(CnecResult.class);
        Mockito.when(result2.getAbsolutePtdfSum()).thenReturn(0.5);
        CnecResultExtension resultExtension2 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension2.getVariant(Mockito.anyString())).thenReturn(result2);

        BranchCnec cnec2 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec2.getId()).thenReturn("cnec2");
        Mockito.when(cnec2.isOptimized()).thenReturn(optimized);
        Mockito.when(cnec2.isMonitored()).thenReturn(monitored);
        Mockito.when(cnec2.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension2);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(600.);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(60.);

        return Sets.newHashSet(cnec1, cnec2);
    }

    @Test
    public void testPureMnecs() {
        Set<BranchCnec> mnecs = setUpMockCnecs(false, true);

        LinearOptimizerParameters linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        assertEquals(0, new MinMarginEvaluator(mnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();
        assertEquals(0, new MinMarginEvaluator(mnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.02))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();
        assertEquals(0, new MinMarginEvaluator(mnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);

        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.02))
                .withPstSensitivityThreshold(0.01)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("operator2"), 2000))
                .build();
        assertEquals(0, new MinMarginEvaluator(mnecs, prePerimeterMargins, initialPtdfSums, linearOptimizerParameters).computeCost(sensitivityAndLoopflowResults), DOUBLE_TOLERANCE);
    }

}
