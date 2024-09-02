/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class InjectionRangeActionImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Network network;
    private Crac crac;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
    }

    @Test
    void applyOnTwoGeneratorsTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(1., "BBE1AA1 _generator")
                .withNetworkElementAndKey(-1., "DDE3AA1 _generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        // set to 100 MW
        ira.apply(network, 100.);
        assertEquals(100., network.getGenerator("BBE1AA1 _generator").getTargetP(), 1e-3);
        assertEquals(-100., network.getGenerator("DDE3AA1 _generator").getTargetP(), 1e-3);
        assertEquals(100., ira.getCurrentSetpoint(network), 1e-3);

        // set to -450 MW
        ira.apply(network, -450);
        assertEquals(-450, network.getGenerator("BBE1AA1 _generator").getTargetP(), 1e-3);
        assertEquals(450, network.getGenerator("DDE3AA1 _generator").getTargetP(), 1e-3);
        assertEquals(-450, ira.getCurrentSetpoint(network), 1e-3);
    }

    @Test
    void applyOnCombinationOfLoadAndGeneratorsTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(0.2, "BBE3AA1 _load")
                .withNetworkElementAndKey(0.3, "FFR2AA1 _generator")
                .withNetworkElementAndKey(0.5, "NNL3AA1 _load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        // set to 100 MW
        ira.apply(network, 100.);
        assertEquals(-20, network.getLoad("BBE3AA1 _load").getP0(), 1e-3);
        assertEquals(30., network.getGenerator("FFR2AA1 _generator").getTargetP(), 1e-3);
        assertEquals(-50., network.getLoad("NNL3AA1 _load").getP0(), 1e-3);
        assertEquals(100., ira.getCurrentSetpoint(network), 1e-3);

        // set to -200 MW
        ira.apply(network, -200);
        assertEquals(40, network.getLoad("BBE3AA1 _load").getP0(), 1e-3);
        assertEquals(-60., network.getGenerator("FFR2AA1 _generator").getTargetP(), 1e-3);
        assertEquals(100., network.getLoad("NNL3AA1 _load").getP0(), 1e-3);
        assertEquals(-200, ira.getCurrentSetpoint(network), 1e-3);
    }

    @Test
    void rangeActionOnNonExistingElementTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(1, "unknown _load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        try {
            ira.apply(network, 100.);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }

        try {
            ira.getCurrentSetpoint(network);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }
    }

    @Test
    void rangeActionOnNotAnInjectionTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(1, "BBE1AA1  BBE2AA1  1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        try {
            ira.apply(network, 100.);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }

        try {
            ira.getCurrentSetpoint(network);
            fail();
        } catch (OpenRaoException e) {
            // should throw
        }
    }

    @Test
    void getCurrentSetpointTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(1., "BBE1AA1 _load")
                .withNetworkElementAndKey(1., "DDE3AA1 _generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        network.getLoad("BBE1AA1 _load").setP0(-50);
        network.getGenerator("DDE3AA1 _generator").setTargetP(50);
        assertEquals(50., ira.getCurrentSetpoint(network), 1e-3);

        network.getLoad("BBE1AA1 _load").setP0(150);
        network.getGenerator("DDE3AA1 _generator").setTargetP(-150);
        assertEquals(-150., ira.getCurrentSetpoint(network), 1e-3);

        network.getLoad("BBE1AA1 _load").setP0(150);
        network.getGenerator("DDE3AA1 _generator").setTargetP(0.);

        try {
            ira.getCurrentSetpoint(network);
            fail();
        } catch (OpenRaoException e) {
            // should throw because setpoint cannot be interpreted
        }
    }

    @Test
    void getMinMaxAdmissibleSetpointTest() {
        InjectionRangeAction ira = crac.newInjectionRangeAction()
                .withId("injectionRangeActionId")
                .withNetworkElementAndKey(1., "BBE1AA1 _load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newRange().withMin(-1300).withMax(400).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals(-1000, ira.getMinAdmissibleSetpoint(0.0), 1e-3);
        assertEquals(400, ira.getMaxAdmissibleSetpoint(0.0), 1e-3);
    }
}
