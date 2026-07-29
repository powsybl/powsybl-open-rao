/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Injection;
import com.powsybl.openrao.data.crac.api.Instant;
import org.mockito.Mockito;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
abstract class AbstractTest {
    protected Instant prevInstant = mockInstant("preventive");
    protected Instant outInstant = mockInstant("outage");
    protected Instant cur1Instant = mockInstant("cur1");
    protected Instant cur2Instant = mockInstant("cur2");
    protected Injection<?> generator = mockInjection(IdentifiableType.GENERATOR);
    protected Injection<?> load = mockInjection(IdentifiableType.LOAD);

    private Instant mockInstant(String id) {
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.getId()).thenReturn(id);
        if (Objects.equals(id, "preventive")) {
            Mockito.when(instant.isPreventive()).thenReturn(true);
        }
        return instant;
    }

    private Injection<?> mockInjection(IdentifiableType type) {
        Injection<?> injection = Mockito.mock(Injection.class);
        Mockito.when(injection.getType()).thenReturn(type);
        return injection;
    }
}
