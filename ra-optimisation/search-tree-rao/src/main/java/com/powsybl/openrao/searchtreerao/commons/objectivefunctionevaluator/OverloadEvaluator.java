/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OverloadEvaluator extends MinMarginEvaluator {
    private static final double OVERLOAD_PENALTY = 10000d; // TODO : set this in RAO parameters

    public OverloadEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        super(flowCnecs, unit, marginEvaluator);
    }

    @Override
    public String getName() {
        return "overload-evaluator";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        Pair<Double, List<FlowCnec>> costAndLimitingElements = super.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, contingenciesToExclude);
        return Pair.of(Math.max(0.0, costAndLimitingElements.getLeft()) * OVERLOAD_PENALTY, costAndLimitingElements.getRight());
    }
}
