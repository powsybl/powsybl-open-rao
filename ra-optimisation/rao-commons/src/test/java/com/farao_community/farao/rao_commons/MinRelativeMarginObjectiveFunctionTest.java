/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinRelativeMarginObjectiveFunctionTest {
    static final double DOUBLE_TOLERANCE = 0.1;
    Crac crac = ExampleGenerator.crac();
    RaoData raoData;
    double commonThreshold = 100.;
    Unit unit;
    SystematicSensitivityResult sensiResult;
    MnecViolationCostEvaluator mnecViolationCostEvaluator;
    MinMarginEvaluator minMarginEvaluator;
    MinMarginEvaluator minRelativeMarginEvaluator;
    MinRelativeMarginObjectiveFunction minRelativeMarginObjectiveFunction;
    private static final String TEST_VARIANT = "test-variant";

    private void setUp(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost) {
        Network network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()));
        this.unit = unit;
        minMarginEvaluator = new MinMarginEvaluator(unit, false);
        minRelativeMarginEvaluator = new MinMarginEvaluator(unit, true);
        mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, mnecAcceptableMarginDiminution, mnecViolationCost);
        minRelativeMarginObjectiveFunction = new MinRelativeMarginObjectiveFunction(unit, mnecAcceptableMarginDiminution, mnecViolationCost);
        crac.newCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FR-BE").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(commonThreshold).setUnit(unit).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial-instant"))
                .add();

        crac.desynchronize();
        RaoInputHelper.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        Map<String, Double> ptdfSums = new HashMap<>();
        Random rand = new Random();
        crac.getCnecs().forEach(cnec -> {
            ptdfSums.put(cnec.getId(), rand.nextDouble());
        });
        raoData.getCrac().getExtension(CracResultExtension.class).getVariant(raoData.getInitialVariantId()).setPtdfSums(ptdfSums);

        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(TEST_VARIANT);

        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
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
        setUp(Unit.MEGAWATT, 20, 5);
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
        setUp(Unit.AMPERE, 37, 15);
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
