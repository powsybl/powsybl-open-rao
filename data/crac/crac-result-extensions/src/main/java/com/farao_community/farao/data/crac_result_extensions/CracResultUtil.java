/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        crac.getNetworkActions().forEach(na -> {
            NetworkActionResultExtension resultExtension = na.getExtension(NetworkActionResultExtension.class);
            if (resultExtension == null) {
                LOGGER.error("Could not find results on network action {}", na.getId());
            } else {
                NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
                if (networkActionResult != null) {
                    if (networkActionResult.isActivated(preventiveStateId)) {
                        LOGGER.debug("Applying network action {}", na.getName());
                        na.apply(network);
                    }
                } else {
                    LOGGER.error("Could not find results for variant {} on network action {}", cracVariantId, na.getId());
                }
            }
        });
        crac.getRangeActions().forEach(ra -> {
            RangeActionResultExtension resultExtension = ra.getExtension(RangeActionResultExtension.class);
            if (resultExtension == null) {
                LOGGER.error("Could not find results on range action {}", ra.getId());
            } else {
                RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
                if (rangeActionResult != null) {
                    if (!Double.isNaN(rangeActionResult.getSetPoint(preventiveStateId))) {
                        LOGGER.debug("Applying range action {}: tap {}", ra.getName(), ((PstRangeResult) rangeActionResult).getTap(preventiveStateId));
                    }
                    ra.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
                } else {
                    LOGGER.error("Could not find results for variant {} on range action {}", cracVariantId, ra.getId());
                }
            }
        });
    }
}
