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
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        for (RangeAction rangeAction : leaf.getRaoData().getCrac().getRangeActions()) {
            String rangeActionName = rangeAction.getName();
            int rangeActionTap = ((PstRangeResult) rangeAction.getExtension(RangeActionResultExtension.class)
                    .getVariant(leaf.getRaoData().getWorkingVariantId()))
                    .getTap(leaf.getRaoData().getCrac().getPreventiveState().getId());
            rangeActionMsg
                    .append(format("%s: %d", rangeActionName, rangeActionTap))
                    .append(" , ");
        }
        SearchTreeRao.LOGGER.info(rangeActionMsg.toString());
    }

    static void logMostLimitingElementsResults(Leaf leaf, Crac crac, Unit unit) {
        List<Cnec> sortedCnecs = new ArrayList<>(crac.getCnecs());
        sortedCnecs.sort(Comparator.comparingDouble(cnec -> computeCnecMargin(cnec, leaf.getBestVariantId(), unit)));

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            leaf.getRaoData().setWorkingVariant(leaf.getBestVariantId());
            double margin = computeCnecMargin(cnec, leaf.getBestVariantId(), unit);
            SearchTreeRao.LOGGER.info(format("Limiting element #%d: element %s at state %s with a margin of %.2f %s",
                i + 1,
                cnecNetworkElementName,
                cnecStateId,
                margin,
                unit));
        }
    }

    private static double computeCnecMargin(Cnec cnec, String variantId, Unit unit) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        unit.checkPhysicalParameter(PhysicalParameter.FLOW);
        double actualValue = unit.equals(Unit.MEGAWATT) ? cnecResult.getFlowInMW() : cnecResult.getFlowInA();
        return cnec.computeMargin(actualValue, unit);
    }
}
