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

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginViolationEvaluator implements CnecViolationCostEvaluator {
    private final MinMarginEvaluator minMarginEvaluator;
    private final CnecMarginManager cnecMarginManager;
    private static final double OVERLOAD_PENALTY = 10000d; // TODO : set this in RAO parameters

    public MinMarginViolationEvaluator(CnecMarginManager cnecMarginManager) {
        this.minMarginEvaluator = new MinMarginEvaluator(cnecMarginManager);
        this.cnecMarginManager = cnecMarginManager;
    }

    @Override
    public String getName() {
        return "min-margin-violation-evaluator";
    }

    @Override
    public double evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return Math.max(0.0, minMarginEvaluator.evaluate(flowResult, remedialActionActivationResult, contingenciesToExclude)) * OVERLOAD_PENALTY;
    }

    @Override
    public Unit getUnit() {
        return cnecMarginManager.unit();
    }

    @Override
    public List<FlowCnec> getElementsInViolation(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> flowCnecsByMargin = cnecMarginManager.sortFlowCnecsByMargin(flowResult, contingenciesToExclude);
        return flowCnecsByMargin.isEmpty() ? List.of() : List.of(flowCnecsByMargin.get(0));
    }
}
