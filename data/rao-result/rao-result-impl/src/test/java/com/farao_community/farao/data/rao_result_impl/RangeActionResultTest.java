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
import org.junit.Before;
import org.junit.Test;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.AUTO;
import static com.farao_community.farao.data.crac_api.Instant.OUTAGE;
import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RangeActionResultTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();

        //define CNECs on Outage state so that the Crac contains outage states
        crac.newFlowCnec()
                .withId("cnec-outage-co1")
                .withNetworkElement("anyNetworkElement")
                .withContingency("Contingency FR1 FR2")
                .withInstant(OUTAGE)
                .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
                .add();

        crac.newFlowCnec()
                .withId("cnec-outage-co2")
                .withNetworkElement("anyNetworkElement")
                .withContingency("Contingency FR1 FR3")
                .withInstant(OUTAGE)
                .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
                .add();
    }

    @Test
    public void defaultValuesTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", OUTAGE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", OUTAGE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        assertEquals(Double.NaN, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()));
    }

    @Test
    public void pstActivatedInPreventiveTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        // initial values

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);
    }

    @Test
    public void pstActivatedInCurativeTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CURATIVE), -1.6);

        //is activated

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));

        // preventive state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
    }

    @Test
    public void pstActivatedInPreventiveAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CURATIVE), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
    }

    @Test
    public void pstActivatedInPreventiveAndAutoAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        // Add dummy flow cnec to create auto state
        crac.newFlowCnec().withId("dummy").withContingency("Contingency FR1 FR2").withInstant(AUTO).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", CURATIVE), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", OUTAGE)), 1e-3);

        // auto state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", AUTO)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", CURATIVE)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", CURATIVE)), 1e-3);
    }
}
