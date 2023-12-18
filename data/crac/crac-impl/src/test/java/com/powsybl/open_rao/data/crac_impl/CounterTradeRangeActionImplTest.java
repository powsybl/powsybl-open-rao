/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.InstantKind;
import com.powsybl.open_rao.data.crac_api.range_action.CounterTradeRangeAction;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.open_rao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CounterTradeRangeActionImplTest {
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
    void applyTest() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("counterTradeRangeAction")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .add();
        Exception e = assertThrows(FaraoException.class, () -> counterTradeRangeAction.apply(network, 100.));
        assertEquals("Can't apply a counter trade range action on a network", e.getMessage());
    }

    @Test
    void getMinMaxAdmissibleSetpointTest() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("injectionRangeActionId")
                .newRange().withMin(-1000).withMax(1000).add()
                .newRange().withMin(-1300).withMax(400).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals(-1000, counterTradeRangeAction.getMinAdmissibleSetpoint(0.0), 1e-3);
        assertEquals(400, counterTradeRangeAction.getMaxAdmissibleSetpoint(0.0), 1e-3);
    }

    @Test
    void getCurrentSetpointTest() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("injectionRangeActionId")
                .newRange().withMin(-1000).withMax(1000).add()
                .newRange().withMin(-1300).withMax(400).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        assertEquals(0, counterTradeRangeAction.getCurrentSetpoint(network));

    }
}
