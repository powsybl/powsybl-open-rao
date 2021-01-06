/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
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
     * Apply preventive remedial actions saved in CRAC result extension on current working variant of given network.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     * @param cracVariantId CRAC variant to get active remedial actions from
     */
    public static void applyPreventiveRemedialActions(Network network, Crac crac, String cracVariantId) {
        String preventiveStateId = crac.getPreventiveState().getId();
        crac.getNetworkActions().forEach(na -> applyNetworkAction(na, network, cracVariantId, preventiveStateId));
        crac.getRangeActions().forEach(ra -> applyRangeAction(ra, network, cracVariantId, preventiveStateId));
    }

    private static void applyNetworkAction(NetworkAction networkAction, Network network, String cracVariantId, String preventiveStateId) {
        NetworkActionResultExtension resultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on network action {}", networkAction.getId());
        } else {
            NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
            if (networkActionResult != null) {
                if (networkActionResult.isActivated(preventiveStateId)) {
                    LOGGER.debug("Applying network action {}", networkAction.getName());
                    networkAction.apply(network);
                }
            } else {
                LOGGER.error("Could not find results for variant {} on network action {}", cracVariantId, networkAction.getId());
            }
        }
    }

    private static void applyRangeAction(RangeAction rangeAction, Network network, String cracVariantId, String preventiveStateId) {
        RangeActionResultExtension resultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error("Could not find results on range action {}", rangeAction.getId());
        } else {
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                if (!Double.isNaN(rangeActionResult.getSetPoint(preventiveStateId))) {
                    LOGGER.debug("Applying range action {}: tap {}", rangeAction.getName(), ((PstRangeResult) rangeActionResult).getTap(preventiveStateId));
                }
                rangeAction.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
            } else {
                LOGGER.error("Could not find results for variant {} on range action {}", cracVariantId, rangeAction.getId());
            }
        }
    }

    /**
     * Apply preventive remedial actions saved in CRAC result extension on current working variant of given network,
     * with automatically selected cracVariantId.
     *
     * @param network Network on which remedial actions should be applied
     * @param crac CRAC that should contain result extension
     */
    public static void applyPreventiveRemedialActions(Network network, Crac crac) {

        CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager != null && cracExtension != null) {
            String cracVariantId = findPostOptimVariant(resultVariantManager, cracExtension);
            applyPreventiveRemedialActions(network, crac, cracVariantId);
        } else {
            LOGGER.info("Could not find postOptimVariant");
        }
    }

    // Find post optim variant if any
    // this comes from CNEHelper (until String cracVariantId) ...
    private static String findPostOptimVariant(ResultVariantManager resultVariantManager, CracResultExtension cracExtension) {
        List<String> variants = new ArrayList<>(resultVariantManager.getVariants());
        String postOptimVariantId = variants.get(0);

        double minCost = cracExtension.getVariant(variants.get(0)).getCost();
        for (String variant : variants) {
            if (cracExtension.getVariant(variant).getCost() <= minCost) {
                minCost = cracExtension.getVariant(variant).getCost();
                postOptimVariantId = variant;
            }
        }
        return postOptimVariantId;
    }
}
