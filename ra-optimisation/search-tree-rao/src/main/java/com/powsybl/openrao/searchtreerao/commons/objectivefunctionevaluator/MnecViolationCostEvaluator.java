/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumCnecWiseCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluatorUtils.sortFlowCnecsByDecreasingCost;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final FlowResult initialFlowResult;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecViolationCost;

    public MnecViolationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, FlowResult initialFlowResult, double mnecAcceptableMarginDecrease, double mnecViolationCost) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.initialFlowResult = initialFlowResult;
        this.mnecAcceptableMarginDecrease = mnecAcceptableMarginDecrease;
        this.mnecViolationCost = mnecViolationCost;
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> costPerMnec = flowCnecs.stream().filter(Cnec::isMonitored)
            .collect(Collectors.toMap(Function.identity(), mnec -> computeMnecViolation(flowResult, mnec)))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() * mnecViolationCost));

        if (costPerMnec.values().stream().anyMatch(mnecViolationCost -> mnecViolationCost > 0)) {
            // will be logged even if the contingency is filtered out at some point
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Some MNEC constraints are not respected.");
        }

        List<FlowCnec> sortedMnecs = sortFlowCnecsByDecreasingCost(costPerMnec);
        return new SumCnecWiseCostEvaluatorResult(costPerMnec, sortedMnecs);
    }

    private double computeMnecViolation(FlowResult flowResult, FlowCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, unit);
        double currentMargin = flowResult.getMargin(mnec, unit);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDecrease * RaoUtil.getFlowUnitMultiplier(mnec, TwoSides.ONE, Unit.MEGAWATT, unit)) - currentMargin);
    }
}
