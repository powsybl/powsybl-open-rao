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
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluatorTest {
    static final double DOUBLE_TOLERANCE = 0.1;

    Network network = ExampleGenerator.network();
    Crac crac = ExampleGenerator.crac();
    RaoData raoData = new RaoData(network, crac);
    Cnec mnec;
    double mnecThreshold = 1000.;
    SystematicSensitivityAnalysisResult sensiResult;
    MnecViolationCostEvaluator evaluator1;
    MnecViolationCostEvaluator evaluator2;
    private static final String TEST_VARIANT = "test-variant";

    @Before
    public void setUp() {
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        raoData = new RaoData(network, crac);

        evaluator1 = new MnecViolationCostEvaluator(Unit.MEGAWATT, 50, 10);
        evaluator2 = new MnecViolationCostEvaluator(Unit.MEGAWATT, 20, 2);

        crac.newCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FR-BE").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(mnecThreshold).setUnit(Unit.MEGAWATT).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial-instant"))
                .add();
        mnec = crac.getCnec("MNEC1 - initial-instant - preventive");

        crac.desynchronize();
        RaoInput.cleanCrac(crac, network);
        RaoInput.synchronize(crac, network);

        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(TEST_VARIANT);

        sensiResult = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        raoData.setSystematicSensitivityAnalysisResult(sensiResult);
    }

    private void testCost(double initFlow, double newFlow, MnecViolationCostEvaluator evaluator, double expectedCost) {
        mnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setFlowInMW(initFlow);
        Mockito.when(sensiResult.getReferenceFlow(mnec)).thenReturn(newFlow);
        assertEquals(expectedCost, evaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    private void testCost(double initMargin, double newMargin, double expectedCostWithEval1, double expectedCostWithEval2) {
        double initFlow = mnecThreshold - initMargin;
        double newFlow = mnecThreshold - newMargin;

        testCost(initFlow, newFlow, evaluator1, expectedCostWithEval1);
        testCost(-initFlow, -newFlow, evaluator1, expectedCostWithEval1);

        testCost(initFlow, newFlow, evaluator2, expectedCostWithEval2);
        testCost(-initFlow, -newFlow, evaluator2, expectedCostWithEval2);
    }

    @Test
    public void testVirtualCostComputation() {
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

    @Test(expected = NotImplementedException.class)
    public void testCrash() {
        MnecViolationCostEvaluator evaluator = new MnecViolationCostEvaluator(Unit.AMPERE, 0, 0);
    }

}
