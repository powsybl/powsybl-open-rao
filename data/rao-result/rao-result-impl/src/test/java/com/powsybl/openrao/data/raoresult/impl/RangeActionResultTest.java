/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class RangeActionResultTest {
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = CommonCracCreation.create();
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        //define CNECs on Outage state so that the Crac contains outage states
        crac.newFlowCnec()
                .withId("cnec-outage-co1")
                .withNetworkElement("anyNetworkElement")
                .withContingency("Contingency FR1 FR2")
                .withInstant(OUTAGE_INSTANT_ID)
                .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).withMax(1000.).add()
                .add();

        crac.newFlowCnec()
                .withId("cnec-outage-co2")
                .withNetworkElement("anyNetworkElement")
                .withContingency("Contingency FR1 FR3")
                .withInstant(OUTAGE_INSTANT_ID)
                .newThreshold().withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).withMax(1000.).add()
                .add();
    }

    @Test
    void defaultValuesTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", outageInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", outageInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", curativeInstant)));

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
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", curativeInstant)));

        // initial values
        assertEquals(0.3, rangeActionResult.getInitialSetpoint(), 1e-3);

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", outageInstant)), 1e-3);

        // curative state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", curativeInstant)), 1e-3);
    }

    @Test
    void pstActivatedInCurativeTest() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant), -1.6);

        //is activated

        assertFalse(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", curativeInstant)));

        // preventive state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", outageInstant)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", curativeInstant)), 1e-3);

        // other curative state (not activated)
        assertEquals(0.3, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", curativeInstant)), 1e-3);
    }

    @Test
    void pstActivatedInPreventiveAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", curativeInstant)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", outageInstant)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", curativeInstant)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", curativeInstant)), 1e-3);
    }

    @Test
    void pstActivatedInPreventiveAndAutoAndCurative() {
        RangeActionResult rangeActionResult = new RangeActionResult();

        // Add dummy flow cnec to create auto state
        crac.newFlowCnec().withId("dummy").withContingency("Contingency FR1 FR2").withInstant(AUTO_INSTANT_ID).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).add()
                .add();

        rangeActionResult.setInitialSetpoint(0.3);
        rangeActionResult.addActivationForState(crac.getPreventiveState(), 1.6);
        rangeActionResult.addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant), -1.6);

        //is activated

        assertTrue(rangeActionResult.isActivatedDuringState(crac.getPreventiveState()));
        assertTrue(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(rangeActionResult.isActivatedDuringState(crac.getState("Contingency FR1 FR3", curativeInstant)));
        assertEquals(2, rangeActionResult.getStatesWithActivation().size());

        // preventive state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getPreventiveState()), 1e-3);

        // outage state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", outageInstant)), 1e-3);

        // auto state
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", autoInstant)), 1e-3);

        // curative state
        assertEquals(-1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR2", curativeInstant)), 1e-3);

        // other curative state (not activated)
        assertEquals(1.6, rangeActionResult.getOptimizedSetpointOnState(crac.getState("Contingency FR1 FR3", curativeInstant)), 1e-3);
    }
}
