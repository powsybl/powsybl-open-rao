/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdImpl;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluatorTest {

    private static final double DOUBLE_TOLERANCE = 0.01;
    private static final String INITIAL = "initial";
    private static final String CURRENT = "current";

    private Network network;
    private Crac crac;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.addExtension(ResultVariantManager.class, new ResultVariantManager());
        crac.getExtension(ResultVariantManager.class).createVariant("initial");
        crac.getExtension(ResultVariantManager.class).createVariant("current");
        crac.getExtension(ResultVariantManager.class).setInitialVariantId("initial");
    }

    private RaoData createRaoDataOnPreventiveStateBasedOnExistingVariant(String variantId) {
        return new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, variantId, new RaoParameters());
    }

    @Test
    public void testLoopFlowViolationCostEvaluator1() {
        // no loop-flow violation for both cnecs
        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(10.);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(100.);

        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");

        assertEquals(0., new LoopFlowViolationCostEvaluator(0., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(15., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(95., 0.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator2() {
        // 90 MW loop-flow violation for cnec1
        // no loop-flow violation for cnec2
        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(190.);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(9);

        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");

        assertEquals(0., new LoopFlowViolationCostEvaluator(0., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 90., new LoopFlowViolationCostEvaluator(15., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 90., new LoopFlowViolationCostEvaluator(95., 0.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator3() {
        // no loop-flow violation for cnec1
        // 10 MW of loop-flow violation for cnec2

        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(99.);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(-110.);

        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");

        assertEquals(0., new LoopFlowViolationCostEvaluator(0., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 10., new LoopFlowViolationCostEvaluator(15., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 10., new LoopFlowViolationCostEvaluator(95., 0.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator4() {
        // 20 MW no loop-flow violation for cnec1, loopflow = initialLoopFlow + acceptableAugmentation (50) + 20
        // 10 MW of loop-flow violation for cnec2, constrained by threshold and not initial loop-flow
        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(200.);
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(270.);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(100., Unit.MEGAWATT));
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(0.);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(-110.);

        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");

        assertEquals(0., new LoopFlowViolationCostEvaluator(0., 50.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 30., new LoopFlowViolationCostEvaluator(15., 50.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 30., new LoopFlowViolationCostEvaluator(95., 50.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator5() {
        // 0 MW no loop-flow violation for cnec1, loop flow below initial loopFlow + acceptable augmentation (50)
        // 10 MW of loop-flow violation for cnec2, loopflow = initialLoopFlow + acceptableAugmentation (50) + 10
        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(230., Unit.MEGAWATT));
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(200.);
        crac.getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(-245.);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, new LoopFlowThresholdImpl(50., Unit.MEGAWATT));
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(INITIAL).setLoopflowInMW(100.);
        crac.getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(CURRENT).setLoopflowInMW(-160.);

        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");

        assertEquals(0., new LoopFlowViolationCostEvaluator(0., 50.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 10., new LoopFlowViolationCostEvaluator(15., 50.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 10., new LoopFlowViolationCostEvaluator(95., 50.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator6() {
        // no cnec with LF extension
        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        RaoData raoData = createRaoDataOnPreventiveStateBasedOnExistingVariant("current");
        assertEquals(0., new LoopFlowViolationCostEvaluator(15., 0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(95., 0.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    private void addLoopFlowExtensions(Crac crac) {
        LoopFlowThresholdImpl cnecLoopFlowExtension1 = new LoopFlowThresholdImpl(100., Unit.MEGAWATT);
        LoopFlowThresholdImpl cnecLoopFlowExtension2 = new LoopFlowThresholdImpl(100., Unit.MEGAWATT);

        crac.getBranchCnec("cnec1basecase").addExtension(LoopFlowThresholdImpl.class, cnecLoopFlowExtension1);
        crac.getBranchCnec("cnec2basecase").addExtension(LoopFlowThresholdImpl.class, cnecLoopFlowExtension2);
    }
}
