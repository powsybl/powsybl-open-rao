/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.TsoEICode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.objects.CountertradeRemedialAction;

import java.util.List;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class CounterTradingRangeActionCreator {
    private final Crac crac;

    public record CounterTradingCountries(Country importingCountry, Country exportingCountry) { }

    /**
     * Constructor of the PstRangeActionCreator class which is used to create PST range actions
     * inside the CRAC object
     *
     * @param crac Open RAO Crac object
     */
    public CounterTradingRangeActionCreator(Crac crac) {
        this.crac = crac;
    }

    /**
     * Method to get the PST Range Action Adder object:
     * - It is used to get the PstRangeActionAdder which adds tap position elementary RAs in the
     * CRAC object
     *
     * @param counterTradingRemedialAction PowerRemedialAction object consisting ofs
     *                                          the counter trading action
     * @param remedialActionId                  ID of the RemedialAction (RA id)
     * @param alterations                       alteration messages
     * @return CounterTradeRangeActionAdder object with the pst range action added (if valid)
     */
    public CounterTradeRangeActionAdder getCounterTradeRangeActionAdder(CountertradeRemedialAction counterTradingRemedialAction,
                                                                        String remedialActionId, List<String> alterations) {

        validateCountertradeRemedialAction(counterTradingRemedialAction, remedialActionId);

        // checks for the min and max range
        Double minRange;
        if (Double.isNaN(counterTradingRemedialAction.maxRegulatingDown())) {
            minRange = NcConstants.COUNTER_TRADING_RANGE_MIN_RANGE;
            alterations.add("the minimum range was not set. It has been set to the minimal range value of " + minRange);
        } else {
            minRange = counterTradingRemedialAction.maxRegulatingDown();
        }
        Double maxRange;
        if (Double.isNaN(counterTradingRemedialAction.maxRegulatingUp())) {
            maxRange = NcConstants.COUNTER_TRADING_RANGE_MAX_RANGE;
            alterations.add("the maximum range was not set. It has been set to the maximal range value of " + maxRange);
        } else {
            maxRange = counterTradingRemedialAction.maxRegulatingUp();
        }

        CounterTradingCountries counterTradingCountries = getImportingExportingCountries(counterTradingRemedialAction, remedialActionId);

        return crac.newCounterTradeRangeAction()
                .withId(remedialActionId)
                .withOperator(NcCracUtils.getTsoNameFromUrl(counterTradingRemedialAction.operator()))
                .newRange().withMin(minRange).withMax(maxRange).add()
                .withInitialSetpoint(0.)
                .withImportingCountry(counterTradingCountries.importingCountry())
                .withExportingCountry(counterTradingCountries.exportingCountry());
    }

    private static CounterTradingCountries getImportingExportingCountries(CountertradeRemedialAction countertradeRemedialAction,
                                                                          String remedialActionId) {
        String operatorEic = NcCracUtils.getEicFromUrl(countertradeRemedialAction.operator());
        if (TsoEICode.FR.getEICode().equals(operatorEic)) { // if it is FR, we know imp/exp countries.
            return new CounterTradingCountries(Country.ES, Country.FR);
        } else if (TsoEICode.PT.getEICode().equals(operatorEic)) { // if it is PT, we know imp/exp countries.
            return new CounterTradingCountries(Country.ES, Country.PT);
        } else if (TsoEICode.ES.getEICode().equals(operatorEic)) { // If ES, determine with the regionCode
            String regionCode = NcCracUtils.getEicFromUrl(countertradeRemedialAction.region());
            if ("10YDOM--ES-FR--D".equals(regionCode)) {
                return new CounterTradingCountries(Country.FR, Country.ES);
            }
            if ("10YDOM--ES-PT--T".equals(regionCode)) {
                return new CounterTradingCountries(Country.PT, Country.ES);
            }
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                String.format("Remedial action %s will not be imported because border %s is not supported for ES counter-trading.", remedialActionId, regionCode));
        }
        throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                String.format("Remedial action %s will not be imported because system operator %s is not supported.", remedialActionId, operatorEic));
    }

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
