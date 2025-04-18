/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AutomatonBatch implements Comparable<AutomatonBatch> {
    private final int timeAfterOutage;
    private final Set<NetworkAction> topologicalAutomatons;
    private final Set<RangeAction<?>> rangeAutomatons;

    public AutomatonBatch(int timeAfterOutage) {
        this.timeAfterOutage = timeAfterOutage;
        this.topologicalAutomatons = new HashSet<>();
        this.rangeAutomatons = new HashSet<>();
    }

    public int getTimeAfterOutage() {
        return timeAfterOutage;
    }

    public Set<NetworkAction> getTopologicalAutomatons() {
        return topologicalAutomatons;
    }

    public Set<RangeAction<?>> getRangeAutomatons() {
        return rangeAutomatons;
    }

    public void add(RemedialAction<?> automaton) {
        // TODO: default speed is 0, see if we want to keep this
        if (automaton.getSpeed().orElse(0) != timeAfterOutage) {
            throw new OpenRaoException("The speed of automaton %s is inconsistent with the automaton batch speed (%s).".formatted(automaton.getId(), timeAfterOutage));
        }
        if (automaton instanceof NetworkAction topologicalAutomaton) {
            topologicalAutomatons.add(topologicalAutomaton);
        } else if (automaton instanceof RangeAction<?> rangeAutomaton) {
            rangeAutomatons.add(rangeAutomaton);
        }
    }

    public OptimizationResult simulate(Network network, State automatonState, OptimizationResult preBatchOptimizationResult) {
        Set<NetworkAction> appliedTopologicalAutomatons = preBatchOptimizationResult.getActivatedNetworkActions();
        Map<RangeAction<?>, Double> appliedAutomatonSetPoints = preBatchOptimizationResult.getOptimizedSetpointsOnState(automatonState);
        TECHNICAL_LOGS.info("Simulating automaton batch for state {} and speed {}", automatonState.getId(), timeAfterOutage);

        // STEP 1: Simulate topological automatons
        OptimizationResult postTopologicalAutomatonsResult = simulateTopologicalAutomatons(network, automatonState, preBatchOptimizationResult, appliedAutomatonSetPoints);
        if (AutomatonSimulationUtils.isSecure(postTopologicalAutomatonsResult)) {
            return postTopologicalAutomatonsResult;
        }

        // STEP 2: Simulate range automatons
        return null;
    }

    private OptimizationResult simulateTopologicalAutomatons(Network network, State automatonState, OptimizationResult preBatchOptimizationResult, Map<RangeAction<?>, Double> appliedAutomatonSetPoints) {
        Set<NetworkAction> topologicalAutomatonsToSimulate = new HashSet<>();
        topologicalAutomatons.forEach(
            networkAction -> {
                if (networkAction.hasImpactOnNetwork(network)) {
                    topologicalAutomatonsToSimulate.add(networkAction);
                } else {
                    TECHNICAL_LOGS.info("Automaton {} - {} has been skipped as it has no impact on network.", networkAction.getId(), networkAction.getName());
                }
            }
        );

        if (topologicalAutomatonsToSimulate.isEmpty()) {
            return preBatchOptimizationResult;
        }

        topologicalAutomatonsToSimulate.forEach(
            networkAction -> {
                TECHNICAL_LOGS.debug("Activating automaton {} - {}.", networkAction.getId(), networkAction.getName());
                networkAction.apply(network);
            }
        );

        // -- Sensitivity analysis must be run to evaluate available auto range actions
        // -- If network actions have been applied, run sensitivity :
        TECHNICAL_LOGS.info("Running sensitivity analysis post application of topological automatons for automaton state {} and batch speed {}.", automatonState.getId(), timeAfterOutage);
        PrePerimeterResult automatonRangeActionOptimizationSensitivityAnalysisOutput = preAutoPstOptimizationSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialFlowResult, operatorsNotSharingCras, null);
        if (automatonRangeActionOptimizationSensitivityAnalysisOutput.getSensitivityStatus(automatonState) == ComputationStatus.FAILURE) {
            return new AutomatonSimulator.TopoAutomatonSimulationResult(automatonRangeActionOptimizationSensitivityAnalysisOutput, topologicalAutomatonsToSimulate);
        }
        RaoLogger.logMostLimitingElementsResults(TECHNICAL_LOGS, automatonRangeActionOptimizationSensitivityAnalysisOutput, Set.of(automatonState), raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), numberLoggedElementsDuringRao);
    }

    @Override
    public int compareTo(AutomatonBatch automatonBatch) {
        return Integer.compare(timeAfterOutage, automatonBatch.getTimeAfterOutage());
    }
}
