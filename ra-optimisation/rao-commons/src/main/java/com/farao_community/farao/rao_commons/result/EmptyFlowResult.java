/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_commons.result_api.FlowResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyFlowResult implements FlowResult {

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return new HashSet<>();
    }
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
