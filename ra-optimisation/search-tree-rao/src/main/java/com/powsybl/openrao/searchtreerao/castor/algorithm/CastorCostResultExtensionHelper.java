/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

/**
 * Utility class used to convert {@link ObjectiveFunctionResult}s to a
 * {@link CostResult} RAO Result extension.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class CastorCostResultExtensionHelper {
    private CastorCostResultExtensionHelper() {
    }

    public static CostResult convertToExtension(ObjectiveFunctionResult initialResult) {
        CostResult costResult = new CostResult();
        addInitialCosts(initialResult, costResult);
        return costResult;
    }

    public static CostResult convertToExtension(ObjectiveFunctionResult initialResult,
                                                ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                                ObjectiveFunctionResult postPraResult,
                                                boolean costOptimization,
                                                Instant preventiveInstant) {
        CostResult costResult = convertToExtension(initialResult);
        addPreventiveCosts(preventiveAndOutageOnlyResult, postPraResult, costResult, preventiveInstant, costOptimization);
        return costResult;
    }

    public static CostResult convertToExtension(ObjectiveFunctionResult initialResult,
                                                ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                                ObjectiveFunctionResult postPraResult,
                                                Map<State, PostPerimeterResult> postContingencyResults,
                                                boolean costOptimization,
                                                Crac crac) {
        CostResult costResult = convertToExtension(
            initialResult,
            preventiveAndOutageOnlyResult,
            postPraResult,
            costOptimization,
            crac.getPreventiveInstant()
        );
        for (Instant instant : crac.getSortedInstants()) {
            if (instant.isAuto() || instant.isCurative()) {
                costResult.addFunctionalCostResult(
                    instant,
                    computeFunctionalCost(preventiveAndOutageOnlyResult, postContingencyResults, costOptimization, instant)
                );
                postPraResult.getVirtualCostNames().forEach(
                    virtualCostName -> costResult.addVirtualCostResult(
                        instant,
                        virtualCostName,
                        computeVirtualCost(virtualCostName, preventiveAndOutageOnlyResult, postContingencyResults, instant)
                    )
                );
            }
        }
        return costResult;
    }

    private static double computeFunctionalCost(ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                                Map<State, PostPerimeterResult> postContingencyResults,
                                                boolean costOptimization,
                                                Instant instant) {
        BinaryOperator<Double> operator = costOptimization ? Double::sum : Math::max;

        // initialize cost to preventive optimization cost
        AtomicReference<Double> totalCost = new AtomicReference<>(preventiveAndOutageOnlyResult.getFunctionalCost());

        // for states which come strictly before optimizedInstant, consider optimizationResult
        postContingencyResults.entrySet().stream()
            .filter(stateAndResult -> stateAndResult.getKey().getInstant().comesBefore(instant))
            .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().optimizationResult().getFunctionalCost())));

        // for states which have same instant as optimizedInstant, consider prePerimeterResultForAllFollowingStates
        postContingencyResults.entrySet().stream()
            .filter(stateAndResult -> stateAndResult.getKey().getInstant().equals(instant))
            .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(),
                //for costly use optim result; for max min margin use prePerimeter result
                costOptimization ?
                    stateAndResult.getValue().optimizationResult().getFunctionalCost() :
                    stateAndResult.getValue().prePerimeterResultForAllFollowingStates().getFunctionalCost()))
            );

        return totalCost.get();
    }

    private static double computeVirtualCost(String virtualCostName,
                                             ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                             Map<State, PostPerimeterResult> postContingencyResults,
                                             Instant instant) {
        BinaryOperator<Double> operator;
        if ("min-margin-violation-evaluator".equals(virtualCostName) || "sensitivity-failure-cost".equals(virtualCostName)) {
            operator = Math::max;
        } else {
            operator = Double::sum;
        }

        // initialize cost to preventive optimization cost
        AtomicReference<Double> totalCost = new AtomicReference<>(preventiveAndOutageOnlyResult.getVirtualCost(virtualCostName));

        // for states which come strictly before optimizedInstant, consider optimizationResult
        postContingencyResults.entrySet().stream()
            .filter(stateAndResult -> stateAndResult.getKey().getInstant().comesBefore(instant))
            .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().optimizationResult().getVirtualCost(virtualCostName))));

        // for states which have same instant as optimizedInstant, consider prePerimeterResultForAllFollowingStates
        postContingencyResults.entrySet().stream()
            .filter(stateAndResult -> stateAndResult.getKey().getInstant().equals(instant))
            .forEach(stateAndResult -> totalCost.set(operator.apply(totalCost.get(), stateAndResult.getValue().prePerimeterResultForAllFollowingStates().getVirtualCost(virtualCostName))));

        return totalCost.get();
    }

    private static void addInitialCosts(ObjectiveFunctionResult objectiveFunctionResult, CostResult costResult) {
        costResult.addFunctionalCostResult(null, objectiveFunctionResult.getFunctionalCost());
        objectiveFunctionResult.getVirtualCostNames().forEach(
            virtualCostName -> {
                double virtualCost = objectiveFunctionResult.getVirtualCost(virtualCostName);
                costResult.addVirtualCostResult(null, virtualCostName, Double.isNaN(virtualCost) ? 0 : virtualCost);
            }
        );
    }

    private static void addPreventiveCosts(ObjectiveFunctionResult preventiveAndOutageOnlyResult,
                                           ObjectiveFunctionResult postPraResult,
                                           CostResult costResult,
                                           Instant preventiveInstant,
                                           boolean costOptimization) {
        // for costly optimization, we only care about the cost of preventive actions (for after PRA result)
        costResult.addFunctionalCostResult(preventiveInstant, (costOptimization ? preventiveAndOutageOnlyResult : postPraResult).getFunctionalCost());
        postPraResult.getVirtualCostNames().forEach(
            virtualCostName -> {
                double virtualCost = postPraResult.getVirtualCost(virtualCostName);
                costResult.addVirtualCostResult(preventiveInstant, virtualCostName, Double.isNaN(virtualCost) ? 0 : virtualCost);
            }
        );
    }
}
