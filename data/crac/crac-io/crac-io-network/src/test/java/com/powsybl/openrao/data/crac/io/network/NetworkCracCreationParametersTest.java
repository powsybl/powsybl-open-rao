/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.network.parameters.NetworkCracCreationParameters;
import com.powsybl.openrao.data.crac.io.network.parameters.RedispatchingRangeActions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NetworkCracCreationParametersTest {
    @Test
    void testGeneratorCombination() {
        RedispatchingRangeActions parameters = new NetworkCracCreationParameters().getRedispatchingRangeActions();
        Map<String, Set<String>> combinations = Map.of("combi1", Set.of("gen1", "gen2"), "combi2", Set.of("gen1", "gen3"));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.setGeneratorCombinations(combinations));
        assertEquals("A generator can only be used once in generator combinations.", exception.getMessage());
    }
}
