/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluatorTest {

    private final static double MNEC_THRESHOLD = 1000.;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private RaoData raoData;
    private Cnec mnec;
    private Unit unit;
    private SystematicSensitivityResult sensiResult;
    private MnecViolationCostEvaluator evaluator1;
    private MnecViolationCostEvaluator evaluator2;
    private static final String TEST_VARIANT = "test-variant";

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
        evaluator1 = new MnecViolationCostEvaluator(unit, 50, 10);
        evaluator2 = new MnecViolationCostEvaluator(unit, 20, 2);

        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getVoltageLevels().forEach(v -> v.setNominalV(400.));

        Crac crac = CommonCracCreation.create();

        mnec = crac.newCnec().setId("MNEC1 - initial-instant - preventive")
                .newNetworkElement().setId("FFR2AA1  FFR3AA1  1").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(MNEC_THRESHOLD).setUnit(unit).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial"))
                .add();
        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        crac.getExtension(ResultVariantManager.class).createVariant(TEST_VARIANT);
        crac.getExtension(ResultVariantManager.class).setInitialVariantId(TEST_VARIANT);

        raoData = RaoData.createOnPreventiveState(network, crac);

        RaoInputHelper.cleanCrac(crac, network, false);
        RaoInputHelper.synchronize(crac, network);

        raoData = RaoData.createOnPreventiveStateBasedOnExistingVariant(network, crac, TEST_VARIANT);
        sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        raoData.setSystematicSensitivityResult(sensiResult);
    }

    private void testCost(double initFlow, double newFlow, MnecViolationCostEvaluator evaluator, double expectedCost) {
        if (unit == Unit.MEGAWATT) {
            mnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setFlowInMW(initFlow);
            Mockito.when(sensiResult.getReferenceFlow(mnec)).thenReturn(newFlow);
        } else {
            mnec.getExtension(CnecResultExtension.class).getVariant(TEST_VARIANT).setFlowInA(initFlow);
            Mockito.when(sensiResult.getReferenceIntensity(mnec)).thenReturn(newFlow);
        }
        assertEquals(expectedCost, evaluator.getCost(raoData), DOUBLE_TOLERANCE);
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
        new MnecViolationCostEvaluator(Unit.KILOVOLT, 0, 0);
    }
}
