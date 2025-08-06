/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.iidm;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IidmInjectionHelperTest {
    @Test
    void getInjectionSetpointFromNetwork() {
        Network network = Mockito.mock(Network.class);
        Generator generator = Mockito.mock(Generator.class);
        Mockito.when(generator.getTargetP()).thenReturn(10.0);
        Mockito.when(network.getGenerator("BBE2AA11_Generator")).thenReturn(generator);
        Load load = Mockito.mock(Load.class);
        Mockito.when(load.getP0()).thenReturn(20.0);
        Mockito.when(network.getLoad("FFR3AA11_Load")).thenReturn(load);
        Double initialSetpoint = IidmInjectionHelper.getCurrentSetpoint(network, Map.of("BBE2AA11_Generator", 1., "FFR3AA11_Load", -2.));
        assertEquals(10., initialSetpoint);
    }
}
