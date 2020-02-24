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
public class RelativeFlowThresholdTest {

    private static final double DOUBLE_TOL = 0.5;

    private RelativeFlowThreshold relativeFlowThresholdAmps;
    private Cnec cnec1;
    private Network networkWithoutLf;

    @Before
    public void setUp() {
        relativeFlowThresholdAmps = new RelativeFlowThreshold(Side.RIGHT, Direction.BOTH, 60);

        cnec1 = new SimpleCnec("cnec1", "cnec1", new NetworkElement("FRANCE_BELGIUM_1", "FRANCE_BELGIUM_1"),
                relativeFlowThresholdAmps, new SimpleState(Optional.empty(), new Instant("initial", 0)));

        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
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
    public void getMinMaxThresholdWithUnit() throws SynchronizationException {
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);

        assertEquals(432.6, relativeFlowThresholdAmps.getMaxThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(300.0, relativeFlowThresholdAmps.getMaxThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);

        assertEquals(-432.6, relativeFlowThresholdAmps.getMinThreshold(AMPERE).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
        assertEquals(-300.0, relativeFlowThresholdAmps.getMinThreshold(MEGAWATT).orElse(Double.POSITIVE_INFINITY), DOUBLE_TOL);
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
            relativeFlowThresholdAmps.getMaxThreshold(MEGAWATT);
            fail();
        } catch (SynchronizationException e) {
            // should throw, conversion cannot be made if voltage level has not been synchronised
        }
    }

    @Test
    public void synchronize() {
        assertTrue(Double.isNaN(relativeFlowThresholdAmps.getMaxValue()));
        relativeFlowThresholdAmps.synchronize(networkWithoutLf, cnec1);
        assertEquals(432.6, relativeFlowThresholdAmps.getMaxValue(), DOUBLE_TOL);
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
