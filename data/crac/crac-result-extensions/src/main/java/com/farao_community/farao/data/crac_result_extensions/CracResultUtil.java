/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class CracResultUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracResultUtil.class);

    private CracResultUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Apply remedial actions saved in CRAC result extension on current working variant of given network,
     * with automatically selected cracVariantId, at a given state.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     * @param state State for which the RAs should be applied
     */
    public static void applyRemedialActionsForState(Network network, Crac crac, State state) {
        CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager != null && cracExtension != null) { // Results from RAO
            LOGGER.debug("Remedial Actions selected from RAO results for state {}.", state.getId());
            String cracVariantId = findPostOptimVariant(resultVariantManager, cracExtension);
            applyRemedialActionsForState(network, crac, cracVariantId, state);
        } else { // Apply all RAs from CRAC
            LOGGER.debug("No RAO results found. All Remedial Actions from CRAC are applied for state {}.", state.getId());
            applyAllNetworkRemedialActionsForState(network, crac, state);
        }
    }

    // Find post optim variant if any
    // this comes from CNEHelper (until String cracVariantId) ...
    private static String findPostOptimVariant(ResultVariantManager resultVariantManager, CracResultExtension cracExtension) {
        List<String> variants = new ArrayList<>(resultVariantManager.getVariants());
        String postOptimVariantId = variants.get(0);

        double minCost = cracExtension.getVariant(variants.get(0)).getCost();
        if (variants.size() < 2) {
            LOGGER.warn("No variant after post optimisation, because the crac contains only {} variant.", variants.size());
        }
        for (String variant : variants) {
            if (cracExtension.getVariant(variant).getCost() <= minCost) {
                minCost = cracExtension.getVariant(variant).getCost();
                postOptimVariantId = variant;
            }
        }
        return postOptimVariantId;
    }

    /**
     * Apply remedial actions saved in CRAC result extension on current working variant of given network, at a given state.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     * @param cracVariantId CRAC variant to get active remedial actions from
     * @param state State for which the RAs should be applied
     */
    public static void applyRemedialActionsForState(Network network, Crac crac, String cracVariantId, State state) {
        String stateId = state.getId();
        crac.getNetworkActions().forEach(na -> applyNetworkAction(na, network, cracVariantId, stateId));
        crac.getRangeActions().forEach(ra -> applyRangeAction(ra, network, cracVariantId, stateId));
    }

    private static void applyNetworkAction(NetworkAction networkAction, Network network, String cracVariantId, String stateId) {
        NetworkActionResultExtension resultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on network action {}", networkAction.getId());
        } else {
            NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
            if (networkActionResult != null && networkActionResult.activationMap.containsKey(stateId)) {
                if (networkActionResult.isActivated(stateId)) {
                    LOGGER.debug("Applying network action {}", networkAction.getName());
                    networkAction.apply(network);
                }
            } else {
                LOGGER.error("Could not find results for variant {} on network action {}", cracVariantId, networkAction.getId());
            }
        }
    }

    private static void applyRangeAction(RangeAction rangeAction, Network network, String cracVariantId, String stateId) {
        RangeActionResultExtension resultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on range action {}", rangeAction.getId());
        } else {
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                if (!Double.isNaN(rangeActionResult.getSetPoint(stateId))) {
                    LOGGER.debug("Applying range action {}: tap {}", rangeAction.getName(), ((PstRangeResult) rangeActionResult).getTap(stateId));
                }
                rangeAction.apply(network, rangeActionResult.getSetPoint(stateId));
            } else {
                LOGGER.error("Could not find results for variant {} on range action {}", cracVariantId, rangeAction.getId());
            }
        }
    }

    /**
     * Apply all remedial actions saved in CRAC, on a given network, at a given state.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     * @param state State for which the RAs should be applied
     */
    public static void applyAllNetworkRemedialActionsForState(Network network, Crac crac, State state) {
        crac.getNetworkActions().forEach(na -> {
            UsageMethod usageMethod = na.getUsageMethod(state);
            if (usageMethod.equals(UsageMethod.AVAILABLE) || usageMethod.equals(UsageMethod.FORCED)) {
                na.apply(network);
            }
        });
    }
}
