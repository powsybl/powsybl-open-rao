/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
final class SearchTreeRaoLogger {
    private static final int MAX_LOGS_LIMITING_ELEMENTS = 10;

    private SearchTreeRaoLogger() { }

    static void logRangeActions(Leaf leaf) {
        logRangeActions(leaf, null);
    }

    static void logRangeActions(Leaf leaf, String prefix) {
        StringBuilder rangeActionMsg = new StringBuilder();
        if (prefix != null) {
            rangeActionMsg.append(prefix).append(" - ");
        }
        rangeActionMsg.append("Range action(s): ");
        rangeActionMsg.append(leaf.getRaoData().getCrac().getRangeActions().stream()
            .map(rangeAction -> format("%s: %d", rangeAction.getName(),
                ((PstRangeResult) rangeAction.getExtension(RangeActionResultExtension.class)
                    .getVariant(leaf.getRaoData().getWorkingVariantId()))
                    .getTap(leaf.getRaoData().getCrac().getPreventiveState().getId())))
            .collect(Collectors.joining(", ")));
        SearchTreeRao.LOGGER.info(rangeActionMsg.toString());
    }

    static void logMostLimitingElementsResults(Leaf leaf, Crac crac, Unit unit) {
        List<Cnec> sortedCnecs = new ArrayList<>(crac.getCnecs());
        sortedCnecs.sort(Comparator.comparingDouble(cnec -> computeMarginInMW(cnec, leaf.getBestVariantId())));

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            SearchTreeRao.LOGGER.info(format("Limiting element #%d: element %s at state %s with a margin of %.2f %s",
                i + 1,
                cnec.getNetworkElement().getName(),
                cnec.getState().getId(),
                computeMarginInMW(cnec, leaf.getBestVariantId()),
                unit));
        }
    }

    private static double computeMarginInMW(Cnec cnec, String variantId) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        return Math.min(cnecResult.getMaxThresholdInMW() - cnecResult.getFlowInMW(),
            cnecResult.getFlowInMW() - cnecResult.getMinThresholdInMW());
    }
}
