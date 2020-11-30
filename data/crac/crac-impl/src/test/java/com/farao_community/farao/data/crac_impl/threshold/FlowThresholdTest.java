/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Side;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowThresholdTest {
    private static final double DOUBLE_PRECISION = 1;

    private Network network;

    @Before
    public void setUp() {
        network = Importers.loadNetwork(
            "TestCase12Nodes_with_Xnodes_different_imax.uct",
            getClass().getResourceAsStream("/TestCase12Nodes_with_Xnodes_different_imax.uct"));
        UcteAliasesCreation.createAliases(network);
    }

    // TEST ON LINES

    @Test
    public void testComputeMarginOnLineAllInAmpsWithoutFrm() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.AMPERE,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.DIRECT,
            5000,
            0);

        flowThreshold.synchronize(network);
        assertEquals(2500, flowThreshold.computeMargin(2500, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineAllInMWWithFrm() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.DIRECT,
            3000,
            50);

        flowThreshold.synchronize(network);
        assertEquals(450, flowThreshold.computeMargin(2500, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithoutFrm() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.DIRECT,
            3000,
            0);

        flowThreshold.synchronize(network);
        assertEquals(2558, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithFrm() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.DIRECT,
            3000,
            50);

        flowThreshold.synchronize(network);
        assertEquals(2482, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithFrmDirectThresholdOppositeFlow() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.DIRECT,
            3000,
            0);

        flowThreshold.synchronize(network);
        assertEquals(5000, flowThreshold.computeMargin(-2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithFrmOppositeThresholdOppositeFlow() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.OPPOSITE,
            3000,
            0);

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(-2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithFrmBothThresholdOppositeFlow() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.BOTH,
            3000,
            0);

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(-2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineThresholdInMWAndMarginInAmpsWithFrmBothThresholdDirectFlow() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.LEFT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnLineSideDifference() {
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("FFR1AA1  FFR3AA1  1"),
            Side.RIGHT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    // TESTS ON TRANSFORMERS

    @Test
    public void testComputeMarginOnTransformerOnHighVoltageLevelBothInAmps() {
        // As we are looking for margin in A with threshold defined in Amps no difference if it's defined on
        // high-voltage level or low-voltage level
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.AMPERE,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.RIGHT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnTransformerOnLowVoltageLevelBothInAmps() {
        // As we are looking for margin in A with threshold defined in Amps no difference if it's defined on
        // high-voltage level or low-voltage level
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.AMPERE,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.LEFT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnTransformerOnHighVoltageLevelInMW() {
        // As we are looking for margin in MW no difference if it's defined on high-voltage level or low-voltage level
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.RIGHT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnTransformerOnLowVoltageLevelInMW() {
        // As we are looking for margin in MW no difference if it's defined on high-voltage level or low-voltage level
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.LEFT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(1000, flowThreshold.computeMargin(2000, Unit.MEGAWATT), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnTransformerOnHighVoltageLevelInAmps() {
        // Here RIGHT means high-voltage level (with UCTE and PowSyBl conventions). So with 3000MW threshold on
        // high-voltage we have a "quite low" Amps limit which is 4558A
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.RIGHT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(2558, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }

    @Test
    public void testComputeMarginOnTransformerOnLowVoltageLevelInAmps() {
        // Here LEFT means low-voltage level (with UCTE and PowSyBl conventions). So with 3000MW threshold on
        // low-voltage we have a "quite high" Amps limit which is 7872A. Which is higher than the previous limit.
        AbstractFlowThreshold flowThreshold = new AbsoluteFlowThreshold(
            Unit.MEGAWATT,
            new NetworkElement("BBE1AA1  BBE1AA2  1"),
            Side.LEFT,
            Direction.BOTH,
            3000,
            0

        );

        flowThreshold.synchronize(network);
        assertEquals(5872, flowThreshold.computeMargin(2000, Unit.AMPERE), DOUBLE_PRECISION);
    }
}
