/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.networkarea.BorderBasedCountryArea;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmCounterTradeHelper;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.objects.CountertradeRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class CounterTradingRangeActionCreator {
    /**
     * The CRAC object being built.
     */
    private final Crac crac;
    /**
     * NC CRAC creation parameters.
     */
    private final NcCracCreationParameters ncCracCreationParameters;
    /**
     * Network used to compute connected areas and net positions (assumed to already carry a load flow result).
     */
    private final Network network;
    /**
     * Country areas used to compute border exchanges, created once per country.
     */
    private final Map<Country, BorderBasedCountryArea> countryAreas = new EnumMap<>(Country.class);

    public CounterTradingRangeActionCreator(Crac crac,
                                            Network network,
                                            NcCracCreationParameters ncCracCreationParameters) {
        this.crac = crac;
        this.ncCracCreationParameters = ncCracCreationParameters;
        this.network = network;
    }

    /**
     * Get the Counter Trading Range Action Adder object to create a CounterTradingRangeActions
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction object
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @param alterations                       alteration messages
     * @return CounterTradeRangeActionAdder     object with the counter trading range action added (if valid)
     */
    public CounterTradeRangeActionAdder getCounterTradeRangeActionAdder(CountertradeRemedialAction countertradeRemedialAction,
                                                                        String remedialActionId, List<String> alterations) {

        validateCountertradeRemedialAction(countertradeRemedialAction, remedialActionId);

        String area = getAreaId(countertradeRemedialAction, remedialActionId);

        IidmCounterTradeHelper iidmCounterTradeHelper = new IidmCounterTradeHelper(area, network);
        List<String> neighbors = iidmCounterTradeHelper.getNeighbors();

        double initialNetPosition = computeInitialNetPosition(area, neighbors);
        if (Double.isNaN(initialNetPosition)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the initial net position of area %s could not be computed from the network.", remedialActionId, area));
        }

        // Both SSI values come from the same origin: the CountertradeRemedialAction if it provides any, the RemedialActionGroup otherwise
        boolean useCountertradeValues = hasAnyValue(countertradeRemedialAction.maxRegulatingUp(), countertradeRemedialAction.maxRegulatingDown());
        double maxRegulatingUp = useCountertradeValues ? countertradeRemedialAction.maxRegulatingUp() : countertradeRemedialAction.groupMaxRegulatingUp();
        double maxRegulatingDown = useCountertradeValues ? countertradeRemedialAction.maxRegulatingDown() : countertradeRemedialAction.groupMaxRegulatingDown();

        // Checks for the min and max range
        double minRange = getMinRange(countertradeRemedialAction, remedialActionId, initialNetPosition, maxRegulatingDown, alterations);
        double maxRange = getMaxRange(countertradeRemedialAction, remedialActionId, initialNetPosition, maxRegulatingUp, alterations);
        if (minRange > maxRange) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA,
                    String.format("Remedial action %s will not be imported because its range is empty: the min range %s is greater than the max range %s.",
                            remedialActionId, minRange, maxRange));
        }

        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId(remedialActionId)
                .withName(countertradeRemedialAction.name())
                .withOperator(NcCracUtils.safeGetOperator(countertradeRemedialAction.operator()))
                .newRange().withMin(minRange).withMax(maxRange).add()
                .withInitialSetpoint(0.)
                .withAreaId(area);

        neighbors.forEach(neighbor -> counterTradeRangeActionAdder.newConnectedArea().withAreaId(neighbor).add());
        return counterTradeRangeActionAdder;
    }

    /**
     * Compute the initial net position of an area as the sum of its border exchanges with each of its neighbors.
     * Neighbors are the areas sharing a border with it in the network.
     *
     * @param area       Country code of the area
     * @param neighbors  Country codes of the areas sharing a border with the area
     * @return the net position, or NaN if it cannot be computed (no neighbor in the network, or no load flow result on a border)
     */
    private double computeInitialNetPosition(String area, List<String> neighbors) {
        if (neighbors.isEmpty()) {
            return Double.NaN;
        }
        Country country = Country.valueOf(area);
        Set<Country> neighborCountries = neighbors.stream().map(Country::valueOf).collect(Collectors.toSet());
        if (isBorderFlowMissing(country, neighborCountries)) {
            return Double.NaN;
        }
        return neighbors.stream()
                .mapToDouble(neighbor -> getBorderExchange(country, Country.valueOf(neighbor)))
                .sum();
    }

    /**
     * Check whether the flow is missing (NaN) on a connected terminal of a line, tie-line or HVDC line between the country
     * and one of its neighbors. BorderBasedCountryArea silently counts such flows as 0, so they must be detected beforehand.
     */
    private boolean isBorderFlowMissing(Country country, Set<Country> neighbors) {
        return Stream.of(
                network.getLineStream().map(line -> List.of(line.getTerminal1(), line.getTerminal2())),
                network.getTieLineStream().map(tieLine -> List.of(tieLine.getTerminal1(), tieLine.getTerminal2())),
                network.getHvdcLineStream().map(hvdcLine -> List.of(hvdcLine.getConverterStation1().getTerminal(), hvdcLine.getConverterStation2().getTerminal())))
            .flatMap(Function.identity())
            .filter(terminals -> isBorder(terminals.get(0), terminals.get(1), country, neighbors))
            .flatMap(List::stream)
            .anyMatch(terminal -> terminal.isConnected() && Double.isNaN(terminal.getP()));
    }

    private static boolean isBorder(Terminal terminal1, Terminal terminal2, Country country, Set<Country> neighbors) {
        Country country1 = getCountry(terminal1);
        Country country2 = getCountry(terminal2);
        return country1 == country && neighbors.contains(country2) || country2 == country && neighbors.contains(country1);
    }

    private static Country getCountry(Terminal terminal) {
        return terminal.getVoltageLevel().getSubstation().flatMap(Substation::getCountry).orElse(null);
    }

    /**
     * Get the flow leaving the country 'from' towards the country 'to'.
     */
    private double getBorderExchange(Country from, Country to) {
        return getCountryArea(from).getLeavingFlowToCountry(getCountryArea(to));
    }

    private BorderBasedCountryArea getCountryArea(Country country) {
        return countryAreas.computeIfAbsent(country, c -> new BorderBasedCountryArea(network, List.of(c)));
    }

    /**
     * Get the maximum range of a CountertradeRemedialAction, relative to the initial net position of its bidding zone.
     * The physical limit is maxRegulatingUp and the economic limit is maxEconomicP - initialNetPosition.
     * If both limits are provided -> use the tightest one (intersection).
     * If only one of them is provided -> use it as the max range.
     * If none of them is provided -> use the value from ncCracCreationParameters, or do not import the remedial action if it is not set.
     * If the initial net position is negative, initialNetPosition + max range cannot be positive (not applied to the ncCracCreationParameters value).
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @param initialNetPosition                Initial net position of the bidding zone
     * @param maxRegulatingUp                   SSI maxRegulatingUp value
     * @param alterations                       List to store alteration messages
     * @return the maximum range value
     */
    private double getMaxRange(CountertradeRemedialAction countertradeRemedialAction, String remedialActionId, double initialNetPosition, double maxRegulatingUp, List<String> alterations) {

        double physicalMaxRange = maxRegulatingUp;
        double economicMaxRange = countertradeRemedialAction.maxEconomicP() - initialNetPosition;

        double maxRange;
        if (!Double.isNaN(physicalMaxRange) && !Double.isNaN(economicMaxRange)) {
            maxRange = Math.min(physicalMaxRange, economicMaxRange);
        } else if (!Double.isNaN(physicalMaxRange)) {
            maxRange = physicalMaxRange;
        } else if (!Double.isNaN(economicMaxRange)) {
            maxRange = economicMaxRange;
        } else {
            // Fallback to the value from the parameters
            Double parametersMaxRange = ncCracCreationParameters.getCounterTradingMaxRange();
            if (parametersMaxRange == null) {
                throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                        String.format("Remedial action %s will not be imported because its maximum range was not provided.", remedialActionId));
            }
            alterations.add("the maximum range was not provided. It has been set to the maximal range value of " + parametersMaxRange);
            return parametersMaxRange;
        }

        // A negative net position cannot become positive
        if (initialNetPosition < 0 && maxRange > -initialNetPosition) {
            alterations.add(String.format("the maximum range %s would make the negative initial net position %s positive. It has been set to %s",
                    maxRange, initialNetPosition, -initialNetPosition));
            maxRange = -initialNetPosition;
        }
        return maxRange;
    }

    /**
     * Get the minimum range of a CountertradeRemedialAction, relative to the initial net position of its bidding zone.
     * The physical limit is -maxRegulatingDown and the economic limit is minEconomicP - initialNetPosition.
     * If both limits are provided -> use the tightest one (intersection).
     * If only one of them is provided -> use it as the min range.
     * If none of them is provided -> use the value from ncCracCreationParameters, or do not import the remedial action if it is not set.
     * If the initial net position is positive, initialNetPosition + min range cannot be negative (not applied to the ncCracCreationParameters value).
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @param initialNetPosition                Initial net position of the bidding zone
     * @param maxRegulatingDown                 SSI maxRegulatingDown value
     * @param alterations                       List to store alteration messages
     * @return the minimum range value
     */
    private double getMinRange(CountertradeRemedialAction countertradeRemedialAction, String remedialActionId, double initialNetPosition, double maxRegulatingDown, List<String> alterations) {

        double physicalMinRange = -maxRegulatingDown;
        double economicMinRange = countertradeRemedialAction.minEconomicP() - initialNetPosition;

        double minRange;
        if (!Double.isNaN(physicalMinRange) && !Double.isNaN(economicMinRange)) {
            minRange = Math.max(physicalMinRange, economicMinRange);
        } else if (!Double.isNaN(physicalMinRange)) {
            minRange = physicalMinRange;
        } else if (!Double.isNaN(economicMinRange)) {
            minRange = economicMinRange;
        } else {
            // Fallback to the value from the parameters
            Double parametersMinRange = ncCracCreationParameters.getCounterTradingMinRange();
            if (parametersMinRange == null) {
                throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                        String.format("Remedial action %s will not be imported because its minimum range was not provided.", remedialActionId));
            }
            alterations.add("the minimum range was not provided. It has been set to the minimal range value of " + parametersMinRange);
            return parametersMinRange;
        }

        // A positive net position cannot become negative
        if (initialNetPosition > 0 && minRange < -initialNetPosition) {
            alterations.add(String.format("the minimum range %s would make the positive initial net position %s negative. It has been set to %s",
                    minRange, initialNetPosition, -initialNetPosition));
            minRange = -initialNetPosition;
        }
        return minRange;
    }

    /**
     * Get the area of a CountertradeRemedialAction, from its bidding zone.
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @return the area code of the counter trading remedial action
     */
    private String getAreaId(CountertradeRemedialAction countertradeRemedialAction, String remedialActionId) {
        String biddingZoneEic = NcCracUtils.getEicFromUrl(countertradeRemedialAction.biddingZone());
        if (biddingZoneEic == null) {
            throw new OpenRaoImportException(
                    ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the bidding zone is invalid.",
                            remedialActionId));
        }

        try {
            return new CountryEICode(biddingZoneEic).getCountry().toString();
        } catch (IllegalArgumentException e) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA,
                    String.format("Remedial action %s will not be imported because the bidding zone code %s is invalid.", remedialActionId, biddingZoneEic));
        }
    }

    /**
     * Validate a CountertradeRemedialAction before creating the CounterTradeRangeActionAdder
     *
     * @param countertradeRemedialAction    Native CountertradeRemedialAction object to validate
     * @param remedialActionId              ID of the RemedialAction (RA id)
     */
    private void validateCountertradeRemedialAction(CountertradeRemedialAction countertradeRemedialAction,
                                                    String remedialActionId) {

        if (!countertradeRemedialAction.normalAvailable()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                    String.format("Remedial action %s will not be imported because it is not set to be available.", remedialActionId));
        }

        // Check for null conditions
        String operatorUrl = countertradeRemedialAction.operator();
        if (operatorUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the counter trading remedial action has a null operator code.", remedialActionId));
        }

        String operatorEic = NcCracUtils.getEicFromUrl(operatorUrl);
        if (operatorEic == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because operator %s does not contain a valid EIC code.", remedialActionId, operatorUrl));
        }

        TsoEICode.fromEICode(operatorEic)
                .orElseThrow(() -> new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                        String.format("Remedial action %s will not be imported because system operator %s is not supported.",
                                remedialActionId, operatorEic)));

        // The optional RemedialActionSystemOperator should match the creator of the RA profile, which is the one used as operator
        String systemOperatorUrl = countertradeRemedialAction.remedialActionSystemOperator();
        if (systemOperatorUrl != null && !operatorEic.equals(NcCracUtils.getEicFromUrl(systemOperatorUrl))) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format(
                    "Remedial action %s: the RemedialActionSystemOperator %s does not match the creator %s of the RA profile, the creator will be used as operator.",
                    remedialActionId, systemOperatorUrl, operatorUrl));
        }

        String biddingZoneUrl = countertradeRemedialAction.biddingZone();
        if (biddingZoneUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the counter trading remedial action has a null bidding zone code.", remedialActionId));
        }

        // SSI values may be provided by the CountertradeRemedialAction or by the RemedialActionGroup, but not with different values
        double maxRegulatingUp = countertradeRemedialAction.maxRegulatingUp();
        double maxRegulatingDown = countertradeRemedialAction.maxRegulatingDown();
        double groupMaxRegulatingUp = countertradeRemedialAction.groupMaxRegulatingUp();
        double groupMaxRegulatingDown = countertradeRemedialAction.groupMaxRegulatingDown();
        if (hasAnyValue(maxRegulatingUp, maxRegulatingDown) && hasAnyValue(groupMaxRegulatingUp, groupMaxRegulatingDown)
                && (Double.compare(maxRegulatingUp, groupMaxRegulatingUp) != 0 || Double.compare(maxRegulatingDown, groupMaxRegulatingDown) != 0)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA,
                    String.format("Remedial action %s will not be imported because its maxRegulatingUp/maxRegulatingDown values are inconsistent: "
                            + "they are defined both in the CountertradeRemedialAction (%s/%s) and in the RemedialActionGroup (%s/%s), with different values.",
                            remedialActionId, maxRegulatingUp, maxRegulatingDown, groupMaxRegulatingUp, groupMaxRegulatingDown));
        }
    }

    private static boolean hasAnyValue(double maxRegulatingUp, double maxRegulatingDown) {
        return !Double.isNaN(maxRegulatingUp) || !Double.isNaN(maxRegulatingDown);
    }

}
