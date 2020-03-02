/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AlreadySynchronizedException;
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
public class RelativeFlowThresholdTest {

    private static final double DOUBLE_TOL = 0.5;

    private RelativeFlowThreshold relativeFlowThresholdAmps;
    private Network networkWithoutLf;

    @Before
    public void setUp() {
        relativeFlowThresholdAmps = new RelativeFlowThreshold(new NetworkElement("FRANCE_BELGIUM_1"), Side.RIGHT, Direction.BOTH, 60);
        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
    }

    @Test
    public void equalsWithoutNetworkElement() {
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);
        assertEquals(relativeFlowThreshold1, relativeFlowThreshold2);
    }

    @Test
    public void sameHashCodeWithoutNetworkElement() {
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);
        assertEquals(relativeFlowThreshold1.hashCode(), relativeFlowThreshold2.hashCode());
    }

    @Test
    public void equalsWithNetworkElement() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        assertEquals(relativeFlowThreshold1, relativeFlowThreshold2);
    }

    @Test
    public void sameHashCodeWithNetworkElement() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        assertEquals(relativeFlowThreshold1.hashCode(), relativeFlowThreshold2.hashCode());
    }

    @Test
    public void notEquals() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(networkElement, Side.LEFT, Direction.BOTH, 60);
        assertNotEquals(relativeFlowThreshold1, relativeFlowThreshold2);
    }

    @Test
    public void differentHashCode() {
        NetworkElement networkElement = new NetworkElement("FRANCE_BELGIUM_1");
        RelativeFlowThreshold relativeFlowThreshold1 = new RelativeFlowThreshold(networkElement, Side.RIGHT, Direction.BOTH, 60);
        RelativeFlowThreshold relativeFlowThreshold2 = new RelativeFlowThreshold(networkElement, Side.LEFT, Direction.BOTH, 60);
        assertNotEquals(relativeFlowThreshold1.hashCode(), relativeFlowThreshold2.hashCode());
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
    public void getMinMaxThresholdWithUnit() {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf);

        assertEquals(432.6, relativeFlowThresholdAmps.getMaxThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(300.0, relativeFlowThresholdAmps.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);

        assertEquals(-432.6, relativeFlowThresholdAmps.getMinThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(-300.0, relativeFlowThresholdAmps.getMinThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
    }

    @Test
    public void getMinMaxThresholdWithUnauthorizedUnit() {
        try {
            relativeFlowThresholdAmps.synchronize(networkWithoutLf);
            relativeFlowThresholdAmps.getMaxThreshold(Unit.KILOVOLT);
            fail();
        } catch (FaraoException e) {
            //should throw
        }
    }

    @Test
    public void getMinThresholdNotSynchronizedFails()  {
        try {
            relativeFlowThresholdAmps.getMinThreshold(MEGAWATT);
            fail();
        } catch (NotSynchronizedException e) {
            assertEquals("Relative threshold on branch FRANCE_BELGIUM_1 has not been synchronized so its absolute max value cannot be accessed", e.getMessage());
        }
    }

    @Test
    public void getMaxThresholdNotSynchronizedFails()  {
        try {
            relativeFlowThresholdAmps.getMaxThreshold(MEGAWATT);
            fail();
        } catch (NotSynchronizedException e) {
            assertEquals("Relative threshold on branch FRANCE_BELGIUM_1 has not been synchronized so its absolute max value cannot be accessed", e.getMessage());
        }
    }

    @Test
    public void synchronize() {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf);
        assertEquals(432.6, relativeFlowThresholdAmps.getAbsoluteMax(), DOUBLE_TOL);
    }

    @Test
    public void synchronizeWithNoNetworkElementFails() {
        RelativeFlowThreshold relativeFlowThresholdWithNoNetworkElement = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);
        try {
            relativeFlowThresholdWithNoNetworkElement.synchronize(networkWithoutLf);
            fail();
        } catch (FaraoException e) {
            assertEquals("Network element on threshold has not been defined", e.getMessage());
        }
    }

    @Test
    public void synchronizeWithNotExistingNetworkElementFails() {
        NetworkElement notExistingNetworkElement = new NetworkElement("notExistingNetworkElement");
        RelativeFlowThreshold relativeFlowThresholdWithNotExistingNetworkElement = new RelativeFlowThreshold(
            notExistingNetworkElement, Side.RIGHT, Direction.BOTH, 60);
        try {
            relativeFlowThresholdWithNotExistingNetworkElement.synchronize(networkWithoutLf);
            fail();
        } catch (FaraoException e) {
            assertEquals("Branch notExistingNetworkElement does not exist in the current network", e.getMessage());
        }
    }

    @Test
    public void synchronizeTwiceFails() {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf);
        try {
            relativeFlowThresholdAmps.synchronize(networkWithoutLf);
            fail();
        } catch (AlreadySynchronizedException e) {
            assertEquals("Synchronization on flow threshold has already been done", e.getMessage());
        }
    }

    @Test
    public void desynchronize() {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf);
        assertEquals(432.6, relativeFlowThresholdAmps.getAbsoluteMax(), DOUBLE_TOL);
        relativeFlowThresholdAmps.desynchronize();
        try {
            relativeFlowThresholdAmps.getAbsoluteMax();
            fail();
        } catch (NotSynchronizedException e) {
            // should throw
        }
    }

    @Test
    public void copy() {
        AbstractThreshold copiedRelativeFlowThresholdAmps = relativeFlowThresholdAmps.copy();
        assertEquals(relativeFlowThresholdAmps, copiedRelativeFlowThresholdAmps);
    }

    @Test
    public void copyNotSynchronizeStaysNotSynchronized() {
        AbstractThreshold copiedRelativeFlowThresholdAmps = relativeFlowThresholdAmps.copy();
        assertFalse(copiedRelativeFlowThresholdAmps.isSynchronized());
    }

    @Test
    public void copySynchronizeStaysSynchronized() {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf);
        AbstractThreshold copiedRelativeFlowThresholdAmps = relativeFlowThresholdAmps.copy();
        assertTrue(copiedRelativeFlowThresholdAmps.isSynchronized());
        assertEquals(relativeFlowThresholdAmps.getMaxThreshold(AMPERE), copiedRelativeFlowThresholdAmps.getMaxThreshold(AMPERE));
        assertEquals(relativeFlowThresholdAmps.getMaxThreshold(MEGAWATT), copiedRelativeFlowThresholdAmps.getMaxThreshold(MEGAWATT));
        assertEquals(relativeFlowThresholdAmps.getMinThreshold(AMPERE), copiedRelativeFlowThresholdAmps.getMinThreshold(AMPERE));
        assertEquals(relativeFlowThresholdAmps.getMinThreshold(MEGAWATT), copiedRelativeFlowThresholdAmps.getMinThreshold(MEGAWATT));
    }
}
