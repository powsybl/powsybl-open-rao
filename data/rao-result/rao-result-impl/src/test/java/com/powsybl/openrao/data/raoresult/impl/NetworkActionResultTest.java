/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionResultTest {

    private Crac crac;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = CommonCracCreation.create();
        curativeInstant = crac.getInstant("curative");
    }

    @Test
    void defaultValuesTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();

        assertFalse(networkActionResult.isActivated(crac.getPreventiveState()));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", curativeInstant)));
        assertTrue(networkActionResult.getStatesWithActivation().isEmpty());
    }

    @Test
    void activatedInOnePreventiveTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();
        networkActionResult.addActivationForState(crac.getPreventiveState());

        assertTrue(networkActionResult.isActivated(crac.getPreventiveState()));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", curativeInstant)));
        assertEquals(1, networkActionResult.getStatesWithActivation().size());
    }

    @Test
    void activatedInTwoCurativeStatesTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();
        networkActionResult.addActivationForStates(Set.of(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getState("Contingency FR1 FR2", curativeInstant)));

        assertFalse(networkActionResult.isActivated(crac.getPreventiveState()));
        assertTrue(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", curativeInstant)));
        assertTrue(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", curativeInstant)));
        assertEquals(2, networkActionResult.getStatesWithActivation().size());
    }
}
