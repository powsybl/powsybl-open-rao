/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultDeserializerTest {
    @Test
    void testRaoResultWithTapForNonPstRangeAction() {
        Crac crac = ExhaustiveCracCreation.create();
        InputStream raoResultStream = getClass().getResourceAsStream("/rao-result-with-tap-for-non-pst-range-action.json");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoResult.read(raoResultStream, crac));
        assertEquals("Taps can only be defined for PST range actions.", exception.getMessage());
    }

    @Test
    void testRaoResultWithUnnecessaryPstRangeActionSetPoint() {
        Crac crac = ExhaustiveCracCreation.create();
        InputStream raoResultStream = getClass().getResourceAsStream("/rao-result-with-unnecessary-pst-range-action-set-point.json");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoResult.read(raoResultStream, crac));
        assertEquals("Since version 1.8, only the taps are reported for PST range actions.", exception.getMessage());
    }
}
