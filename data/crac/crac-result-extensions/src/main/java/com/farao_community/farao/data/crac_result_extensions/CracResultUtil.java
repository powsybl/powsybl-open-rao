/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
                LOGGER.error(String.format("Could not find results on network action %s", na.getId()));
                return;
            }
            NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
            if (networkActionResult != null) {
                if (networkActionResult.isActivated(preventiveStateId)) {
                    na.apply(network);
                }
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on network action %s", cracVariantId, na.getId()));
            }
        });
        crac.getRangeActions().forEach(ra -> {
            RangeActionResultExtension resultExtension = ra.getExtension(RangeActionResultExtension.class);
            if (resultExtension == null) {
                LOGGER.error(String.format("Could not find results on range action %s", ra.getId()));
                return;
            }
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                ra.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on range action %s", cracVariantId, ra.getId()));
            }
        });
    }

    /**
     * Creates a new usage rule (FORCED) in CRAC for all the PRAs selected in CRAC result extension.
     *
     * @param crac CRAC that should contain result extension
     */
    public static void setAllSelectedPrasToForced(Crac crac) {

        CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager != null && cracExtension != null) {

            String cracVariantId = findPostOptimVariant(resultVariantManager, cracExtension);

            // Convert activated resultExtension into enforced UsageRule
            String preventiveStateId = crac.getPreventiveState().getId();
            crac.getNetworkActions().forEach(na -> setForcedNetworkAction(na, preventiveStateId, cracVariantId));
            crac.getRangeActions().forEach(ra -> {
                NetworkAction networkAction = setForcedRangeAction(ra, preventiveStateId, cracVariantId);
                if (networkAction != null) {
                    crac.getNetworkActions().add(networkAction);
                }
            });
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

    private static void setForcedNetworkAction(NetworkAction na, String preventiveStateId, String cracVariantId) {
        NetworkActionResultExtension resultExtension = na.getExtension(NetworkActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error(String.format("Could not find results on network action %s", na.getId()));
            return;
        }
        NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
        if (networkActionResult != null) {
            if (networkActionResult.isActivated(preventiveStateId)) {
                na.addUsageRule(new FreeToUseImpl(UsageMethod.FORCED, new Instant(preventiveStateId, 0)));
            }
        } else {
            LOGGER.error(String.format("Could not find results for variant %s on network action %s", cracVariantId, na.getId()));
        }
    }

    private static PstSetpoint setForcedRangeAction(RangeAction ra, String preventiveStateId, String cracVariantId) {
        RangeActionResultExtension resultExtension = ra.getExtension(RangeActionResultExtension.class);
        if (resultExtension != null) {
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                if (ra instanceof PstRange) {
                    Optional<NetworkElement> networkElement = ra.getNetworkElements().stream().findAny();
                    if (networkElement.isPresent()) {
                        return new PstSetpoint(ra.getId(),
                            ra.getName(),
                            ra.getOperator(),
                            Collections.singletonList(new FreeToUseImpl(UsageMethod.FORCED, new Instant(preventiveStateId, 0))),
                            networkElement.get(),
                            rangeActionResult.getSetPoint(preventiveStateId),
                            RangeDefinition.CENTERED_ON_ZERO);
                    }
                } else {
                    LOGGER.error(String.format("Unhandled range action type for %s", ra.getId()));
                }
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on range action %s", cracVariantId, ra.getId()));
            }
        } else {
            LOGGER.error(String.format("Could not find results on range action %s", ra.getId()));
        }
        return null;
    }

    public static void applyEnforcedPrasOnNetwork(Network network, Crac crac) {
        crac.getNetworkActions().forEach(na -> applyForcedNetworkAction(na, network));
    }

    private static void applyForcedNetworkAction(NetworkAction networkAction, Network network) {
        if (networkAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getUsageMethod().equals(UsageMethod.FORCED))) {
            LOGGER.debug("Applying network action {}", networkAction.getName());
            networkAction.apply(network);
        }
    }
}
