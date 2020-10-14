/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;

import java.text.DecimalFormat;
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
        SearchTreeRao.LOGGER.info(rangeActionsLog);
    }

    static void logMostLimitingElementsResults(Leaf leaf, Unit unit, boolean relativePositiveMargins) {
        List<Cnec> sortedCnecs = leaf.getRaoData().getCnecs().stream().
            filter(Cnec::isOptimized).
            sorted(Comparator.comparingDouble(cnec -> computeCnecMargin(cnec, leaf.getBestVariantId(), unit, relativePositiveMargins, leaf.getRaoData().getCracResult(leaf.getRaoData().getInitialVariantId())))).
            collect(Collectors.toList());

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            leaf.getRaoData().setWorkingVariant(leaf.getBestVariantId());
            double cnecMargin = computeCnecMargin(cnec, leaf.getBestVariantId(), unit, relativePositiveMargins, leaf.getRaoData().getCracResult(leaf.getRaoData().getInitialVariantId()));
            String margin = new DecimalFormat("#0.00").format(cnecMargin);
            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? "relative " : "";
            SearchTreeRao.LOGGER.info("Limiting element #{}: element {} at state {} with a {}margin of {} {}",
                i + 1,
                cnecNetworkElementName,
                cnecStateId,
                isRelativeMargin,
                margin,
                unit);
        }
    }

    private static double computeCnecMargin(Cnec cnec, String variantId, Unit unit, boolean relativePositiveMargins, CracResult cracResult) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        double actualValue = unit.equals(Unit.MEGAWATT) ? cnecResult.getFlowInMW() : cnecResult.getFlowInA();
        double absoluteMargin = cnec.computeMargin(actualValue, unit);
        if (relativePositiveMargins && (absoluteMargin > 0)) {
            return absoluteMargin / cracResult.getAbsPtdfSums().get(cnec.getId());
        } else {
            return absoluteMargin;
        }
    }
}
