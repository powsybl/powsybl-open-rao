/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.isActivated;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneRemedialActionsCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CneRemedialActionsCreator.class);

    private CneRemedialActionsCreator() {

    }

    static void createRangeRemedialActionSeries(RangeAction rangeAction, String preventiveStateId, List<ConstraintSeries> constraintSeriesList, String preOptimVariantId, String postOptimVariantId) {
        RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (rangeActionResultExtension != null) {
            RangeActionResult preOptimRangeActionResult = rangeActionResultExtension.getVariant(preOptimVariantId);
            RangeActionResult postOptimRangeActionResult = rangeActionResultExtension.getVariant(postOptimVariantId);

            if (preOptimRangeActionResult != null && postOptimRangeActionResult != null
                && isActivated(preventiveStateId, preOptimRangeActionResult, postOptimRangeActionResult)
                && !rangeAction.getNetworkElements().isEmpty()) {

                ConstraintSeries preOptimConstraintSeriesB56 = createB56ConstraintSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), true);
                ConstraintSeries postOptimConstraintSeriesB56 = createB56ConstraintSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), false);

                rangeAction.getNetworkElements().forEach(networkElement -> createRemedialActionRegisteredResource(networkElement, preventiveStateId, preOptimRangeActionResult, preOptimConstraintSeriesB56.getRemedialActionSeries().get(0)));
                rangeAction.getNetworkElements().forEach(networkElement -> createRemedialActionRegisteredResource(networkElement, preventiveStateId, postOptimRangeActionResult, postOptimConstraintSeriesB56.getRemedialActionSeries().get(0)));

                // Add the remedial action series to B54 and B57
                addRemedialActionsToOtherConstraintSeries(postOptimConstraintSeriesB56.getRemedialActionSeries().get(0), constraintSeriesList);

                constraintSeriesList.add(preOptimConstraintSeriesB56);
                constraintSeriesList.add(postOptimConstraintSeriesB56);
            }
        }
    }

    private static ConstraintSeries createB56ConstraintSeries(String remedialActionId, String remedialActionName, String operator, boolean isPreOptim) {
        ConstraintSeries constraintSeries = newConstraintSeries(cutString(remedialActionId + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);
        RemedialActionSeries remedialActionSeries;
        if (isPreOptim) {
            remedialActionSeries = newRemedialActionSeries(remedialActionId, remedialActionName);
        } else {
            remedialActionSeries = newRemedialActionSeries(remedialActionId, remedialActionName, PREVENTIVE_MARKET_OBJECT_STATUS);
        }

        try {
            Country country = Country.valueOf(operator);
            remedialActionSeries.partyMarketParticipant.add(newPartyMarketParticipant(country));
        } catch (IllegalArgumentException e) {
            LOGGER.warn(String.format("Operator %s is not a country id.", operator));
        }

        constraintSeries.remedialActionSeries.add(remedialActionSeries);

        return constraintSeries;
    }

    private static void createRemedialActionRegisteredResource(NetworkElement networkElement, String preventiveStateId, RangeActionResult rangeActionResult, RemedialActionSeries remedialActionSeries) {
        if (rangeActionResult instanceof PstRangeResult) {
            int tap = ((PstRangeResult) rangeActionResult).getTap(preventiveStateId);
            RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
            remedialActionSeries.registeredResource.add(registeredResource);
            remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID(), tap));
        }
    }

    private static String createRangeActionId(String mRid, int tap) {
        return cutString(mRid.substring(0, mRid.length() - 6), 55) + "@" + tap + "@";
    }

    static void createNetworkRemedialActionSeries(NetworkAction networkAction, String preventiveStateId, List<ConstraintSeries> constraintSeriesList, String preOptimVariantId, String postOptimVariantId) {

        NetworkActionResultExtension networkActionResultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (networkActionResultExtension != null) {
            NetworkActionResult preOptimNetworkActionResult = networkActionResultExtension.getVariant(preOptimVariantId);
            NetworkActionResult postOptimNetworkActionResult = networkActionResultExtension.getVariant(postOptimVariantId);

            if (preOptimNetworkActionResult != null && postOptimNetworkActionResult != null
                && isActivated(preventiveStateId, preOptimNetworkActionResult, postOptimNetworkActionResult)
                && !networkAction.getNetworkElements().isEmpty()) {

                ConstraintSeries postOptimConstraintSeriesB56 = createB56ConstraintSeries(networkAction.getId(), networkAction.getName(), networkAction.getOperator(), false);

                // Add the remedial action series to B54 and B57
                addRemedialActionsToOtherConstraintSeries(postOptimConstraintSeriesB56.getRemedialActionSeries().get(0), constraintSeriesList);

                constraintSeriesList.add(postOptimConstraintSeriesB56);
            }
        }
    }

    private static void addRemedialActionsToOtherConstraintSeries(RemedialActionSeries remedialActionSeries, List<ConstraintSeries> constraintSeriesList) {
        RemedialActionSeries shortPostOptimRemedialActionSeries = newRemedialActionSeries(remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getApplicationModeMarketObjectStatusStatus());
        constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).forEach(constraintSeries -> constraintSeries.remedialActionSeries.add(shortPostOptimRemedialActionSeries));
    }
}
