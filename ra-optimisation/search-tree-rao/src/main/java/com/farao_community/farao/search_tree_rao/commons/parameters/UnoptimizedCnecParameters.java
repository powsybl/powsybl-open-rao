/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;
    private final double highestThresholdValue;

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize, double highestThresholdValue) {
        this.operatorNotToOptimize = operatorNotToOptimize;
        this.highestThresholdValue = highestThresholdValue;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    public double getHighestThresholdValue() {
        return highestThresholdValue;
    }

    public static UnoptimizedCnecParameters build(RaoParameters raoParameters, Set<String> operatorsNotSharingCras, Set<FlowCnec> flowCnecs) {

        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }

        if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            return new UnoptimizedCnecParameters(
                operatorsNotSharingCras,
                RaoUtil.getLargestCnecThreshold(flowCnecs));
        } else {
            return null;
        }
    }
}
