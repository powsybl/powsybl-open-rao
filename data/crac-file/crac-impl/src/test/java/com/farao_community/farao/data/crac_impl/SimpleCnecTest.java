/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCnecTest {

    private final static double DOUBLE_TOLERANCE = 0.01;

    private Cnec cnec1;
    private Cnec cnec2;
    private AbstractFlowThreshold threshold;

    private Network networkWithLf;
    private Network networkWithoutLf;

    @Before
    public void setUp() {

        // mock threshold
        threshold = Mockito.mock(AbstractFlowThreshold.class);
        Mockito.when(threshold.getBranchSide()).thenReturn(Branch.Side.ONE);
        State state = Mockito.mock(State.class);

        // arrange Cnecs
        cnec1 = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), threshold, state);
        cnec2 = new SimpleCnec("cnec2", new NetworkElement("FRANCE_BELGIUM_2"), threshold, state);

        // create LF
        networkWithoutLf = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        networkWithLf = Importers.loadNetwork("TestCase2Nodes_withLF.xiidm", getClass().getResourceAsStream("/TestCase2Nodes_withLF.xiidm"));
    }

    @Test
    public void getIOkTest() {
        assertEquals(384.90, cnec1.getI(networkWithLf), DOUBLE_TOLERANCE);
        assertEquals(769.80, cnec2.getI(networkWithLf), DOUBLE_TOLERANCE);
    }

    @Test
    public void getINoData() {
        try {
            cnec1.getP(networkWithoutLf);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void getPOkTest() {
        assertEquals(-266.66, cnec1.getP(networkWithLf), DOUBLE_TOLERANCE);
        assertEquals(-533.33, cnec2.getP(networkWithLf), DOUBLE_TOLERANCE);
    }

    @Test
    public void getPNoData() {
        try {
            cnec1.getP(networkWithoutLf);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void computeMarginInAmpereOk() throws SynchronizationException {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.AMPERE);

        // flow = 384.90 A
        // threshold = [-500;500]
        Mockito.when(threshold.getMinThreshold(Unit.AMPERE)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.AMPERE)).thenReturn(Optional.of(500.0));
        assertEquals(500.0 - 384.90, cnec1.computeMargin(networkWithLf), DOUBLE_TOLERANCE);

        // flow = 384.90 I
        // threshold = [-inf ; 300]
        Mockito.when(threshold.getMinThreshold(Unit.AMPERE)).thenReturn(Optional.empty());
        Mockito.when(threshold.getMaxThreshold(Unit.AMPERE)).thenReturn(Optional.of(300.0));
        assertEquals(300.0 - 384.90, cnec1.computeMargin(networkWithLf), DOUBLE_TOLERANCE);
    }

    @Test
    public void computeMarginInMegawattOk() throws SynchronizationException {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.MEGAWATT);

        // flow = -266.67 MW
        // threshold = [-500;500]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(500.0));
        assertEquals(500.0 - 266.67, cnec1.computeMargin(networkWithLf), DOUBLE_TOLERANCE);

        // flow = -266.67 MW
        // threshold = [-inf ; 300]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(300.0));
        assertEquals(300.0 + 266.67, cnec1.computeMargin(networkWithLf), DOUBLE_TOLERANCE);

        // flow = -266.67 MW
        // threshold = [-300 ; +inf]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-300.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.empty());
        assertEquals(300.0 - 266.67, cnec1.computeMargin(networkWithLf), DOUBLE_TOLERANCE);
    }

    @Test
    public void computeMarginDisconnectedBranch() throws SynchronizationException {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.MEGAWATT);
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(500.0));

        // terminal 1 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(networkWithLf), DOUBLE_TOLERANCE);

        // terminal 2 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().connect();
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal2().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(networkWithLf), DOUBLE_TOLERANCE);

        // both terminal disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(networkWithLf), DOUBLE_TOLERANCE);
    }
}
