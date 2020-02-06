/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.RoundTripUtil.roundTrip;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class StateImportExportTest {

    @Test
    public void statePreventiveJsonCreator() {
        SimpleState simpleState = new SimpleState(
            Optional.empty(),
            new Instant("N", 0)
        );

        SimpleState transformedSimpleState = roundTrip(simpleState, SimpleState.class);
        assertEquals(transformedSimpleState, simpleState);
    }

    @Test
    public void stateJsonCreator() {
        SimpleState simpleState = new SimpleState(
            Optional.of(new ComplexContingency("contingencyId", Collections.singleton(new NetworkElement("neId")))),
            new Instant("N", 0)
        );

        SimpleState transformedSimpleState = roundTrip(simpleState, SimpleState.class);
        assertEquals(transformedSimpleState, simpleState);
    }
}
