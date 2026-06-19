/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
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
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .withExportingArea("FR")
                .withImportingArea("DE")
                .add();
        Exception e = assertThrows(OpenRaoException.class, () -> counterTradeRangeAction.apply(network, 100.));
        assertEquals("Can't apply a counter trade range action on a network", e.getMessage());
    }

    @Test
    void getMinMaxAdmissibleSetpointTest() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("injectionRangeActionId")
                .newRange().withMin(-1000).withMax(1000).add()
                .newRange().withMin(-1300).withMax(400).add()
                .withExportingArea("FR")
                .withImportingArea("DE")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
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
                .withExportingArea("FR")
                .withImportingArea("DE")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();
        assertEquals(0, counterTradeRangeAction.getCurrentSetpoint(network));

    }

    @Test
    void testEquals() {
        CounterTradeRangeActionImpl ctFrEs1 = new CounterTradeRangeActionImpl("CT", "CT", "FR", null, Set.of(), List.of(new StandardRangeImpl(-1000.0, 1000.0, RangeType.ABSOLUTE)), 0.0, null, null, null, "FR", "ES");
        CounterTradeRangeActionImpl ctFrEs2 = new CounterTradeRangeActionImpl("CT", "CT", "FR", null, Set.of(), List.of(new StandardRangeImpl(-1000.0, 1000.0, RangeType.ABSOLUTE)), 0.0, null, null, null, "FR", "ES");
        CounterTradeRangeActionImpl ctFrEs3 = new CounterTradeRangeActionImpl("CT", "CT", "FR", null, Set.of(), List.of(new StandardRangeImpl(-2000.0, 2000.0, RangeType.ABSOLUTE)), 0.0, null, null, null, "FR", "ES");
        CounterTradeRangeActionImpl ctPtEs = new CounterTradeRangeActionImpl("CT", "CT", "PT", null, Set.of(), List.of(new StandardRangeImpl(-1000.0, 1000.0, RangeType.ABSOLUTE)), 0.0, null, null, null, "PT", "ES");
        CounterTradeRangeActionImpl ctFrNl = new CounterTradeRangeActionImpl("CT", "CT", "FR", null, Set.of(), List.of(new StandardRangeImpl(-1000.0, 1000.0, RangeType.ABSOLUTE)), 0.0, null, null, null, "FR", "NL");

        assertEquals(ctFrEs1, ctFrEs1);
        assertEquals(ctFrEs1, ctFrEs2);
        assertNotEquals(ctFrEs1, ctFrEs3);
        assertNotEquals(ctFrEs1, ctPtEs);
        assertNotEquals(ctFrEs1, ctFrNl);
    }
}
