/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AbsoluteCostEvaluatorResult implements CostEvaluatorResult {
    private final double cost;

    public AbsoluteCostEvaluatorResult(double cost) {
        this.cost = cost;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        return cost;
    }

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        return new ArrayList<>();
    }
}
