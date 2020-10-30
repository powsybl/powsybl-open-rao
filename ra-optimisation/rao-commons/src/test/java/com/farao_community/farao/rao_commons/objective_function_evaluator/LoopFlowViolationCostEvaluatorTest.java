/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.RaoData;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluatorTest {

    private static final double DOUBLE_TOLERANCE = 0.01;
    private RaoData raoData;
    private Network network;
    private Crac crac;
    private State state;
    private Set<State> perimeter;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        state = crac.getPreventiveState();
        perimeter = Collections.singleton(crac.getPreventiveState());
    }

    @Test
    public void testLoopFlowViolationCostEvaluator1() {
        // no loop-flow violation for both cnecs
        addLoopFlowExtensions(crac);
        raoData = new RaoData(network, crac, state, perimeter);

        String var = raoData.getWorkingVariantId();
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(0.);
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(100.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);

        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(15.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(95.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator2() {
        // 90 MW loop-flow violation for cnec1
        // no loop-flow violation for cnec2
        addLoopFlowExtensions(crac);
        raoData = new RaoData(network, crac, state, perimeter);

        String var = raoData.getWorkingVariantId();
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(190.);
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(9.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);

        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 90., new LoopFlowViolationCostEvaluator(15.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 90., new LoopFlowViolationCostEvaluator(95.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator3() {
        // no loop-flow violation for cnec1
        // 10 MW of loop-flow violation for cnec2
        addLoopFlowExtensions(crac);
        raoData = new RaoData(network, crac, state, perimeter);

        String var = raoData.getWorkingVariantId();
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(99.);
        raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowInMW(-110.);
        raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).setLoopflowThresholdInMW(100.);

        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(15. * 10., new LoopFlowViolationCostEvaluator(15.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(95. * 10., new LoopFlowViolationCostEvaluator(95.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testLoopFlowViolationCostEvaluator4() {
        // no cnec with LF extension
        // assertEquals(0., new LoopFlowViolationCostEvaluator(0.).getCost(raoData), DOUBLE_TOLERANCE);
        raoData = new RaoData(network, crac, state, perimeter);
        assertEquals(0., new LoopFlowViolationCostEvaluator(15.).getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(0., new LoopFlowViolationCostEvaluator(95.).getCost(raoData), DOUBLE_TOLERANCE);
    }

    private void addLoopFlowExtensions(Crac crac) {
        CnecLoopFlowExtension cnecLoopFlowExtension1 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);
        CnecLoopFlowExtension cnecLoopFlowExtension2 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);

        crac.getCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension1);
        crac.getCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension2);
    }
}
