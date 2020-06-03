/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;

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

    static void logMostLimitingElementsResults(Leaf leaf, Crac crac) {
        List<Cnec> sortedCnecs = new ArrayList<>(crac.getCnecs());
        sortedCnecs.sort(Comparator.comparingDouble(cnec -> computeMarginInMW(cnec, leaf.getBestVariantId())));

        for (int i = 0; i < Math.min(MAX_LOGS_LIMITING_ELEMENTS, sortedCnecs.size()); i++) {
            Cnec cnec = sortedCnecs.get(i);
            SearchTreeRao.LOGGER.info(format("Limiting element #%d: element %s at state %s with a margin of %f",
                i + 1,
                cnec.getNetworkElement().getName(),
                cnec.getState().getId(),
                computeMarginInMW(cnec, leaf.getBestVariantId())));
        }
    }

    private static double computeMarginInMW(Cnec cnec, String variantId) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(variantId);
        return Math.min(cnecResult.getMaxThresholdInMW() - cnecResult.getFlowInMW(),
            cnecResult.getFlowInMW() - cnecResult.getMinThresholdInMW());
    }
}
