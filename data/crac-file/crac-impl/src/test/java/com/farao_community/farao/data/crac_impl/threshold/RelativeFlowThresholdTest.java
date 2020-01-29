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
 * @author Baptiste Seguinot {@literal <baptiste.seguino at rte-france.com>}
 */
public class RelativeFlowThresholdTest {

    private static final double DOUBLE_TOL = 0.5;

    private RelativeFlowThreshold relativeFlowThresholdAmps;
    private Cnec cnec1;
    private Cnec cnec2;
    private Network networkWithoutLf;
    private Network networkWithtLf;

    @Before
    public void setUp() {
        relativeFlowThresholdAmps = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);

        cnec1 = new SimpleCnec("cnec1", "cnec1", new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                relativeFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec2 = new SimpleCnec("cnec2", "cnec2", new NetworkElement("FRANCE_BELGIUM_2", "FRANCE_BELGIUM_2"),
                relativeFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        networkWithtLf = Importers.loadNetwork("TestCase2Nodes_withLF.xiidm", getClass().getResourceAsStream("/TestCase2Nodes_withLF.xiidm"));
    }

    @Test
    public void forbiddenThresholdConstruction() {
        try {
            // forbidden value
            new RelativeFlowThreshold(Side.LEFT, Direction.BOTH, -1);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
        try {
            // forbidden value
            new RelativeFlowThreshold(Side.LEFT, Direction.BOTH, 101);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void getMinMaxThreshold() throws SynchronizationException {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);

        // relativeFlowThresholdAmps -> 60% * 721 A = 432 A
        // relativeFlowThresholdMW -> 75% * 500 MW = 375 MW

        assertEquals(432.6, relativeFlowThresholdAmps.getMaxThreshold().orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(-432.6, relativeFlowThresholdAmps.getMinThreshold().orElse(Double.MIN_VALUE), DOUBLE_TOL);
    }

    @Test
    public void getMinMaxThresholdWithUnit() throws SynchronizationException {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);

        assertEquals(432.6, relativeFlowThresholdAmps.getMaxThreshold(Unit.AMPERE).orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(300.0, relativeFlowThresholdAmps.getMaxThreshold(Unit.MEGAWATT).orElse(Double.MAX_VALUE), DOUBLE_TOL);

        assertEquals(-432.6, relativeFlowThresholdAmps.getMinThreshold(Unit.AMPERE).orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(-300.0, relativeFlowThresholdAmps.getMinThreshold(Unit.MEGAWATT).orElse(Double.MAX_VALUE), DOUBLE_TOL);
    }

    @Test
    public void getMinMaxThresholdWithUnauthorizedUnit() throws SynchronizationException {
        try {
            relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);
            relativeFlowThresholdAmps.getMaxThreshold(Unit.KILOVOLT);
            fail();
        } catch (FaraoException e) {
            //should throw
        }
    }

    @Test
    public void getMinMaxThresholdWithUnitNotSynchronised()  {
        try {
            relativeFlowThresholdAmps.getMaxThreshold(Unit.MEGAWATT);
            fail();
        } catch (SynchronizationException e) {
            // should throw, conversion cannot be made if voltage level has not been synchronised
        }
    }

    @Test
    public void isMinThresholdOvercome() throws Exception {
        relativeFlowThresholdAmps.synchronize(networkWithtLf, cnec1);
        assertFalse(relativeFlowThresholdAmps.isMinThresholdOvercome(networkWithtLf, cnec1));
    }

    @Test
    public void isMaxThresholdOvercome() throws SynchronizationException {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));

        relativeFlowThresholdAmps.synchronize(networkWithtLf, cnec1);
        // relativeFlowThresholdAmps -> 60% * 721 A = 432.6 A
        // on cnec 1, after LF -> 384.9 A
        assertFalse(relativeFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec1));

        relativeFlowThresholdAmps.synchronize(networkWithtLf, cnec2);
        // relativeFlowThresholdAmps -> 60% * 721 A = 432.6 A
        // on cnec 2, after LF -> 769.8 A
        assertTrue(relativeFlowThresholdAmps.isMaxThresholdOvercome(networkWithtLf, cnec2));
    }

    @Test
    public void synchronize() {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);
        assertEquals(432.6, relativeFlowThresholdAmps.getMaxValue(), DOUBLE_TOL);
    }

    @Test
    public void computeMarginInAmpsOk() throws SynchronizationException {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));

        relativeFlowThresholdAmps.synchronize(networkWithtLf, cnec1);
        // relativeFlowThresholdAmps -> 60% * 721 A = 432 A
        // on cnec 1, after LF -> 384.9 A
        assertEquals(432.6 - 384.9, relativeFlowThresholdAmps.computeMargin(networkWithtLf, cnec1), DOUBLE_TOL);

        relativeFlowThresholdAmps.synchronize(networkWithtLf, cnec2);
        // relativeFlowThresholdAmps -> 60% * 721 A = 432 A
        // on cnec 2, after LF -> 769.8 A
        assertEquals(432.6 - 769.8, relativeFlowThresholdAmps.computeMargin(networkWithtLf, cnec2), DOUBLE_TOL);
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
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);
        try {
            relativeFlowThresholdAmps.computeMargin(networkWithoutLf, cnec1);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void desynchronize() {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        cnec1.synchronize(networkWithoutLf);
        assertEquals(432.6, relativeFlowThresholdAmps.getMaxValue(), DOUBLE_TOL);
        cnec1.desynchronize();
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
    }
}
