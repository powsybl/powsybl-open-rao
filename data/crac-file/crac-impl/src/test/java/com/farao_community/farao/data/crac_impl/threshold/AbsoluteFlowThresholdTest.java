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
public class AbsoluteFlowThresholdTest {

    private AbsoluteFlowThreshold absoluteFlowThresholdAmps;
    private AbsoluteFlowThreshold absoluteFlowThresholdMW;
    private Cnec cnec1;
    private Cnec cnec2;
    private Network networkWithoutLf;
    private Network networkWithtLf;

    @Before
    public void setUp() {

        absoluteFlowThresholdAmps = new AbsoluteFlowThreshold(
                Unit.AMPERE,
                Side.RIGHT,
                Direction.IN,
                500
        );

        absoluteFlowThresholdMW = new AbsoluteFlowThreshold(
                Unit.MEGAWATT,
                Side.RIGHT,
                Direction.IN,
                500
        );

        cnec1 = new SimpleCnec(
                "cnec1",
                "cnec1",
                new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                absoluteFlowThresholdAmps,
                new SimpleState(Optional.empty(), new Instant(0))
        );

        cnec2 = new SimpleCnec(
                "cnec2",
                "cnec2",
                new NetworkElement("FRANCE_BELGIUM_2", "FRANCE_BELGIUM_2"),
                absoluteFlowThresholdAmps,
                new SimpleState(Optional.empty(), new Instant(0))
        );

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        networkWithtLf = Importers.loadNetwork("TestCase2Nodes_withLF.xiidm", getClass().getResourceAsStream("/TestCase2Nodes_withLF.xiidm"));
    }

    @Test
    public void isMinThresholdOvercome() throws Exception {
        assertFalse(absoluteFlowThresholdAmps.isMinThresholdOvercome(networkWithoutLf, cnec1));
        assertFalse(absoluteFlowThresholdAmps.isMinThresholdOvercome(networkWithtLf, cnec1));
    }

    @Test
    public void isMaxThresholdOvercomeOk() throws Exception {
        assertFalse(absoluteFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec1)); // on cnec1, after LF: 385 A
        assertTrue(absoluteFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec2)); // on cnec2, after LF: 770 A
    }

    @Test
    public void computeMarginOk() throws Exception {
        assertEquals(500 - 385, absoluteFlowThresholdAmps.computeMargin(networkWithtLf, cnec1), 1); // on cnec1, after LF: 385 A
        assertEquals(500 - 770, absoluteFlowThresholdAmps.computeMargin(networkWithtLf, cnec2), 1); // on cnec2, after LF: 770 A
        assertEquals(500 - 266, absoluteFlowThresholdMW.computeMargin(networkWithtLf, cnec1), 1); // on cnec1, after LF: 266 MW
        assertEquals(500 - 533, absoluteFlowThresholdMW.computeMargin(networkWithtLf, cnec2), 1); // on cnec2, after LF: 533 MW
    }

    @Test
    public void computeMarginNoData() throws Exception {
        try {
            absoluteFlowThresholdAmps.computeMargin(networkWithoutLf, cnec1);
            fail();
        } catch (FaraoException e) {
            //should throw
        }
    }

    @Test
    public void synchronize() {
        assertEquals(500, absoluteFlowThresholdAmps.getMaxValue(), 1);
        cnec1.synchronize(networkWithoutLf);
        assertEquals(500, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }

    @Test
    public void desynchronize() {
        cnec1.synchronize(networkWithoutLf);
        assertEquals(500, absoluteFlowThresholdAmps.getMaxValue(), 1);
        cnec1.desynchronize();
        assertEquals(500, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }
}
