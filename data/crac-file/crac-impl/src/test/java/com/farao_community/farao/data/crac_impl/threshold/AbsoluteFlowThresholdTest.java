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

import static com.farao_community.farao.data.crac_api.Unit.AMPERE;
import static com.farao_community.farao.data.crac_api.Unit.MEGAWATT;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguino at rte-france.com>}
 */
public class AbsoluteFlowThresholdTest {

    private static final double DOUBLE_TOL = 0.5;

    private AbsoluteFlowThreshold absoluteFlowThresholdAmps;
    private AbsoluteFlowThreshold absoluteFlowThresholdMW;
    private Cnec cnec1;
    private Cnec cnec2;
    private Cnec cnec3;
    private Network networkWithoutLf;
    private Network networkWithLf;

    @Before
    public void setUp() {
        absoluteFlowThresholdAmps = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        absoluteFlowThresholdMW = new AbsoluteFlowThreshold(MEGAWATT, Side.LEFT, Direction.BOTH, 1500.0);

        cnec1 = new SimpleCnec("cnec1", "cnec1", new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                absoluteFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec2 = new SimpleCnec("cnec2", "cnec2", new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_2"),
                absoluteFlowThresholdMW, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec3 = new SimpleCnec("cnec3", "cnec3", new NetworkElement("FRANCE_BELGIUM_2", "FRANCE_BELGIUM_2"),
                absoluteFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        networkWithLf = Importers.loadNetwork("TestCase2Nodes_withLF.xiidm", getClass().getResourceAsStream("/TestCase2Nodes_withLF.xiidm"));
    }

    @Test
    public void getPhysicalParameter() {
        assertEquals(PhysicalParameter.FLOW, absoluteFlowThresholdAmps.getPhysicalParameter());
    }

    @Test
    public void forbiddenThresholdConstruction() {
        try {
            // forbidden unit
            new AbsoluteFlowThreshold(Unit.KILOVOLT, Side.LEFT, Direction.BOTH, 500);
        } catch (FaraoException e) {
            // should throw
        }
        try {
            // forbidden value
            new AbsoluteFlowThreshold(AMPERE, Side.LEFT, Direction.BOTH, -500);
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void getMinMaxThresholdWithUnit() throws SynchronizationException {
        absoluteFlowThresholdAmps.synchronize(networkWithLf, cnec1);
        absoluteFlowThresholdMW.synchronize(networkWithLf, cnec2);

        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxThreshold(AMPERE).orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(346.4, absoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT).orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(2165.1, absoluteFlowThresholdMW.getMaxThreshold(AMPERE).orElse(Double.MAX_VALUE), DOUBLE_TOL);
        assertEquals(1500.0, absoluteFlowThresholdMW.getMaxThreshold(MEGAWATT).orElse(Double.MAX_VALUE), DOUBLE_TOL);

        assertEquals(-500.0, absoluteFlowThresholdAmps.getMinThreshold(AMPERE).orElse(Double.MIN_VALUE), DOUBLE_TOL);
        assertEquals(-346.4, absoluteFlowThresholdAmps.getMinThreshold(MEGAWATT).orElse(Double.MIN_VALUE), DOUBLE_TOL);
        assertEquals(-2165.1, absoluteFlowThresholdMW.getMinThreshold(AMPERE).orElse(Double.MIN_VALUE), DOUBLE_TOL);
        assertEquals(-1500.0, absoluteFlowThresholdMW.getMinThreshold(MEGAWATT).orElse(Double.MIN_VALUE), DOUBLE_TOL);
    }

    @Test
    public void getMinMaxThresholdWithUnauthorizedUnit() throws SynchronizationException {
        try {
            absoluteFlowThresholdAmps.getMaxThreshold(Unit.KILOVOLT);
            fail();
        } catch (FaraoException e) {
            //should throw
        }
    }

    @Test
    public void getMinMaxThresholdWithUnitUnsynchronized() {
        try {
            absoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT);
            fail();
        } catch (SynchronizationException e) {
            // should throw, conversion cannot be made if voltage level has not been synchronised
        }
    }

    @Test
    public void isMinThresholdOvercome() throws SynchronizationException {
        // on cnec 1, after LF: 384.9 A
        assertFalse(absoluteFlowThresholdAmps.isMinThresholdOvercome(networkWithLf, cnec1));
    }

    @Test
    public void isMaxThresholdOvercomeOk() throws SynchronizationException {
        // on cnec 1, after LF: 384.9 A
        // on cnec 3, after LF: 769.8 A
        assertFalse(absoluteFlowThresholdAmps.isMaxThresholdOvercome(networkWithLf, cnec1));
        assertTrue(absoluteFlowThresholdAmps.isMaxThresholdOvercome(networkWithLf, cnec3));
    }

    @Test
    public void computeMarginOk() throws Exception {
        // on cnec 1, after LF: 384.9 A
        // on cnec 2, after LF: - 384.9 A = - 266.7 MW
        // on cnec 3, after LF: 769.8 A
        assertEquals(500.0 - 384.9, absoluteFlowThresholdAmps.computeMargin(networkWithLf, cnec1), DOUBLE_TOL);
        assertEquals(-266.7 - (-1500), absoluteFlowThresholdMW.computeMargin(networkWithLf, cnec2), DOUBLE_TOL);
        assertEquals(500.0 - 769.8, absoluteFlowThresholdAmps.computeMargin(networkWithLf, cnec3), DOUBLE_TOL);
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
    public void computeMarginDisconnectedLine() throws Exception {
        // terminal 1 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, absoluteFlowThresholdAmps.computeMargin(networkWithLf, cnec3), DOUBLE_TOL);
        // terminal 2 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().connect();
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal2().disconnect();
        assertEquals(500.0 - 0.0, absoluteFlowThresholdAmps.computeMargin(networkWithLf, cnec3), DOUBLE_TOL);
        // both terminal disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, absoluteFlowThresholdAmps.computeMargin(networkWithLf, cnec3), DOUBLE_TOL);
    }

    @Test
    public void synchronize() {
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
        cnec1.synchronize(networkWithoutLf);
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }

    @Test
    public void desynchronize() {
        cnec1.synchronize(networkWithoutLf);
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
        cnec1.desynchronize();
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }
}
