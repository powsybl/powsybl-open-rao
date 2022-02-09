/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyFlowResultImpl implements FlowResult {

    @Override
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        return Double.NaN;
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
        return Collections.emptyMap();
    }
}
