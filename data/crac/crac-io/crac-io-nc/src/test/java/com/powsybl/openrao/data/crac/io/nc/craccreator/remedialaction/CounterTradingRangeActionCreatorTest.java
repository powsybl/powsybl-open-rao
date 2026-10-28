/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.assertRaNotImported;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals("FR", counterTradeRangeAction.getAreaId());
        assertEquals(0., counterTradeRangeAction.getInitialSetpoint());
        assertEquals(Optional.of(10), counterTradeRangeAction.getSpeed());
        assertEquals(Set.of("BE", "DE"), counterTradeRangeAction.getConnectedAreas().stream().map(ConnectedArea::getAreaId).collect(Collectors.toSet()));
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        NcCracCreationTestUtil.assertHasOnInstantUsageRule(cracCreationContext, "ct-economic-limits", NcCracCreationTestUtil.PREVENTIVE_INSTANT_ID);
    }

    @Test
    void importCounterTradeRangeActionsWithValidRanges() {
        NcCracCreationContext cracCreationContext = importCrac();

        assertEquals(17, cracCreationContext.getCrac().getRangeActions().stream().filter(CounterTradeRangeAction.class::isInstance).count());

        // Ranges are relative to the FR net position (500), and the min range cannot make it negative (min range >= -500)
        // Economic limit is the tightest for max
        assertRange(cracCreationContext, "ct-economic-limits", -500, 500);
        // Physical limit is the tightest for max
        assertRange(cracCreationContext, "ct-ssi-limits", -500, 1000);
        // Economic limit is the tightest for min, physical limit for max
        assertRange(cracCreationContext, "ct-mixed-limits", -500, 1000);
        // Only physical limits
        assertRange(cracCreationContext, "ct-ssi-only", -200, 300);
        // Only economic limits
        assertRange(cracCreationContext, "ct-economic-only", -500, 2500);
        // Physical limit only for max
        assertRange(cracCreationContext, "ct-ssi-up-only", -500, 1000);
        // Outdated SSI profile is ignored: the action stays available and uses economic limits only
        assertRange(cracCreationContext, "ct-outdated-ssi", -500, 1500);
        // Physical limits from the RemedialActionGroup linked through a RemedialActionDependency
        assertRange(cracCreationContext, "ct-group-limits", -500, 700);
        // Same RemedialActionGroup shared by another CountertradeRemedialAction, economic limit is the tightest for max
        assertRange(cracCreationContext, "ct-shared-group-limits", -500, 500);
        // RemedialActionDependency not enabled: the RemedialActionGroup physical limits are ignored
        assertRange(cracCreationContext, "ct-disabled-group-dependency", -500, 2500);
        // Same physical limits in the CountertradeRemedialAction and the RemedialActionGroup
        assertRange(cracCreationContext, "ct-ctra-and-group-limits", -500, 1000);

        // RemedialActionGroups only made of CountertradeRemedialActions are not imported as network actions
        assertNull(cracCreationContext.getRemedialActionCreationContext("ct-group"));
        assertNull(cracCreationContext.getRemedialActionCreationContext("ct-same-limits-group"));
        assertNull(cracCreationContext.getRemedialActionCreationContext("ct-different-limits-group"));

        ElementaryCreationContext notAlteredContext = cracCreationContext.getRemedialActionCreationContext("ct-mixed-limits");
        assertTrue(notAlteredContext.isImported());
        assertFalse(notAlteredContext.isAltered());
    }

    @Test
    void importCounterTradeRangeActionsWithNegativeNetPosition() {
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(CT_ARCHIVE,
                getNetworkWithFrNetPosition(-FR_NET_POSITION), NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension());

        // Ranges are relative to the FR net position (-500), and the max range cannot make it positive (max range <= 500)
        // Physical limit for max (1000) is capped, physical limit for min (-1500) is kept
        assertRange(cracCreationContext, "ct-ssi-limits", -1500, 500);
        // Economic limit for max (1500) is capped, economic limit for min (-500) is kept
        assertRange(cracCreationContext, "ct-economic-limits", -500, 500);
        // Physical limit for max (300) is below the cap
        assertRange(cracCreationContext, "ct-ssi-only", -200, 300);
        // Economic limit for min (500) is the tightest, physical limit for max (1000) is capped
        assertRange(cracCreationContext, "ct-mixed-limits", 500, 500);
    }

    @Test
    void doNotImportCounterTradeRangeActionWithoutRange() {
        NcCracCreationContext cracCreationContext = importCrac();

        assertRaNotImported(cracCreationContext, "ct-no-limits", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-no-limits will not be imported because its minimum range was not provided.");
    }

    @Test
    void doNotImportCounterTradeRangeActionWithoutMaxRange() {
        CracCreationParameters cracCreationParameters = NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension();
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setCounterTradingMinRange(-1234.);
        NcCracCreationContext cracCreationContext = importCrac(cracCreationParameters);

        assertRaNotImported(cracCreationContext, "ct-no-limits", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-no-limits will not be imported because its maximum range was not provided.");
    }

    @Test
    void importCounterTradeRangeActionWithDefaultRangeFromParameters() {
        CracCreationParameters cracCreationParameters = NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension();
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setCounterTradingMinRange(-1234.);
        cracCreationParameters.getExtension(NcCracCreationParameters.class).setCounterTradingMaxRange(4321.);
        NcCracCreationContext cracCreationContext = importCrac(cracCreationParameters);

        // The range from the parameters is used as is, even if it would make the FR net position (500) negative
        assertRange(cracCreationContext, "ct-no-limits", -1234, 4321);
        assertEquals("the minimum range was not provided. It has been set to the minimal range value of -1234.0, "
                + "the maximum range was not provided. It has been set to the maximal range value of 4321.0",
                cracCreationContext.getRemedialActionCreationContext("ct-no-limits").getImportStatusDetail());
        // Parameters are only used when no limit is provided
        assertRange(cracCreationContext, "ct-economic-limits", -500, 500);
    }

    @Test
    void warnWhenRemedialActionSystemOperatorDoesNotMatchCreator() {
        ListAppender<ILoggingEvent> listAppender = NcCracCreationTestUtil.getLogs(RaoBusinessWarns.class);
        NcCracCreationContext cracCreationContext = importCrac();

        // Imported with the creator as operator, whether the RemedialActionSystemOperator matches it or not
        assertRange(cracCreationContext, "ct-economic-limits", -500, 500);
        assertRange(cracCreationContext, "ct-different-system-operator", -500, 500);

        List<String> systemOperatorWarnings = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("RemedialActionSystemOperator"))
                .toList();
        assertEquals(List.of("Remedial action ct-different-system-operator: the RemedialActionSystemOperator http://energy.referencedata.eu/EIC/10XES-REE------E "
                + "does not match the creator http://energy.referencedata.eu/EIC/10XFR-RTE------Q of the RA profile, the creator will be used as operator."),
                systemOperatorWarnings);
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

        // Range: min(4000 - 500, 1000) = 1000 < max(3000 - 500, -500) = 2500
        assertRaNotImported(cracCreationContext, "ct-empty-range", ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action ct-empty-range will not be imported because its range is empty: the min range 2500.0 is greater than the max range 1000.0.");

        // Different physical limits in the CountertradeRemedialAction and the RemedialActionGroup
        assertRaNotImported(cracCreationContext, "ct-different-ctra-and-group-limits", ImportStatus.INCONSISTENCY_IN_DATA,
                "Remedial action ct-different-ctra-and-group-limits will not be imported because the maxRegulatingUp/maxRegulatingDown values of the CountertradeRemedialAction"
                    + " (1000.0/1500.0) and of the RemedialActionGroup (1000.0/10.0) are different.");
    }

    @Test
    void overrideAvailabilityWithSis() {
        NcCracCreationContext cracCreationContext = importCrac();

        // The availability of the first SIS time point at or after the import timestamp overrides normalAvailable,
        // whatever the order in which the time points are defined
        assertRaNotImported(cracCreationContext, "ct-unavailable-in-sis", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-unavailable-in-sis will not be imported it is not set to be available.");
        assertRange(cracCreationContext, "ct-available-in-sis", -500, 500);
        // The SSI availability overrides the SIS one
        assertRange(cracCreationContext, "ct-sis-overridden-by-ssi", -500, 500);
        // The applicable SIS time point has no availability: normalAvailable is kept
        assertRaNotImported(cracCreationContext, "ct-sis-without-availability", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-sis-without-availability will not be imported it is not set to be available.");
        // Several SIS schedules: only the "next" one with the smallest id is read
        assertRaNotImported(cracCreationContext, "ct-sis-several-schedules", ImportStatus.NOT_FOR_RAO,
                "Remedial action ct-sis-several-schedules will not be imported it is not set to be available.");
    }

    @Test
    void ignoreUnsupportedSisSchedules() {
        ListAppender<ILoggingEvent> listAppender = NcCracCreationTestUtil.getLogs(RaoBusinessWarns.class);
        NcCracCreationContext cracCreationContext = importCrac();

        // The SIS profile is ignored: normalAvailable is kept
        assertRange(cracCreationContext, "ct-sis-no-next-time-point", -500, 500);
        assertRange(cracCreationContext, "ct-sis-unsupported-interpolation", -500, 500);
        // Outdated SIS profile is ignored without warning
        assertRange(cracCreationContext, "ct-outdated-sis", -500, 500);

        // No warning for the schedule of a remedial action that is not a CountertradeRemedialAction
        Set<String> sisWarnings = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("SIS profile"))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "Remedial action ct-sis-no-next-time-point: the GenericAvailableSchedule ct-sis-no-next-time-point-schedule of the SIS profile will be ignored "
                    + "because it has no AvailabilityTimePoint at or after the import timestamp 2023-03-29T12:00Z.",
                "Remedial action ct-sis-unsupported-interpolation: the GenericAvailableSchedule ct-sis-unsupported-interpolation-schedule of the SIS profile will be ignored "
                    + "because its interpolation kind http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.none is not supported, only http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.next is.",
                "Remedial action ct-sis-several-schedules: the GenericAvailableSchedule ct-sis-several-schedules-schedule-2 of the SIS profile will be ignored "
                    + "because its interpolation kind http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.none is not supported, only http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.next is.",
                "Remedial action ct-sis-several-schedules: several GenericAvailableSchedules with the http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.next interpolation kind "
                    + "are defined in the SIS profile, only ct-sis-several-schedules-schedule-1 will be used."),
                sisWarnings);
    }

    @Test
    void doNotImportCounterTradeRangeActionWithoutInitialNetPosition() {
        Network network = NcCracCreationTestUtil.getNetworkFromResource("/networks/16Nodes.zip");
        List.copyOf(network.getLineStream().toList()).forEach(Line::remove);
        NcCracCreationContext cracCreationContext = NcCracCreationTestUtil.getNcCracCreationContext(CT_ARCHIVE, network, NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension());

        assertRaNotImported(cracCreationContext, "ct-economic-limits", ImportStatus.INCOMPLETE_DATA,
                "Remedial action ct-economic-limits will not be imported because the initial net position of area FR could not be computed from the network.");
    }

    private static Network getNetworkWithFrNetPosition(double frNetPosition) {
        Network network = NcCracCreationTestUtil.getNetworkFromResource("/networks/16Nodes.zip");
        network.getLineStream().forEach(line -> {
            line.getTerminal1().setP(0.);
            line.getTerminal2().setP(0.);
        });
        Line frToDeLine = network.getLine(FR_TO_DE_LINE);
        frToDeLine.getTerminal1().setP(frNetPosition);
        frToDeLine.getTerminal2().setP(-frNetPosition);
        return network;
    }

    private static NcCracCreationContext importCrac(CracCreationParameters cracCreationParameters) {
        return NcCracCreationTestUtil.getNcCracCreationContext(CT_ARCHIVE, getNetworkWithFrNetPosition(FR_NET_POSITION), cracCreationParameters);
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
