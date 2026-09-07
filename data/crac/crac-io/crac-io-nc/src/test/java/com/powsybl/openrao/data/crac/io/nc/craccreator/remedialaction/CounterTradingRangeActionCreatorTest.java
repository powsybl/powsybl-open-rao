/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class CounterTradingRangeActionCreatorTest {

    private static final String FR_AREA = "FR";
    private static final String ES_AREA = "ES";

    @Test
    void importCounterTradingRangeActions() {
        CracCreationParameters cracCreationParameters = NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension();
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setConnectedAreas(List.of(
                new NcCracCreationParameters.ConnectedArea(ES_AREA, List.of(new NcCracCreationParameters.BorderRange("relative", -500.0, 500.0)))
        ));

        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(
                "/profiles/remedialactions/CountertradeRemedialActions.zip",
                NcCracCreationTestUtil.NETWORK,
                cracCreationParameters
        );

        List<CounterTradeRangeAction> importedCountertradeActions = cracCreationContext.getCrac().getRangeActions().stream()
                .filter(CounterTradeRangeAction.class::isInstance)
                .map(CounterTradeRangeAction.class::cast)
                .sorted(Comparator.comparing(CounterTradeRangeAction::getId))
                .toList();

        assertEquals(4, importedCountertradeActions.size());

        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.getFirst(),
                "remedial-action-11",
                "RA11 COUNTERTRADING SWE",
                4000,
                3000,
                "RTE"
        );
        assertEquals(FR_AREA, importedCountertradeActions.getFirst().getArea());
        assertEquals(1, importedCountertradeActions.getFirst().getConnectedAreas().size());
        assertEquals(ES_AREA, importedCountertradeActions.getFirst().getConnectedAreas().getFirst().getArea());

        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(1),
                "remedial-action-12",
                "RA12 COUNTERTRADING BASELINE",
                3500,
                1500,
                "RTE"
        );
        assertEquals(FR_AREA, importedCountertradeActions.get(1).getArea());

        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(2),
                "remedial-action-13",
                "RA13 COUNTERTRADING ONLY-UP",
                2500,
                -5000,
                "RTE"
        );
        assertEquals(FR_AREA, importedCountertradeActions.get(2).getArea());

        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(
                importedCountertradeActions.get(3),
                "remedial-action-14",
                "RA14 COUNTERTRADING ONLY-DOWN",
                5000,
                1200,
                "RTE"
        );
        assertEquals(FR_AREA, importedCountertradeActions.get(3).getArea());

        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-15",
                ImportStatus.NOT_FOR_RAO,
                "Remedial action remedial-action-15 will not be imported it is not set to be available."
        );
        NcCracCreationTestUtil.assertRaNotImported(
                cracCreationContext,
                "remedial-action-16",
                ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action remedial-action-16 will not be imported because the bidding zone code XXXXX-XXX------X is invalid."
        );

    }
}
