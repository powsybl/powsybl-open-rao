/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class SensitivityComputerTest {
    @Test
    void testOutageInstantMustBeDefinedInTheBuilder() {
        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        Set<FlowCnec> flowCnecs = Set.of(Mockito.mock(FlowCnec.class));
        Set rangeActions = Set.of(Mockito.mock(RangeAction.class));

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(toolProvider)
            .withCnecs(flowCnecs)
            .withRangeActions(rangeActions);

        assertThrows(NullPointerException.class, sensitivityComputerBuilder::build);
    }

    @Test
    void testInstantMustBeAnOutageInTheBuilder() {
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.isOutage()).thenReturn(false);
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create();
        FaraoException exception = assertThrows(FaraoException.class, () -> sensitivityComputerBuilder.withOutageInstant(instant));
        assertEquals("The provided instant must be an outage", exception.getMessage());
    }
}
