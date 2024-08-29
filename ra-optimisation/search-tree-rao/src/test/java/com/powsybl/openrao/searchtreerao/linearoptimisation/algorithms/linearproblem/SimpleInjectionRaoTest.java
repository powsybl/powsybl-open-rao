/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class SimpleInjectionRaoTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    Network network;
    Crac crac;
    RaoParameters raoParameters;

    @BeforeEach
    public void setUp() {
        network = Network.read("network/12Nodes_3gen_BE.uct", getClass().getResourceAsStream("/network/12Nodes_3gen_BE.uct"));
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));
    }

    @Test
    public void testRunRaoInjection0() {
        crac = CracImporters.importCrac("crac/small-crac-no-range-action.json",
            getClass().getResourceAsStream("/crac/small-crac-no-range-action.json"),
            network);

        crac.newInjectionRangeAction()
            .withId("injectionRangeActionId")
            .withNetworkElementAndKey(1., "BBE1AA1 _generator")
//            .withNetworkElementAndKey(-1., "DDE1AA1 _load")
//            .withInitialSetpoint(-500)
            .newRange().withMin(300).withMax(1000).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newInjectionRangeAction()
            .withId("injectionRangeActionId2")
            .withNetworkElementAndKey(1., "BBE2AA1 _generator")
            .newRange().withMin(300).withMax(1000).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newInjectionRangeAction()
            .withId("injectionRangeActionId1")
            .withNetworkElementAndKey(1., "DDE1AA1 _load")
            .newRange().withMin(-1000).withMax(-100).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInput, raoParameters);
    }

    @Test
    public void testRunRaoInjection1() {
        crac = CracImporters.importCrac("multi-ts/crac/crac-injection-ts1.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-injection-ts1.json"),
            network);

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInput, raoParameters);
    }

    @Test
    public void testRunRaoInjection2() {
        crac = CracImporters.importCrac("crac/small-crac-injection-multiple.json",
            getClass().getResourceAsStream("/crac/small-crac-injection-multiple.json"),
            network);

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInput, raoParameters);
    }

    @Test
    public void testRunRaoInjection3() {
        crac = CracImporters.importCrac("crac/small-crac-no-range-action.json",
            getClass().getResourceAsStream("/crac/small-crac-no-range-action.json"),
            network);

        crac.newInjectionRangeAction()
            .withId("injectionRangeActionId")
            .withNetworkElementAndKey(1., "BBE1AA1 _generator")
            .withNetworkElementAndKey(1., "BBE2AA1 _generator")
            .newRange().withMin(300).withMax(1000).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newInjectionRangeAction()
            .withId("injectionRangeActionId1")
            .withNetworkElementAndKey(1., "DDE1AA1 _load")
            .newRange().withMin(-1000).withMax(-100).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInput, raoParameters);
    }
}
