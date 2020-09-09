/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginObjectiveFunctionTest {
    static final double DOUBLE_TOLERANCE = 0.1;
    Crac crac = ExampleGenerator.crac();
    RaoData raoData;
    Cnec mnec;
    double commonThreshold = 1000.;
    Unit unit;
    SystematicSensitivityAnalysisResult sensiResult;
    MnecViolationCostEvaluator mnecViolationCostEvaluator;
    MinMarginEvaluator minMarginEvaluator;
    MinMarginObjectiveFunction minMarginObjectiveFunction;
    private static final String TEST_VARIANT = "test-variant";

    private void setUp(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost) {
        Network network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        raoData = new RaoData(network, crac, crac.getPreventiveState());
        this.unit = unit;
        minMarginEvaluator = new MinMarginEvaluator(unit);
        mnecViolationCostEvaluator = new MnecViolationCostEvaluator(unit, mnecAcceptableMarginDiminution, mnecViolationCost);
        minMarginObjectiveFunction = new MinMarginObjectiveFunction(unit, mnecAcceptableMarginDiminution, mnecViolationCost);
        crac.newCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FR-BE").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(commonThreshold).setUnit(unit).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial-instant"))
                .add();
        Cnec mnec = crac.getCnec("MNEC1 - initial-instant - preventive");

        crac.desynchronize();
        RaoInputHelper.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(TEST_VARIANT);

        sensiResult = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        raoData.setSystematicSensitivityAnalysisResult(sensiResult);
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
        double expectedFunctionalCost = minMarginEvaluator.getCost(raoData);
        double expectedVirtualCost = mnecViolationCostEvaluator.getCost(raoData);
        assertEquals(expectedFunctionalCost, minMarginObjectiveFunction.getFunctionalCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(expectedVirtualCost, minMarginObjectiveFunction.getVirtualCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(expectedFunctionalCost + expectedVirtualCost, minMarginObjectiveFunction.getCost(raoData), DOUBLE_TOLERANCE);
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
    }

}
