/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.range.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.assertRaNotImported;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class CounterTradingRangeActionCreatorTest {

    private static final String CT_ARCHIVE = "/profiles/remedialactions/CountertradeRemedialActions.zip";
    private static final double FR_NET_POSITION = 500.;
    private static final String FR_TO_DE_LINE = "FFR2AA1  DDE3AA1  1";

    @Test
    void importCounterTradeRangeActionAttributes() {
        NcCracCreationContext cracCreationContext = importCrac();
        CounterTradeRangeAction counterTradeRangeAction = getCounterTradeRangeAction(cracCreationContext, "ct-economic-limits");

        assertEquals("CT-ECONOMIC-LIMITS", counterTradeRangeAction.getName());
        assertEquals("RTE", counterTradeRangeAction.getOperator());
        assertEquals("FR", counterTradeRangeAction.getArea());
        assertEquals(FR_NET_POSITION, counterTradeRangeAction.getInitialNetPosition());
        assertEquals(0., counterTradeRangeAction.getInitialSetpoint());
        assertEquals(Optional.of(10), counterTradeRangeAction.getSpeed());
        assertEquals(Set.of("BE", "DE"), counterTradeRangeAction.getConnectedAreas().stream().map(ConnectedArea::getArea).collect(Collectors.toSet()));
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "ct-economic-limits", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID);
    }

    @Test
    void importCounterTradeRangeActionsWithValidRanges() {
        NcCracCreationContext cracCreationContext = importCrac();

        assertEquals(8, cracCreationContext.getCrac().getRangeActions().stream().filter(CounterTradeRangeAction.class::isInstance).count());

        // Economic limits are the tightest on both sides
        assertRange(cracCreationContext, "ct-economic-limits", -1000, 1000);
        // SSI limits (around the FR net position) are the tightest on both sides
        assertRange(cracCreationContext, "ct-ssi-limits", -1000, 1500);
        // Economic limit is the tightest for min, SSI limit for max
        assertRange(cracCreationContext, "ct-mixed-limits", 0, 1500);
        // Only SSI limits
        assertRange(cracCreationContext, "ct-ssi-only", 300, 800);
        // Only economic limits (and no isCrossBorderRelevant)
        assertRange(cracCreationContext, "ct-economic-only", -2000, 3000);
        // SSI limit only for max
        assertRange(cracCreationContext, "ct-ssi-up-only", -4000, 1500);
        // Outdated SSI profile is ignored: the action stays available and uses economic limits only
        assertRange(cracCreationContext, "ct-outdated-ssi", -2000, 2000);

        ElementaryCreationContext notAlteredContext = cracCreationContext.getRemedialActionCreationContext("ct-mixed-limits");
        assertTrue(notAlteredContext.isImported());
        assertFalse(notAlteredContext.isAltered());
    }

    @Test
    void importCounterTradeRangeActionWithDefaultRange() {
        NcCracCreationContext cracCreationContext = importCrac();

        assertRange(cracCreationContext, "ct-no-limits", -5000, 5000);
        ElementaryCreationContext context = cracCreationContext.getRemedialActionCreationContext("ct-no-limits");
        assertTrue(context.isAltered());
        assertEquals("the minimum range was not provided. It has been set to the minimal range value of -5000.0, "
                + "the maximum range was not provided. It has been set to the maximal range value of 5000.0", context.getImportStatusDetail());
    }

    @Test
    void importCounterTradeRangeActionWithDefaultRangeFromParameters() {
        CracCreationParameters cracCreationParameters = NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension();
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setCounterTradingMinRange(-1234.);
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setCounterTradingMaxRange(4321.);
        NcCracCreationContext cracCreationContext = importCrac(cracCreationParameters);

        assertRange(cracCreationContext, "ct-no-limits", -1234, 4321);
        assertEquals("the minimum range was not provided. It has been set to the minimal range value of -1234.0, "
                + "the maximum range was not provided. It has been set to the maximal range value of 4321.0",
                cracCreationContext.getRemedialActionCreationContext("ct-no-limits").getImportStatusDetail());
        // Parameters are only used when no limit is provided
        assertRange(cracCreationContext, "ct-economic-limits", -1000, 1000);
    }

    @Test
    void doNotImportInvalidCounterTradeRangeActions() {
        NcCracCreationContext cracCreationContext = importCrac();

        // Availability
        assertRaNotImported(cracCreationContext, "ct-unavailable", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-unavailable will not be imported it is not set to be available.");
        assertRaNotImported(cracCreationContext, "ct-unavailable-in-ssi", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-unavailable-in-ssi will not be imported it is not set to be available.");

        // Kind
        assertRaNotImported(cracCreationContext, "ct-curative", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-curative will not be imported because unsupported kind is provided");

        // Operator
        assertRaNotImported(cracCreationContext, "ct-no-operator", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-no-operator will not be imported the counter trading remedial action has null operator code.");
        assertRaNotImported(cracCreationContext, "ct-operator-without-eic", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-operator-without-eic will not be imported because operator http://example.com/operator/RTE does not contain a valid EIC code.");
        assertRaNotImported(cracCreationContext, "ct-unsupported-operator", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-unsupported-operator will not be imported because system operator 10XDE-VE-------2 is not supported.");

        // Bidding zone
        assertRaNotImported(cracCreationContext, "ct-missing-bidding-zone", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-missing-bidding-zone will not be imported the counter trading remedial action has null bidding zone code.");
        assertRaNotImported(cracCreationContext, "ct-bidding-zone-without-eic", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-bidding-zone-without-eic will not be imported because the bidding zone is invalid.");
        assertRaNotImported(cracCreationContext, "ct-unknown-bidding-zone", ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action ct-unknown-bidding-zone will not be imported because the bidding zone code XXXXX-XXX------X is invalid.");

        // Range: min(4000, 500 + 1000) = 1500 < max(3000, 500 - 500) = 3000
        assertRaNotImported(cracCreationContext, "ct-empty-range", ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action ct-empty-range will not be imported because its range is empty: the min range 3000.0 is greater than the max range 1500.0.");
    }

    private static Network getNetworkWithFrNetPosition() {
        Network network = NcCracCreationTestUtil.getNetworkFromResource("/networks/16Nodes.zip");
        network.getLineStream().forEach(line -> {
            line.getTerminal1().setP(0.);
            line.getTerminal2().setP(0.);
        });
        Line frToDeLine = network.getLine(FR_TO_DE_LINE);
        frToDeLine.getTerminal1().setP(FR_NET_POSITION);
        frToDeLine.getTerminal2().setP(-FR_NET_POSITION);
        return network;
    }

    private static NcCracCreationContext importCrac(CracCreationParameters cracCreationParameters) {
        return NcCracCreationTestUtil.getNcCracCreationContext(CT_ARCHIVE, getNetworkWithFrNetPosition(), cracCreationParameters);
    }

    private static NcCracCreationContext importCrac() {
        return importCrac(NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension());
    }

    private static CounterTradeRangeAction getCounterTradeRangeAction(NcCracCreationContext cracCreationContext, String raId) {
        return (CounterTradeRangeAction) cracCreationContext.getCrac().getRangeAction(raId);
    }

    private static void assertRange(NcCracCreationContext cracCreationContext, String raId, double expectedMin, double expectedMax) {
        CounterTradeRangeAction counterTradeRangeAction = getCounterTradeRangeAction(cracCreationContext, raId);
        NcCracCreationTestUtil.assertCounterTradeRangeActionsImported(counterTradeRangeAction, raId, raId.toUpperCase(), expectedMax, expectedMin, "RTE");
    }

}
