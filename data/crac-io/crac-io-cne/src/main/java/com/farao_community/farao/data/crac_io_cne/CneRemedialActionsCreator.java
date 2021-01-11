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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
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

    static void createRangeRemedialActionSeries(RangeAction rangeAction, CneHelper cneHelper, List<ConstraintSeries> constraintSeriesList, ConstraintSeries preventiveB56) {

        String preventiveStateId = cneHelper.getCrac().getPreventiveState().getId();
        String preOptimVariantId = cneHelper.getPreOptimVariantId();
        String postOptimVariantId = cneHelper.getPostOptimVariantId();
        Network network = cneHelper.getNetwork();
        RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (rangeActionResultExtension != null) {
            RangeActionResult preOptimRangeActionResult = rangeActionResultExtension.getVariant(preOptimVariantId);
            RangeActionResult postOptimRangeActionResult = rangeActionResultExtension.getVariant(postOptimVariantId);

            ConstraintSeries preOptimConstraintSeriesB56 = createB56ConstraintSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator());
            rangeAction.getNetworkElements().forEach(networkElement -> {
                TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(networkElement.getId());
                createRemedialActionRegisteredResourceFromNetwork(transformer, networkElement, preOptimConstraintSeriesB56.getRemedialActionSeries().get(0));
            });

            if (preOptimRangeActionResult != null && postOptimRangeActionResult != null
                && isActivated(preventiveStateId, preOptimRangeActionResult, postOptimRangeActionResult)
                && !rangeAction.getNetworkElements().isEmpty()) {

                RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(rangeAction.getId(), rangeAction.getName(), rangeAction.getOperator(), false);
                rangeAction.getNetworkElements().forEach(networkElement -> createRemedialActionRegisteredResource(networkElement, preventiveStateId, postOptimRangeActionResult, remedialActionSeries));
                preventiveB56.remedialActionSeries.add(remedialActionSeries);
            }
            constraintSeriesList.add(preOptimConstraintSeriesB56);
        }
    }

    private static ConstraintSeries createB56ConstraintSeries(String remedialActionId, String remedialActionName, String operator) {
        ConstraintSeries constraintSeries = newConstraintSeries(cutString(remedialActionId + "_" + generateRandomMRID(), 60), B56_BUSINESS_TYPE);
        fillB56ConstraintSeries(remedialActionId, remedialActionName, operator, true, constraintSeries);
        return constraintSeries;
    }

    private static RemedialActionSeries createB56RemedialActionSeries(String remedialActionId, String remedialActionName, String operator, boolean isPreOptim) {
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

        return remedialActionSeries;
    }

    private static void fillB56ConstraintSeries(String remedialActionId, String remedialActionName, String operator, boolean isPreOptim, ConstraintSeries constraintSeries) {
        RemedialActionSeries remedialActionSeries = createB56RemedialActionSeries(remedialActionId, remedialActionName, operator, isPreOptim);
        constraintSeries.remedialActionSeries.add(remedialActionSeries);
    }

    private static void createRemedialActionRegisteredResourceFromNetwork(TwoWindingsTransformer transformer, NetworkElement networkElement, RemedialActionSeries remedialActionSeries) {
        if (transformer != null) {
            int tap = transformer.getPhaseTapChanger().getTapPosition();
            RemedialActionRegisteredResource registeredResource = newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
            remedialActionSeries.registeredResource.add(registeredResource);
            remedialActionSeries.setMRID(createRangeActionId(remedialActionSeries.getMRID(), tap));
        }
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
        return cutString(mRid, 55) + "@" + tap + "@";
    }

    static void createNetworkRemedialActionSeries(NetworkAction networkAction, CneHelper cneHelper, ConstraintSeries preventiveB56) {

        String preventiveStateId = cneHelper.getCrac().getPreventiveState().getId();
        String preOptimVariantId = cneHelper.getPreOptimVariantId();
        String postOptimVariantId = cneHelper.getPostOptimVariantId();

        NetworkActionResultExtension networkActionResultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (networkActionResultExtension != null) {
            NetworkActionResult preOptimNetworkActionResult = networkActionResultExtension.getVariant(preOptimVariantId);
            NetworkActionResult postOptimNetworkActionResult = networkActionResultExtension.getVariant(postOptimVariantId);

            if (preOptimNetworkActionResult != null && postOptimNetworkActionResult != null
                && isActivated(preventiveStateId, preOptimNetworkActionResult, postOptimNetworkActionResult)
                && !networkAction.getNetworkElements().isEmpty()) {

                fillB56ConstraintSeries(networkAction.getId(), networkAction.getName(), networkAction.getOperator(), false, preventiveB56);
            }
        }
    }

    static void addRemedialActionsToOtherConstraintSeries(List<RemedialActionSeries> remedialActionSeriesList, List<ConstraintSeries> constraintSeriesList) {
        remedialActionSeriesList.forEach(remedialActionSeries -> {
            RemedialActionSeries shortPostOptimRemedialActionSeries = newRemedialActionSeries(remedialActionSeries.getMRID(), remedialActionSeries.getName(), remedialActionSeries.getApplicationModeMarketObjectStatusStatus());
            constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B54_BUSINESS_TYPE) || constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE))
                    .forEach(constraintSeries -> constraintSeries.remedialActionSeries.add(shortPostOptimRemedialActionSeries));
        });
    }
}
