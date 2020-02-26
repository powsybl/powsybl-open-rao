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
    private AbsoluteFlowThreshold absoluteFlowThresholdMWIn;
    private AbsoluteFlowThreshold absoluteFlowThresholdMWOut;
    private Cnec cnec1;
    private Cnec cnec2;
    private Cnec cnec4;
    private Cnec cnec5;
    private Network networkWithoutLf;

    @Before
    public void setUp() {
        absoluteFlowThresholdAmps = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        absoluteFlowThresholdMW = new AbsoluteFlowThreshold(MEGAWATT, Side.LEFT, Direction.BOTH, 1500.0);
        absoluteFlowThresholdMWIn = new AbsoluteFlowThreshold(MEGAWATT, Side.LEFT, Direction.OPPOSITE, 1500.0);
        absoluteFlowThresholdMWOut = new AbsoluteFlowThreshold(MEGAWATT, Side.LEFT, Direction.DIRECT, 1500.0);

        cnec1 = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"),
                absoluteFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec2 = new SimpleCnec("cnec2", new NetworkElement("FRANCE_BELGIUM_1"),
                absoluteFlowThresholdMW, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec4 = new SimpleCnec("cnec4", new NetworkElement("FRANCE_BELGIUM_2"),
                absoluteFlowThresholdMWIn, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        cnec5 = new SimpleCnec("cnec5", new NetworkElement("FRANCE_BELGIUM_2"),
                absoluteFlowThresholdMWOut, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
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
            fail();
        } catch (FaraoException e) {
            // should throw
        }
        try {
            // forbidden value
            new AbsoluteFlowThreshold(AMPERE, Side.LEFT, Direction.BOTH, -500);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void getMinMaxThresholdWithUnit() throws SynchronizationException {
        absoluteFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);
        absoluteFlowThresholdMW.synchronize(networkWithoutLf, cnec2);
        absoluteFlowThresholdMWIn.synchronize(networkWithoutLf, cnec4);
        absoluteFlowThresholdMWOut.synchronize(networkWithoutLf, cnec5);

        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(346.4, absoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(2165.1, absoluteFlowThresholdMW.getMaxThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(1500.0, absoluteFlowThresholdMW.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);

        assertEquals(-500.0, absoluteFlowThresholdAmps.getMinThreshold(AMPERE).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);
        assertEquals(-346.4, absoluteFlowThresholdAmps.getMinThreshold(MEGAWATT).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);
        assertEquals(-2165.1, absoluteFlowThresholdMW.getMinThreshold(AMPERE).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);
        assertEquals(-1500.0, absoluteFlowThresholdMW.getMinThreshold(MEGAWATT).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);

        assertEquals(-1500.0, absoluteFlowThresholdMWIn.getMinThreshold(MEGAWATT).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);
        assertEquals(Double.POSITIVE_INFINITY, absoluteFlowThresholdMWIn.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(Double.NEGATIVE_INFINITY, absoluteFlowThresholdMWOut.getMinThreshold(MEGAWATT).orElse(Double.NEGATIVE_INFINITY), DOUBLE_TOL);
        assertEquals(1500.0, absoluteFlowThresholdMWOut.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
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
