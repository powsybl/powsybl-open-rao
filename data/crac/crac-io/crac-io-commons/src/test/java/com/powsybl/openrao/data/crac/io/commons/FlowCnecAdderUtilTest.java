/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.data.crac.io.commons.FlowCnecAdderUtil.setCurrentLimits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FlowCnecAdderUtilTest {
    private FlowCnecAdder flowCnecAdder;
    private Network network;

    @BeforeEach
    void setUp() {
        Crac crac = new CracImplFactory().create("crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE);
        flowCnecAdder = crac.newFlowCnec()
            .withId("flow-cnec")
            .withName("flow-cnec")
            .withInstant("preventive")
            .newThreshold()
            .withSide(TwoSides.ONE)
            .withMin(-1000.)
            .withMax(1000.)
            .withUnit(Unit.MEGAWATT)
            .add();
        network = Network.read("network_with_dangling_lines.xiidm", getClass().getResourceAsStream("/network_with_dangling_lines.xiidm"));
    }

    @Test
    void testCreateFlowCnecFromBranch() {
        FlowCnec flowCnec = createFlowCnec("FFR1AA2  FFR3AA2  1");
        checkIMax(flowCnec, 1000., 1000.);
    }

    @Test
    void testCreateFlowCnecFromBranchWithPatlOneSide1() {
        FlowCnec flowCnec = createFlowCnec("BBE1AA1  BBE2AA1  1");
        checkIMax(flowCnec, 5000., 2894.74);
    }

    @Test
    void testCreateFlowCnecFromBranchWithPatlOneSide2() {
        FlowCnec flowCnec = createFlowCnec("NNL1AA1  NNL3AA1  1");
        checkIMax(flowCnec, 2894.74, 5000.);
    }

    @Test
    void testCreateFlowCnecFromDanglingLine() {
        FlowCnec flowCnec = createFlowCnec("XFRDE11  DDE3AA1  1");
        checkIMax(flowCnec, 4800., 4800.);
    }

    @Test
    void testCreateFlowCnecFromMissingNetworkElement() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> createFlowCnec("unknown-element"));
        assertEquals("No branch or dangling line with id unknown-element was found in the network.", exception.getMessage());
    }

    @Test
    void testCreateFlowCnecFromBranchWithNoCurrentLimits() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> createFlowCnec("BBE1AA1  BBE3AA1  1"));
        assertEquals("Unable to get current limits from network for branch BBE1AA1  BBE3AA1  1.", exception.getMessage());
    }

    @Test
    void testCreateFlowCnecFromDanglingLineWithNoCurrentLimits() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> createFlowCnec("FFR3AA1  XBEFR11  1"));
        assertEquals("Unable to get current limits from network for dangling line FFR3AA1  XBEFR11  1.", exception.getMessage());
    }

    private FlowCnec createFlowCnec(String networkElementId) {
        flowCnecAdder.withNetworkElement(networkElementId);
        setCurrentLimits(flowCnecAdder, network, networkElementId);
        return flowCnecAdder.add();
    }

    private static void checkIMax(FlowCnec flowCnec, double iMax1, double iMax2) {
        assertEquals(iMax1, flowCnec.getIMax(TwoSides.ONE), 1e-2);
        assertEquals(iMax2, flowCnec.getIMax(TwoSides.TWO), 1e-2);
    }
}
