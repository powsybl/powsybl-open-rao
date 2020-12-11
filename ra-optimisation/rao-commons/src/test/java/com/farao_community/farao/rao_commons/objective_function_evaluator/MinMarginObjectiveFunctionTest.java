/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Random;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginObjectiveFunctionTest {
    static final double DOUBLE_TOLERANCE = 0.1;
    Crac crac;
    RaoData raoData;
    double commonThreshold = 100.;
    Unit unit;
    SystematicSensitivityResult sensiResult;
    MnecViolationCostEvaluator mnecViolationCostEvaluator;
    MinMarginEvaluator minMarginEvaluator;
    MinMarginEvaluator minRelativeMarginEvaluator;
    MinMarginObjectiveFunction minRelativeMarginObjectiveFunction;
    private static final String TEST_VARIANT = "test-variant";

    private void setUp(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost, RaoParameters.ObjectiveFunction objectiveFunction) {
        double ptdfSumLowerBound = 0.02;
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        this.unit = unit;
        minMarginEvaluator = new MinMarginEvaluator(unit, false);
        minRelativeMarginEvaluator = new MinMarginEvaluator(unit, true, ptdfSumLowerBound);
        mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, mnecAcceptableMarginDiminution, mnecViolationCost);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setMnecAcceptableMarginDiminution(mnecAcceptableMarginDiminution);
        raoParameters.setMnecViolationCost(mnecViolationCost);
        raoParameters.setObjectiveFunction(objectiveFunction);
        raoParameters.setPtdfSumLowerBound(ptdfSumLowerBound);

        minRelativeMarginObjectiveFunction = new MinMarginObjectiveFunction(raoParameters);
        crac.newCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FR-BE").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(commonThreshold).setUnit(unit).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial"))
                .add();

        crac.desynchronize();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setInitialVariantId(TEST_VARIANT);
        Random rand = new Random();
        crac.getCnecs().forEach(cnec ->
                cnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setAbsolutePtdfSum(rand.nextDouble())
        );

        raoData = RaoData.createOnPreventiveStateBasedOnExistingVariant(network, crac, TEST_VARIANT);

        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getStatus()).thenReturn(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS);
        raoData.setSystematicSensitivityResult(sensiResult);
    }

    private void testCost(double initMargin, double newMargin) {
        double initFlow = commonThreshold - initMargin;
        double newFlow = commonThreshold - newMargin;
        if (unit == Unit.MEGAWATT) {
            crac.getCnecs().forEach(cnec ->
                    cnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setFlowInMW(initFlow)
            );
            Mockito.when(sensiResult.getReferenceFlow(Mockito.any())).thenReturn(newFlow);
        } else {
            crac.getCnecs().forEach(cnec ->
                    cnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setFlowInA(initFlow)
            );
            Mockito.when(sensiResult.getReferenceIntensity(Mockito.any())).thenReturn(newFlow);
        }
        double absoluteFunctionalCost = minMarginEvaluator.getCost(raoData);
        double expectedFunctionalCost = (absoluteFunctionalCost > 0) ? absoluteFunctionalCost : minRelativeMarginEvaluator.getCost(raoData);
        double expectedVirtualCost = mnecViolationCostEvaluator.getCost(raoData);
        assertEquals(expectedFunctionalCost, minRelativeMarginObjectiveFunction.getFunctionalCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(expectedVirtualCost, minRelativeMarginObjectiveFunction.getVirtualCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(expectedFunctionalCost + expectedVirtualCost, minRelativeMarginObjectiveFunction.getCost(raoData), DOUBLE_TOLERANCE);
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
