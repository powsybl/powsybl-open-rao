/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginViolationEvaluator extends MinMarginEvaluator implements CnecViolationCostEvaluator {
    private static final double OVERLOAD_PENALTY = 10000d; // TODO : set this in RAO parameters

    public MinMarginViolationEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        super(flowCnecs, unit, marginEvaluator);
    }

    @Override
    public String getName() {
        return "min-margin-violation-evaluator";
    }

    @Override
    public List<FlowCnec> getElementsInViolation(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> flowCnecsByMargin = FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult, contingenciesToExclude);
        return flowCnecsByMargin.isEmpty() ? List.of() : List.of(flowCnecsByMargin.get(0));
    }

    @Override
    protected double computeCostForState(FlowResult flowResult, Set<FlowCnec> flowCnecsOfState) {
        return Math.max(0, super.computeCostForState(flowResult, flowCnecsOfState)) * OVERLOAD_PENALTY;
    }
}
