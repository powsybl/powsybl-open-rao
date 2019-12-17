/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
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

    private RelativeFlowThreshold relativeFlowThresholdAmps;
    private Cnec cnec1;
    private Cnec cnec2;
    private Network networkWithoutLf;
    private Network networkWithtLf;

    @Before
    public void setUp() {

        relativeFlowThresholdAmps = new RelativeFlowThreshold(
                Unit.AMPERE,
                Side.RIGHT,
                Direction.IN,
                60
        );

        cnec1 = new SimpleCnec(
                "cnec1",
                "cnec1",
                new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                relativeFlowThresholdAmps,
                new SimpleState(Optional.empty(), new Instant(0))
        );

        cnec2 = new SimpleCnec(
                "cnec2",
                "cnec2",
                new NetworkElement("FRANCE_BELGIUM_2", "FRANCE_BELGIUM_2"),
                relativeFlowThresholdAmps,
                new SimpleState(Optional.empty(), new Instant(0))
        );

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        networkWithtLf = Importers.loadNetwork("TestCase2Nodes_withLF.xiidm", getClass().getResourceAsStream("/TestCase2Nodes_withLF.xiidm"));
    }

    @Test
    public void isMinThresholdOvercome() throws Exception {
        assertFalse(relativeFlowThresholdAmps.isMinThresholdOvercome(networkWithoutLf, cnec1));
        assertFalse(relativeFlowThresholdAmps.isMinThresholdOvercome(networkWithtLf, cnec1));
    }

    @Test
    public void isMaxThresholdOvercomeWithNoSynchronization() {
        try {
            relativeFlowThresholdAmps.isMaxThresholdOvercome(networkWithoutLf, cnec1);
            fail();
        } catch (SynchronizationException ignored) {
        }
    }

    @Test
    public void synchronize() {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithoutLf);
        assertEquals(721, relativeFlowThresholdAmps.getMaxValue(), 1);
    }

    @Test
    public void isMaxThresholdOvercomeWithSynchronization() throws SynchronizationException {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithtLf); // threshold 60% * 721 A = 432 A
        assertFalse(relativeFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec1)); // on cnec1, after LF: 385 A

        cnec2.synchronize(networkWithtLf); // threshold 60% * 721 A = 432 A
        assertTrue(relativeFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec2)); // on cnec2, after LF: 770 A
    }

    @Test
    public void computeMarginInAmpsOk() throws SynchronizationException {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithtLf); // threshold 60% * 721 A = 432 A
        assertEquals(432 - 385, relativeFlowThresholdAmps.computeMargin(networkWithtLf, cnec1), 2); // on cnec1, after LF: 385 A

        cnec2.synchronize(networkWithtLf); // threshold 60% * 721 A = 432 A
        assertEquals(432 - 770, relativeFlowThresholdAmps.computeMargin(networkWithtLf, cnec2), 2); // on cnec2, after LF: 770 A

    }

    @Test
    public void computeMarginWithNoSynchronization() {
        try {
            relativeFlowThresholdAmps.computeMargin(networkWithtLf, cnec1);
            fail();
        } catch (SynchronizationException ignored) {
        }
    }

    @Test
    public void computeMarginNoData() throws SynchronizationException {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithtLf);
        try {
            relativeFlowThresholdAmps.computeMargin(networkWithoutLf, cnec1);
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void desynchronize() {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithoutLf);
        assertEquals(721, relativeFlowThresholdAmps.getMaxValue(), 1);
        cnec1.desynchronize();
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
    }
}
