/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RelativeFlowThresholdTest {

    private RelativeFlowThreshold relativeFlowThreshold;
    private Cnec cnec;
    private Network network;

    @Before
    public void setUp() {
        relativeFlowThreshold = new RelativeFlowThreshold(
                Unit.AMPERE,
                Side.RIGHT,
                Direction.IN,
                30
        );
        cnec = new SimpleCnec(
                "cnec",
                "cnec",
                new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                relativeFlowThreshold,
                new SimpleState(Optional.empty(), new Instant(0))
        );
        network = Importers.loadNetwork("4_2nodes_RD_N-1.xiidm", getClass().getResourceAsStream("/4_2nodes_RD_N-1.xiidm"));
    }

    @Test
    public void isMinThresholdOvercome() throws Exception {
        assertFalse(relativeFlowThreshold.isMinThresholdOvercome(network, cnec));
    }

    @Test
    public void isMaxThresholdOvercome() {
    }

    @Test
    public void isMaxThresholdOvercomeWithNoSynchronization() {
        try {
            relativeFlowThreshold.isMaxThresholdOvercome(network, cnec);
            fail();
        } catch (SynchronizationException ignored) {
        }
    }

    @Test
    public void synchronize() {
        assertTrue(Double.isNaN(relativeFlowThreshold.getMaxValue()));
        cnec.synchronize(network);
        assertEquals(721, relativeFlowThreshold.getMaxValue(), 1);
    }

    @Test
    public void desynchronize() {
        assertTrue(Double.isNaN(relativeFlowThreshold.getMaxValue()));
        cnec.synchronize(network);
        assertEquals(721, relativeFlowThreshold.getMaxValue(), 1);
        cnec.desynchronize();
        assertTrue(Double.isNaN(relativeFlowThreshold.getMaxValue()));
    }
}
