/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRangeActionRaoTest {

    private LinearRangeActionRao linearRangeActionRao;

    @Before
    public void setUp() throws Exception {
        linearRangeActionRao = new LinearRangeActionRao();
    }

    @Test
    public void getName() {
        assertEquals("Linear Range Action Rao", linearRangeActionRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", linearRangeActionRao.getVersion());
    }

    @Test
    public void run() {
        Network network = Mockito.mock(Network.class);
        Crac crac = Mockito.mock(Crac.class);
        String variantId = "variant-test";
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);

        assertNull(linearRangeActionRao.run(network, crac, variantId, computationManager, raoParameters));
    }
}
