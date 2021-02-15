/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_commons.RaoUtil;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
final class SearchTreeRaoLogger {

    private SearchTreeRaoLogger() {
    }

    static void logRangeActions(Leaf leaf) {
        logRangeActions(leaf, null);
    }

    static void logRangeActions(Leaf leaf, String prefix) {
        StringBuilder rangeActionMsg = new StringBuilder();
        if (prefix != null) {
            rangeActionMsg.append(prefix).append(" - ");
        }
        rangeActionMsg.append("Range action(s): ");
        for (RangeAction rangeAction : leaf.getRaoData().getAvailableRangeActions()) {
            String rangeActionName = rangeAction.getName();
            int rangeActionTap = ((PstRangeResult) rangeAction.getExtension(RangeActionResultExtension.class)
                    .getVariant(leaf.getRaoData().getWorkingVariantId()))
                    .getTap(leaf.getRaoData().getOptimizedState().getId());
            rangeActionMsg
                    .append(format("%s: %d", rangeActionName, rangeActionTap))
                    .append(" , ");
        }
        String rangeActionsLog = rangeActionMsg.toString();
        SearchTree.LOGGER.info(rangeActionsLog);
    }

    static void logMostLimitingElementsResults(Leaf leaf, Unit unit, boolean relativePositiveMargins, int numberOfLoggedElements) {
        logMostLimitingElementsResults(leaf.getRaoData().getCnecs(), leaf.getBestVariantId(), unit, relativePositiveMargins, numberOfLoggedElements);
    }

    static void logMostLimitingElementsResults(Set<BranchCnec> cnecs, String variantId, Unit unit, boolean relativePositiveMargins, int numberOfLoggedElements) {
        List<BranchCnec> sortedCnecs = cnecs.stream().
                filter(Cnec::isOptimized).
                sorted(Comparator.comparingDouble(cnec -> RaoUtil.computeCnecMargin(cnec, variantId, unit, relativePositiveMargins))).
                collect(Collectors.toList());

        for (int i = 0; i < Math.min(numberOfLoggedElements, sortedCnecs.size()); i++) {
            BranchCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = RaoUtil.computeCnecMargin(cnec, variantId, unit, relativePositiveMargins);
            String margin = new DecimalFormat("#0.00").format(cnecMargin);
            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? "relative " : "";
            String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format("(PTDF %f)", cnec.getExtension(CnecResultExtension.class).getVariant(variantId).getAbsolutePtdfSum()) : "";
            SearchTree.LOGGER.info("Limiting element #{}: element {} at state {} with a {}margin of {} {} {}",
                    i + 1,
                    cnecNetworkElementName,
                    cnecStateId,
                    isRelativeMargin,
                    margin,
                    unit,
                    ptdfIfRelative);
        }
    }
}
