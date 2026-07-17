/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.commons.BorderAreaEICode;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.crac.api.Crac;
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

    /**
     * Record representing the importing and exporting areas.
     *
     * @param importingArea the area importing energy
     * @param exportingArea the area exporting energy
     */
    public record CounterTradingAreas(String importingArea, String exportingArea) {
    }

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

        CounterTradingAreas counterTradingAreas = getImportingExportingAreas(countertradeRemedialAction, remedialActionId);

        return crac.newCounterTradeRangeAction()
                .withId(remedialActionId)
                .withOperator(NcCracUtils.getTsoNameFromUrl(countertradeRemedialAction.operator()))
                .newRange().withMin(minRange).withMax(maxRange).add()
                .withInitialSetpoint(0.)
                .withImportingArea(counterTradingAreas.importingArea())
                .withExportingArea(counterTradingAreas.exportingArea());
    }

    /**
     * Get the importing and exporting areas of a CountertradeRemedialAction.
     * Uses the border area code (region) and the operator (TSO) to
     * determine which area imports and which exports.
     * The operator's area is the exporting area, and the other area
     * of the border is the importing area.
     *
     * @param countertradeRemedialAction        Native CountertradeRemedialAction
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @return CounterTradingAreas record containing the importing and
     * exporting areas of the counter trading remedial action
     */
    private static CounterTradingAreas getImportingExportingAreas(CountertradeRemedialAction countertradeRemedialAction,
            String remedialActionId) {

        String operatorEic = NcCracUtils.getEicFromUrl(countertradeRemedialAction.operator());
        String regionCode = NcCracUtils.getEicFromUrl(countertradeRemedialAction.region());

        if (regionCode == null) {
            throw new OpenRaoImportException(
                    ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because the region code is null.",
                            remedialActionId));
        }

        BorderAreaEICode borderArea = BorderAreaEICode.fromEICode(regionCode)
                    .orElseThrow(() -> new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA,
                            String.format("Remedial action %s will not be imported because border %s is not supported.",
                                    remedialActionId, regionCode)));

        TsoEICode operator = TsoEICode.fromEICode(operatorEic)
                .orElseThrow(() -> new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                        String.format("Remedial action %s will not be imported because system operator %s is not supported.",
                                remedialActionId, operatorEic)));

        List<TsoEICode> areas = borderArea.getAreas();
        TsoEICode area1 = areas.get(0);
        TsoEICode area2 = areas.get(1);

        if (operator != area1 && operator != area2) {
            throw new IllegalArgumentException(
                    String.format("Operator %s (%s) does not belong to border %s (%s-%s)",
                            operator.getDisplayName(), operator.getEICode(),
                            borderArea.getDisplayName(), area1.getShortId(), area2.getShortId())
            );
        }

        return operator == area1 ? new CounterTradingAreas(area2.getShortId(), area1.getShortId())
                : new CounterTradingAreas(area1.getShortId(), area2.getShortId());
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
        String operatorUrl = countertradeRemedialAction.operator();
        if (operatorUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null operator code.", remedialActionId));
        }

        String operatorEic = NcCracUtils.getEicFromUrl(operatorUrl);
        if (operatorEic == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported because operator %s does not contain a valid EIC code.", remedialActionId, operatorUrl));
        }

        String regionUrl = countertradeRemedialAction.region();
        if (regionUrl == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null region code.", remedialActionId));
        }
    }

}
