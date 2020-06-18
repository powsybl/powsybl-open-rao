/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        Mockito.when(threshold.copy()).thenReturn(threshold);
        Mockito.when(threshold.getBranchSide()).thenReturn(Branch.Side.ONE);
        State state = Mockito.mock(State.class);

        // arrange Cnecs
        cnec1 = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), Collections.singleton(threshold), state);
        cnec2 = new SimpleCnec("cnec2", new NetworkElement("FRANCE_BELGIUM_2"), Collections.singleton(threshold), state);

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
    public void computeMarginInAmpereOk() {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.AMPERE);
        Mockito.when(threshold.getPhysicalParameter()).thenReturn(PhysicalParameter.FLOW);

        double flow = 384.90; //A
        // threshold = [-500;500]
        Mockito.when(threshold.getMinThreshold(Unit.AMPERE)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.AMPERE)).thenReturn(Optional.of(500.0));
        assertEquals(500.0 - flow, cnec1.computeMargin(flow, Unit.AMPERE), DOUBLE_TOLERANCE);

        // threshold = [-inf ; 300]
        Mockito.when(threshold.getMinThreshold(Unit.AMPERE)).thenReturn(Optional.empty());
        Mockito.when(threshold.getMaxThreshold(Unit.AMPERE)).thenReturn(Optional.of(300.0));
        assertEquals(300.0 - flow, cnec1.computeMargin(flow, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void computeMarginInMegawattOk() {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.MEGAWATT);
        Mockito.when(threshold.getPhysicalParameter()).thenReturn(PhysicalParameter.FLOW);

        double flow = -266.67; //MW
        // threshold = [-500;500]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(500.0));
        assertEquals(flow - (-500.0), cnec1.computeMargin(flow, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        // threshold = [-inf ; 300]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(300.0));
        assertEquals(300.0 - flow, cnec1.computeMargin(flow, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        // threshold = [-300 ; +inf]
        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-300.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.empty());
        assertEquals(flow - (-300.0), cnec1.computeMargin(flow, Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void computeMarginDisconnectedBranch() {

        Mockito.when(threshold.getUnit()).thenReturn(Unit.MEGAWATT);
        Mockito.when(threshold.getPhysicalParameter()).thenReturn(PhysicalParameter.FLOW);

        Mockito.when(threshold.getMinThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(-500.0));
        Mockito.when(threshold.getMaxThreshold(Unit.MEGAWATT)).thenReturn(Optional.of(500.0));

        // terminal 1 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(cnec2.getP(networkWithLf), Unit.MEGAWATT), DOUBLE_TOLERANCE);

        // terminal 2 disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().connect();
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal2().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(cnec2.getP(networkWithLf), Unit.MEGAWATT), DOUBLE_TOLERANCE);

        // both terminal disconnected
        networkWithLf.getBranch("FRANCE_BELGIUM_2").getTerminal1().disconnect();
        assertEquals(500.0 - 0.0, cnec2.computeMargin(cnec2.getP(networkWithLf), Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void synchronizeTwoCnecsCreatedWithSameThresholdObject() {
        State state = Mockito.mock(State.class);
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, 50);
        Cnec cnecOnLine1 = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), Collections.singleton(relativeFlowThreshold), state);
        Cnec cnecOnLine2 = new SimpleCnec("cnec2", new NetworkElement("FRANCE_BELGIUM_2"), Collections.singleton(relativeFlowThreshold), state);

        Network network = Importers.loadNetwork(
            "TestCase2Nodes_withLF_withDifferentLimits.xiidm",
            getClass().getResourceAsStream("/TestCase2Nodes_withLF_withDifferentLimits.xiidm"));

        cnecOnLine2.synchronize(network);
        cnecOnLine1.synchronize(network);

        assertEquals(400, cnecOnLine1.getMaxThreshold(Unit.AMPERE).get(), 0.1);
        assertEquals(250, cnecOnLine2.getMaxThreshold(Unit.AMPERE).get(), 0.1);
    }

    @Test
    public void severalThresholdTest1() {
        State state = Mockito.mock(State.class);
        AbsoluteFlowThreshold directThreshold = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500);
        AbsoluteFlowThreshold oppositeThreshold = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200);

        Set<AbstractThreshold> thresholds = new HashSet<>();
        thresholds.add(directThreshold);
        thresholds.add(oppositeThreshold);

        Cnec cnec = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), thresholds, state);

        assertEquals(500, cnec.getMaxThreshold(Unit.MEGAWATT).get(), 0.1);
        assertEquals(-200, cnec.getMinThreshold(Unit.MEGAWATT).get(), 0.1);
    }

    @Test
    public void severalThresholdTest2() {
        State state = Mockito.mock(State.class);
        AbsoluteFlowThreshold directThreshold1 = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.DIRECT, 500);
        AbsoluteFlowThreshold oppositeThreshold1 = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.OPPOSITE, 200);
        AbsoluteFlowThreshold directThreshold2 = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.DIRECT, 490);
        AbsoluteFlowThreshold oppositeThreshold2 = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.RIGHT, Direction.OPPOSITE, 210);

        Set<AbstractThreshold> thresholds = new HashSet<>();
        thresholds.add(directThreshold1);
        thresholds.add(oppositeThreshold1);
        thresholds.add(directThreshold2);

        SimpleCnec cnec = new SimpleCnec("cnec1", new NetworkElement("FRANCE_BELGIUM_1"), thresholds, state);
        cnec.addThreshold(oppositeThreshold2);

        assertEquals(490, cnec.getMaxThreshold(Unit.MEGAWATT).get(), 0.1);
        assertEquals(-200, cnec.getMinThreshold(Unit.MEGAWATT).get(), 0.1);
    }
}
