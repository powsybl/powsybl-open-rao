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
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.openrao.data.crac.io.nc.objects.CountertradeRemedialAction;

import java.util.List;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class CounterTradingActionCreator {
    private final Crac crac;

    public record CounterTradingCountries(Country importingCountry, Country exportingCountry) { }

    /**
     * Constructor of the PstRangeActionCreator class which is used to create PST range actions
     * inside the CRAC object
     *
     * @param crac Open RAO Crac object
     */
    public CounterTradingActionCreator(Crac crac) {
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

        validateCounterTradingAction(counterTradingRemedialAction, remedialActionId);

        // checks for the min and max range
        Double minRange;
        if (counterTradingRemedialAction.minEconomicPMargen() == null) {
            minRange = NcConstants.CounterTradingRange.MIN_RANGE.toDouble();
            alterations.add("the minimum range was not set. It has been set to the minimal range value of " + minRange);
        } else {
            minRange = counterTradingRemedialAction.minEconomicPMargen();
        }
        Double maxRange;
        if (counterTradingRemedialAction.maxEconomicPMargin() == null) {
            maxRange = NcConstants.CounterTradingRange.MAX_RANGE.toDouble();
            alterations.add("the maximum range was not set. It has been set to the maximal range value of " + maxRange);
        } else {
            maxRange = counterTradingRemedialAction.maxEconomicPMargin();
        }

        // what to do with the initial set points?
        // set to 0 for now
        // what to do if repeated countries. maybe return the counterTradingCountries in a helper function and use it to filter and get te max of the repeated only.

        CounterTradingCountries counterTradingCountries = getImportingExportingCountries(counterTradingRemedialAction, remedialActionId);

        return crac.newCounterTradeRangeAction()
                .withId(remedialActionId)
                .withOperator(counterTradingRemedialAction.operator())
                .newRange().withMin(minRange).withMax(maxRange).add()
                .withInitialSetpoint(0.)
                .withImportingCountry(counterTradingCountries.importingCountry())
                .withExportingCountry(counterTradingCountries.exportingCountry());
    }

    private static CounterTradingCountries getImportingExportingCountries(CountertradeRemedialAction countertradeRemedialAction,
                                                                          String remedialActionId) {
        String operatorCode = countertradeRemedialAction.operator();
        if (operatorCode.equals(TsoEICode.FR.getEICode())) { // if it is FR, we know imp/exp countries.
            return new CounterTradingCountries(Country.ES, Country.FR);
        } else if (operatorCode.equals(TsoEICode.PT.getEICode())) { // if it is PT, we know imp/exp countries.
            return new CounterTradingCountries(Country.ES, Country.PT);
        } else if (operatorCode.equals(TsoEICode.ES.getEICode())) { // If ES, determine with the regionCode
            // TODO: Check borders
        }
        throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                String.format("Remedial action %s will not be imported the system operator code does not correspond to any of the supported countries.", remedialActionId));
    }

    private static void validateCounterTradingAction(CountertradeRemedialAction counterTradingRemedialAction,
                                                     String remedialActionId) {

        if (!Boolean.TRUE.equals(counterTradingRemedialAction.normalAvailable())) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO,
                    String.format("Remedial action %s will not be imported it is not set to be available.", remedialActionId));
        }

        // Check for null conditions

        String operatorCode = counterTradingRemedialAction.operator();
        if (operatorCode == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null operator code.", remedialActionId));
        }

        String regionCode = counterTradingRemedialAction.region();
        if (regionCode == null) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Remedial action %s will not be imported the counter trading remedial action has null region code.", remedialActionId));
        }
    }
}
