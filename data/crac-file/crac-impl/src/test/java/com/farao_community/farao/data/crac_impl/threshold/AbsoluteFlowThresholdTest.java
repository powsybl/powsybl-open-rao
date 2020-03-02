/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

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
    private Network networkWithoutLf;

    @Before
    public void setUp() {
        NetworkElement networkElement1 = new NetworkElement("FRANCE_BELGIUM_1");
        NetworkElement networkElement2 = new NetworkElement("FRANCE_BELGIUM_2");
        absoluteFlowThresholdAmps = new AbsoluteFlowThreshold(AMPERE, networkElement1, Side.RIGHT, Direction.BOTH, 500.0);
        absoluteFlowThresholdMW = new AbsoluteFlowThreshold(MEGAWATT, networkElement1, Side.LEFT, Direction.BOTH, 1500.0);
        absoluteFlowThresholdMWIn = new AbsoluteFlowThreshold(MEGAWATT, networkElement2, Side.LEFT, Direction.OPPOSITE, 1500.0);
        absoluteFlowThresholdMWOut = new AbsoluteFlowThreshold(MEGAWATT, networkElement2, Side.LEFT, Direction.DIRECT, 1500.0);

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
    }

    @Test
    public void equalsWithoutNetworkElement() {
        AbsoluteFlowThreshold absoluteFlowThreshold1 = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        AbsoluteFlowThreshold absoluteFlowThreshold2 = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        assertEquals(absoluteFlowThreshold1, absoluteFlowThreshold2);
    }

    @Test
    public void sameHashCodeWithoutNetworkElement() {
        AbsoluteFlowThreshold absoluteFlowThreshold1 = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        AbsoluteFlowThreshold absoluteFlowThreshold2 = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        assertEquals(absoluteFlowThreshold1.hashCode(), absoluteFlowThreshold2.hashCode());
    }

    @Test
    public void equalsWithNetworkElement() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        AbsoluteFlowThreshold absoluteFlowThreshold1 = new AbsoluteFlowThreshold(AMPERE, networkElement, Side.RIGHT, Direction.BOTH, 500.0);
        AbsoluteFlowThreshold absoluteFlowThreshold2 = new AbsoluteFlowThreshold(AMPERE, networkElement, Side.RIGHT, Direction.BOTH, 500.0);
        assertEquals(absoluteFlowThreshold1, absoluteFlowThreshold2);
    }

    @Test
    public void sameHashCodeWithNetworkElement() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        AbsoluteFlowThreshold absoluteFlowThreshold1 = new AbsoluteFlowThreshold(AMPERE, networkElement, Side.RIGHT, Direction.BOTH, 500.0);
        AbsoluteFlowThreshold absoluteFlowThreshold2 = new AbsoluteFlowThreshold(AMPERE, networkElement, Side.RIGHT, Direction.BOTH, 500.0);
        assertEquals(absoluteFlowThreshold1.hashCode(), absoluteFlowThreshold2.hashCode());
    }

    @Test
    public void notEqualsOneWithNetworkElementAndNotTheOther() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        AbsoluteFlowThreshold absoluteFlowThreshold1 = new AbsoluteFlowThreshold(AMPERE, networkElement, Side.RIGHT, Direction.BOTH, 500.0);
        AbsoluteFlowThreshold absoluteFlowThreshold2 = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        assertNotEquals(absoluteFlowThreshold1, absoluteFlowThreshold2);
        assertNotEquals(absoluteFlowThreshold2, absoluteFlowThreshold1);
    }

    @Test
    public void notEquals() {
        assertNotEquals(absoluteFlowThresholdAmps, absoluteFlowThresholdMW);
    }

    @Test
    public void differentHashCode() {
        assertNotEquals(absoluteFlowThresholdAmps.hashCode(), absoluteFlowThresholdMW.hashCode());
    }

    @Test
    public void getNetworkElementFail() {
        AbsoluteFlowThreshold absoluteFlowThreshold = new AbsoluteFlowThreshold(AMPERE, Side.RIGHT, Direction.BOTH, 500.0);
        try {
            absoluteFlowThreshold.getNetworkElement();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
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
    public void getMinMaxThresholdWithUnit() {
        absoluteFlowThresholdAmps.synchronize(networkWithoutLf);
        absoluteFlowThresholdMW.synchronize(networkWithoutLf);
        absoluteFlowThresholdMWIn.synchronize(networkWithoutLf);
        absoluteFlowThresholdMWOut.synchronize(networkWithoutLf);

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
    public void getMinMaxThresholdWithUnauthorizedUnit() {
        try {
            absoluteFlowThresholdAmps.getMaxThreshold(Unit.KILOVOLT);
            fail();
        } catch (FaraoException e) {
            //should throw
        }
    }

    @Test
    public void getMinMaxThresholdWithUnitNotSynchronized() {
        try {
            absoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT);
            fail();
        } catch (NotSynchronizedException e) {
            // should throw, conversion cannot be made if voltage level has not been synchronised
        }
    }

    @Test
    public void synchronize() {
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
        absoluteFlowThresholdAmps.synchronize(networkWithoutLf);
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }

    @Test
    public void synchronizeFail() {
        absoluteFlowThresholdAmps.setNetworkElement(new NetworkElement("network_element_fail"));
        try {
            absoluteFlowThresholdAmps.synchronize(networkWithoutLf);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void desynchronize() {
        absoluteFlowThresholdAmps.synchronize(networkWithoutLf);
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
        absoluteFlowThresholdAmps.desynchronize();
        assertEquals(500.0, absoluteFlowThresholdAmps.getMaxValue(), 1);
    }

    @Test
    public void copy() {
        AbstractThreshold copiedAbsoluteFlowThresholdAmps = absoluteFlowThresholdAmps.copy();
        assertEquals(absoluteFlowThresholdAmps, copiedAbsoluteFlowThresholdAmps);
    }

    @Test
    public void copyNotSynchronizeStaysNotSynchronized() {
        AbstractThreshold copiedAbsoluteFlowThresholdAmps = absoluteFlowThresholdAmps.copy();
        assertFalse(copiedAbsoluteFlowThresholdAmps.isSynchronized());
    }

    @Test
    public void copySynchronizeStaysSynchronized() {
        absoluteFlowThresholdAmps.synchronize(networkWithoutLf);
        AbstractThreshold copiedAbsoluteFlowThresholdAmps = absoluteFlowThresholdAmps.copy();
        assertTrue(copiedAbsoluteFlowThresholdAmps.isSynchronized());
        assertEquals(absoluteFlowThresholdAmps.getMaxThreshold(AMPERE), copiedAbsoluteFlowThresholdAmps.getMaxThreshold(AMPERE));
        assertEquals(absoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT), copiedAbsoluteFlowThresholdAmps.getMaxThreshold(MEGAWATT));
        assertEquals(absoluteFlowThresholdAmps.getMinThreshold(AMPERE), copiedAbsoluteFlowThresholdAmps.getMinThreshold(AMPERE));
        assertEquals(absoluteFlowThresholdAmps.getMinThreshold(MEGAWATT), copiedAbsoluteFlowThresholdAmps.getMinThreshold(MEGAWATT));
    }
}
