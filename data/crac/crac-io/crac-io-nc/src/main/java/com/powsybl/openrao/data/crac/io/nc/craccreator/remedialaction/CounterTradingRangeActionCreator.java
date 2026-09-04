/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.objects.CountertradeRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;

import java.util.List;

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

    public CounterTradingRangeActionCreator(Crac crac,
                                            NcCracCreationParameters ncCracCreationParameters) {
        this.crac = crac;
        this.ncCracCreationParameters = ncCracCreationParameters;
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

        // checks for the min and max range
        double minRange;
        if (Double.isNaN(countertradeRemedialAction.minEconomicP())) {
            minRange = ncCracCreationParameters.getCounterTradingMinRange() != null
                    ? ncCracCreationParameters.getCounterTradingMinRange()
                    : NcConstants.COUNTER_TRADING_RANGE_MIN_RANGE;
            alterations.add("the minimum range was not set. It has been set to the minimal range value of " + minRange);
        } else {
            minRange = countertradeRemedialAction.minEconomicP();
        }
        double maxRange;
        if (Double.isNaN(countertradeRemedialAction.maxEconomicP())) {
            maxRange = ncCracCreationParameters.getCounterTradingMaxRange() != null
                    ? ncCracCreationParameters.getCounterTradingMaxRange()
                    : NcConstants.COUNTER_TRADING_RANGE_MAX_RANGE;
            alterations.add("the maximum range was not set. It has been set to the maximal range value of " + maxRange);
        } else {
            maxRange = countertradeRemedialAction.maxEconomicP();
        }

        String area = getArea(countertradeRemedialAction, remedialActionId);
        CounterTradeRangeActionAdder adder = crac.newCounterTradeRangeAction()
                .withId(remedialActionId)
                .withOperator(NcCracUtils.getTsoNameFromUrl(countertradeRemedialAction.creator()))
                .newRange().withMin(minRange).withMax(maxRange).add()
                .withInitialSetpoint(0.)
                .withArea(area)
                .withInitialNetPosition(0.); // TODO: Compute

        for (NcCracCreationParameters.ConnectedArea connectedArea : ncCracCreationParameters.getConnectedAreas()) {
            adder = addConnectedArea(adder, connectedArea);
        }

        return adder;
    }

    private static CounterTradeRangeActionAdder addConnectedArea(CounterTradeRangeActionAdder adder, NcCracCreationParameters.ConnectedArea connectedArea) {
        var connectedAreaAdder = adder.newConnectedArea().withArea(connectedArea.area());
        connectedArea.borderRanges().forEach(borderRange -> connectedAreaAdder.newBorderRange()
                .withMin(borderRange.borderRangeMin())
                .withMax(borderRange.borderRangeMax())
                .withRangeType("relative".equals(borderRange.rangeType()) ? RangeType.RELATIVE_TO_INITIAL_NETWORK : RangeType.ABSOLUTE)
                .add());

        return connectedAreaAdder.add();
    }

    /**
     * Get the area of a CountertradeRemedialAction, from its bidding zone.
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @return the area code of the counter trading remedial action
     */
    private static String getArea(CountertradeRemedialAction countertradeRemedialAction, String remedialActionId) {
        String biddingZoneEic = NcCracUtils.getEicFromUrl(countertradeRemedialAction.biddingZone());
        if (biddingZoneEic == null) {
            throw new OpenRaoImportException(
                    ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the bidding zone code is null.",
                            remedialActionId));
        }

        try {
            CountryEICode countryEICode = new CountryEICode(biddingZoneEic);
            return countryEICode.getCountry() != null ? countryEICode.getCountry().toString() : null;
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
                    String.format("Remedial action %s will not be imported it is not set to be available.", remedialActionId));
        }

        // Check for null conditions
        String operatorUrl = countertradeRemedialAction.creator();
        if (operatorUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null operator code.", remedialActionId));
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

        String biddingZoneUrl = countertradeRemedialAction.biddingZone();
        if (biddingZoneUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null bidding zone code.", remedialActionId));
        }
    }

}
