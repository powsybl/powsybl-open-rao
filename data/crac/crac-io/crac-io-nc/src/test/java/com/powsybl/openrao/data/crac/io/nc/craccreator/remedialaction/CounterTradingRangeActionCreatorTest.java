/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class CounterTradingRangeActionCreatorTest {

    @Test
    void importCounterTradingRangeActions() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(
                "/profiles/remedialactions/CountertradeRemedialActions.zip",
                NcCracCreationTestUtil.NETWORK
        );

        List<CounterTradeRangeAction> importedCountertradeActions = cracCreationContext.getCrac().getRangeActions().stream()
                .filter(CounterTradeRangeAction.class::isInstance)
                .map(CounterTradeRangeAction.class::cast)
                .sorted(Comparator.comparing(CounterTradeRangeAction::getId))
                .toList();

        assertEquals(4, importedCountertradeActions.size());

        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(0),
                "remedial-action-11",
                "RA11 COUNTERTRADING SWE",
                4000,
                3000,
                "RTE"
        );
        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(1),
                "remedial-action-12",
                "RA12 COUNTERTRADING FR BASELINE",
                3500,
                1500,
                "RTE"
        );
        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(2),
                "remedial-action-13",
                "RA13 COUNTERTRADING ES ONLY-UP",
                2500,
                -5000,
                "REE"
        );
        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(3),
                "remedial-action-14",
                "RA14 COUNTERTRADING ES ONLY-DOWN",
                5000,
                1200,
                "REE"
        );

        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-15",
                ImportStatus.NOT_FOR_RAO,
                "Remedial action remedial-action-15 will not be imported it is not set to be available."
        );
        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-16",
                ImportStatus.NOT_FOR_RAO,
                "Remedial action remedial-action-16 will not be imported because system operator 10XDE-VE-------2 is not supported."
        );
        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-17",
                ImportStatus.INCOMPLETE_DATA,
                "Remedial action remedial-action-17 will not be imported the counter trading remedial action has null operator code."
        );
        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-18",
                ImportStatus.NOT_FOR_RAO,
                "Remedial action remedial-action-18 will not be imported because border 10Y1001C--00095L is not supported for ES counter-trading."
        );

    }
}