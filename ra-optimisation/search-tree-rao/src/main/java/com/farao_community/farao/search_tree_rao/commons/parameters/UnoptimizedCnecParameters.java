/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;
    private final Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts;

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize, Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts) {
        this.operatorNotToOptimize = operatorNotToOptimize;
        this.unoptimizedCnecsInSeriesWithPsts = unoptimizedCnecsInSeriesWithPsts;
    }

    public Map<FlowCnec, PstRangeAction>  getUnoptimizedCnecsInSeriesWithPsts() {
        return unoptimizedCnecsInSeriesWithPsts;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    // unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together.
    public static UnoptimizedCnecParameters build(RaoParameters raoParameters, Set<String> operatorsNotSharingCras, Crac crac) {
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }

        if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            return new UnoptimizedCnecParameters(
                operatorsNotSharingCras,
                null);
        } else if (!searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty()) {
            return new UnoptimizedCnecParameters(
                    null,
                    searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPsts(crac));
        } else {
            return null;
        }
    }
}
