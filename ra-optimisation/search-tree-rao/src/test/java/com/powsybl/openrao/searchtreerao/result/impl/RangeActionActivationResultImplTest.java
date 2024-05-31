/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class RangeActionActivationResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private PstRangeAction pstRangeAction1;
    private PstRangeAction pstRangeAction2; // 1 on 2 are on the same PST
    private PstRangeAction pstRangeAction3;
    private State pState;
    private State oState1;
    private State cState1;
    private State cState2;
    private RangeActionSetpointResult rangeActionSetpointResult;

    @BeforeEach
    public void setUp() {

        Crac crac = CommonCracCreation.create();
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newFlowCnec()
            .withId("cnecOnOutageState1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(-1500.).withMax(1500.).add()
            .add();

        pState = crac.getPreventiveState();
        oState1 = crac.getState("Contingency FR1 FR3", outageInstant);
        cState1 = crac.getState("Contingency FR1 FR3", curativeInstant);
        cState2 = crac.getState("Contingency FR1 FR2", curativeInstant);

        pstRangeAction1 = crac.newPstRangeAction()
            .withId("pst1")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-3, -3.1, -2, -2.1, -1, -1.1, 0, 0., 1, 1.1, 2, 2.1, 3, 3.1))
            .add();

        pstRangeAction2 = crac.newPstRangeAction()
            .withId("pst2")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-3, -3.1, -2, -2.1, -1, -1.1, 0, 0., 1, 1.1, 2, 2.1, 3, 3.1))
            .add();

        pstRangeAction3 = crac.newPstRangeAction()
            .withId("pst3")
            .withNetworkElement("anotherPst")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-3, -3.1, -2, -2.1, -1, -1.1, 0, 0., 1, 1.1, 2, 2.1, 3, 3.1))
            .add();

        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(
            pstRangeAction1, 0.,
            pstRangeAction2, 0.,
            pstRangeAction3, -2.1));
    }

    @Test
    void test1() {

        // pstRangeAction1 is activated in preventive, pstRangeAction3 is activated in curative

        RangeActionActivationResultImpl raar = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        raar.activate(pstRangeAction1, pState, 1.1);

        raar.activate(pstRangeAction3, pState, -2.1); // should not be taken into account as activation with same setpoint as reference
        raar.activate(pstRangeAction3, cState2, -1.1);

        // pstRangeAction1
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction1, pState), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction1, oState1), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction1, cState1), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction1, cState2), DOUBLE_TOLERANCE);

        assertEquals(1, raar.getOptimizedTap(pstRangeAction1, pState), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction1, oState1), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction1, cState1), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction1, cState2), DOUBLE_TOLERANCE);

        // pstRangeAction2 (same as RA 1, as same PST)
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction2, pState), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction2, oState1), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction2, cState1), DOUBLE_TOLERANCE);
        assertEquals(1.1, raar.getOptimizedSetpoint(pstRangeAction2, cState2), DOUBLE_TOLERANCE);

        assertEquals(1, raar.getOptimizedTap(pstRangeAction2, pState), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction2, oState1), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction2, cState1), DOUBLE_TOLERANCE);
        assertEquals(1, raar.getOptimizedTap(pstRangeAction2, cState2), DOUBLE_TOLERANCE);

        // pstRangeAction3, activated in cState2
        assertEquals(-2.1, raar.getOptimizedSetpoint(pstRangeAction3, pState), DOUBLE_TOLERANCE);
        assertEquals(-2.1, raar.getOptimizedSetpoint(pstRangeAction3, oState1), DOUBLE_TOLERANCE);
        assertEquals(-2.1, raar.getOptimizedSetpoint(pstRangeAction3, cState1), DOUBLE_TOLERANCE);
        assertEquals(-1.1, raar.getOptimizedSetpoint(pstRangeAction3, cState2), DOUBLE_TOLERANCE);

        assertEquals(-2, raar.getOptimizedTap(pstRangeAction3, pState), DOUBLE_TOLERANCE);
        assertEquals(-2, raar.getOptimizedTap(pstRangeAction3, oState1), DOUBLE_TOLERANCE);
        assertEquals(-2, raar.getOptimizedTap(pstRangeAction3, cState1), DOUBLE_TOLERANCE);
        assertEquals(-1, raar.getOptimizedTap(pstRangeAction3, cState2), DOUBLE_TOLERANCE);

        // activations
        // both pstRangeAction1 and pstRangeAction2 have tap which change in the preventive state
        // but only the pstRangeAction1 is explicitly activated
        // it is a classic example of a PRA and a CRA on a same PST

        assertEquals(Set.of(pstRangeAction1), raar.getActivatedRangeActions(pState));
        assertEquals(Set.of(), raar.getActivatedRangeActions(oState1));
        assertEquals(Set.of(), raar.getActivatedRangeActions(cState1));
        assertEquals(Set.of(pstRangeAction3), raar.getActivatedRangeActions(cState2));

        // tap and setpoint per State
        assertEquals(Map.of(pstRangeAction1, 1.1, pstRangeAction2, 1.1, pstRangeAction3, -2.1), raar.getOptimizedSetpointsOnState(pState));
        assertEquals(Map.of(pstRangeAction1, 1.1, pstRangeAction2, 1.1, pstRangeAction3, -1.1), raar.getOptimizedSetpointsOnState(cState2));
        assertEquals(Map.of(pstRangeAction1, 1, pstRangeAction2, 1, pstRangeAction3, -2), raar.getOptimizedTapsOnState(pState));
        assertEquals(Map.of(pstRangeAction1, 1, pstRangeAction2, 1, pstRangeAction3, -1), raar.getOptimizedTapsOnState(cState2));
    }

    @Test
    void test2() {

        // pstRangeAction1 is activated in preventive,
        // pstRangeAction2, on same PST, is activated in curative
        // pstRangeAction3 is activated in preventive and in both curative states

        RangeActionActivationResultImpl raar = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        raar.activate(pstRangeAction1, pState, 3.1);
        raar.activate(pstRangeAction1, cState2, 3.1); //should not be taken into account as activation with same setpoint as previous instant

        raar.activate(pstRangeAction2, cState1, 2.1);
        raar.activate(pstRangeAction2, cState2, 3.1); //should not be taken into account as activation with same setpoint as previous instant

        raar.activate(pstRangeAction3, pState, 0.0);
        raar.activate(pstRangeAction3, cState1, -3.1);
        raar.activate(pstRangeAction3, cState2, -2.1); //come back to initial tap, but diff compared to preventive

        // pstRangeAction1
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction1, pState), DOUBLE_TOLERANCE);
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction1, oState1), DOUBLE_TOLERANCE);
        assertEquals(2.1, raar.getOptimizedSetpoint(pstRangeAction1, cState1), DOUBLE_TOLERANCE);
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction1, cState2), DOUBLE_TOLERANCE);

        assertEquals(3, raar.getOptimizedTap(pstRangeAction1, pState), DOUBLE_TOLERANCE);
        assertEquals(3, raar.getOptimizedTap(pstRangeAction1, oState1), DOUBLE_TOLERANCE);
        assertEquals(2, raar.getOptimizedTap(pstRangeAction1, cState1), DOUBLE_TOLERANCE);
        assertEquals(3, raar.getOptimizedTap(pstRangeAction1, cState2), DOUBLE_TOLERANCE);

        // pstRangeAction2 (same as RA 1, as same PST)
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction2, pState), DOUBLE_TOLERANCE);
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction2, oState1), DOUBLE_TOLERANCE);
        assertEquals(2.1, raar.getOptimizedSetpoint(pstRangeAction2, cState1), DOUBLE_TOLERANCE);
        assertEquals(3.1, raar.getOptimizedSetpoint(pstRangeAction2, cState2), DOUBLE_TOLERANCE);

        assertEquals(3, raar.getOptimizedTap(pstRangeAction2, pState), DOUBLE_TOLERANCE);
        assertEquals(3, raar.getOptimizedTap(pstRangeAction2, oState1), DOUBLE_TOLERANCE);
        assertEquals(2, raar.getOptimizedTap(pstRangeAction2, cState1), DOUBLE_TOLERANCE);
        assertEquals(3, raar.getOptimizedTap(pstRangeAction2, cState2), DOUBLE_TOLERANCE);

        // pstRangeAction3, activated in cState2
        assertEquals(0.0, raar.getOptimizedSetpoint(pstRangeAction3, pState), DOUBLE_TOLERANCE);
        assertEquals(0.0, raar.getOptimizedSetpoint(pstRangeAction3, oState1), DOUBLE_TOLERANCE);
        assertEquals(-3.1, raar.getOptimizedSetpoint(pstRangeAction3, cState1), DOUBLE_TOLERANCE);
        assertEquals(-2.1, raar.getOptimizedSetpoint(pstRangeAction3, cState2), DOUBLE_TOLERANCE);

        assertEquals(0, raar.getOptimizedTap(pstRangeAction3, pState), DOUBLE_TOLERANCE);
        assertEquals(0, raar.getOptimizedTap(pstRangeAction3, oState1), DOUBLE_TOLERANCE);
        assertEquals(-3, raar.getOptimizedTap(pstRangeAction3, cState1), DOUBLE_TOLERANCE);
        assertEquals(-2, raar.getOptimizedTap(pstRangeAction3, cState2), DOUBLE_TOLERANCE);

        // activations
        assertEquals(Set.of(pstRangeAction1, pstRangeAction3), raar.getActivatedRangeActions(pState));
        assertEquals(Set.of(), raar.getActivatedRangeActions(oState1));
        assertEquals(Set.of(pstRangeAction2, pstRangeAction3), raar.getActivatedRangeActions(cState1));
        assertEquals(Set.of(pstRangeAction3), raar.getActivatedRangeActions(cState2));

        // tap and setpoint per State
        assertEquals(Map.of(pstRangeAction1, 3.1, pstRangeAction2, 3.1, pstRangeAction3, 0.0), raar.getOptimizedSetpointsOnState(pState));
        assertEquals(Map.of(pstRangeAction1, 2.1, pstRangeAction2, 2.1, pstRangeAction3, -3.1), raar.getOptimizedSetpointsOnState(cState1));
        assertEquals(Map.of(pstRangeAction1, 3, pstRangeAction2, 3, pstRangeAction3, 0), raar.getOptimizedTapsOnState(pState));
        assertEquals(Map.of(pstRangeAction1, 2, pstRangeAction2, 2, pstRangeAction3, -3), raar.getOptimizedTapsOnState(cState1));
    }
}
