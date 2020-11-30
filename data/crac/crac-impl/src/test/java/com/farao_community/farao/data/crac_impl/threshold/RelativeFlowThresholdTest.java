/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Side;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RelativeFlowThresholdTest {
    private static final double DOUBLE_PRECISION = 0.01;

    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork(
            "TestCase12Nodes_with_Xnodes_different_imax.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_with_Xnodes_different_imax.uct"));
        UcteAliasesCreation.createAliases(network);
    }

    public void testOnSynchronize(String networkElementId, double expectedValue) {
        RelativeFlowThreshold threshold = new RelativeFlowThreshold(
            new NetworkElement(networkElementId),
            Side.LEFT,
            Direction.DIRECT,
            100,
            0);

        threshold.synchronize(network);
        assertEquals(expectedValue, threshold.getAbsoluteMax(), DOUBLE_PRECISION);
    }

    @Test
    public void testSynchronizeOnHalfLine1() {
        testOnSynchronize("FFR3AA1  X_BEFR1  1", 500);
    }

    @Test
    public void testSynchronizeOnHalfLine2() {
        testOnSynchronize("BBE2AA1  X_BEFR1  1", 1500);
    }

    @Test
    public void testSynchronizeOnTieLine() {
        // It takes most limiting threshold
        testOnSynchronize("BBE2AA1  X_BEFR1  1 + FFR3AA1  X_BEFR1  1", 500);
    }
}
