/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
class RaUsageLimitsAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private CracImpl crac;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
    }

    @Test
    void testAdd() {
        // without limitations
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(preventiveInstant));
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(outageInstant));
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(curativeInstant));

        // with empty limitations
        crac.newRaUsageLimits("preventive").add();
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(preventiveInstant));

        // with fake instant
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.newRaUsageLimits("fake_instant"));
        assertEquals("The instant fake_instant does not exist in the crac.", exception.getMessage());

        // preventiveInstant with limitations
        crac.newRaUsageLimits("preventive").withMaxRa(33).add();
        assertEquals(33, crac.getRaUsageLimits(preventiveInstant).getMaxRa());
        assertEquals(Integer.MAX_VALUE, crac.getRaUsageLimits(preventiveInstant).getMaxTso());

        // curativeInstant with limitations
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxTso(4);
        raUsageLimits.setMaxRaPerTso(new HashMap<>(Map.of("FR", 41)));
        raUsageLimits.setMaxPstPerTso(new HashMap<>(Map.of("BE", 7)));
        raUsageLimits.setMaxTopoPerTso(new HashMap<>(Map.of("DE", 5)));
        raUsageLimits.setMaxElementaryActionsPerTso(new HashMap<>(Map.of("FR", 3)));
        crac.newRaUsageLimits("curative")
            .withMaxTso(raUsageLimits.getMaxTso())
            .withMaxRaPerTso(raUsageLimits.getMaxRaPerTso())
            .withMaxPstPerTso(raUsageLimits.getMaxPstPerTso())
            .withMaxTopoPerTso(raUsageLimits.getMaxTopoPerTso())
            .withMaxElementaryActionPerTso(raUsageLimits.getMaxElementaryActionsPerTso())
            .add();
        assertEquals(raUsageLimits, crac.getRaUsageLimits(curativeInstant));
    }
}
