/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class RangeActionResultTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = CommonCracCreation.create();

        //define CNECs on Outage state so that the Crac contains outage states
        crac.newFlowCnec()
            .withId("cnec-outage-co1")
            .withNetworkElement("anyNetworkElement")
            .withContingency("Contingency FR1 FR2")
            .withInstantId(CommonCracCreation.INSTANT_OUTAGE.getId())
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();

        crac.newFlowCnec()
            .withId("cnec-outage-co2")
            .withNetworkElement("anyNetworkElement")
            .withContingency("Contingency FR1 FR3")
            .withInstantId(CommonCracCreation.INSTANT_OUTAGE.getId())
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();
    }

    @Test
    void defaultValuesTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_OUTAGE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_OUTAGE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)));

        assertEquals(Double.NaN, rangeActionResult.getInitialSetpoint());
        assertEquals(Double.NaN, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()));
    }

    @Test
    void pstActivatedInPreventiveTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)));

        // initial values
        assertEquals(0.3, rangeActionResult.getInitialSetpoint(), 1e-3);

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_OUTAGE)), 1e-3);

        // curative state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);
    }

    @Test
    void pstActivatedInCurativeTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE), -1.6);

        //is activated

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)));

        // preventive state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_OUTAGE)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);
    }

    @Test
    void pstActivatedInPreventiveAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_OUTAGE)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);
    }

    @Test
    void pstActivatedInPreventiveAndAutoAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        // Add dummy flow cnec to create auto state
        crac.newFlowCnec().withId("dummy").withContingency("Contingency FR1 FR2").withInstantId(CommonCracCreation.INSTANT_AUTO.getId()).withNetworkElement("ne")
            .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
            .add();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_OUTAGE)), 1e-3);

        // auto state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_AUTO)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CommonCracCreation.INSTANT_CURATIVE)), 1e-3);
    }
}
