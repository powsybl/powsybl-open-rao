/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.openrao.commons.MultiScenarioTemporalData;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.roda.scenariorepository.ScenarioRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;


/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MultiScenarioObjectiveFunction {
    private final MultiScenarioTemporalData<ObjectiveFunction> objectiveFunctions;

    public MultiScenarioObjectiveFunction(MultiScenarioTemporalData<ObjectiveFunction> objectiveFunctions) {
        this.objectiveFunctions = objectiveFunctions;
    }

    public ObjectiveFunctionResult evaluate(MultiScenarioTemporalData<? extends FlowResult> flowResults) {
        return evaluate(flowResults, null);
    }

    public ObjectiveFunctionResult evaluate(MultiScenarioTemporalData<? extends FlowResult> flowResults, TemporalData<RemedialActionActivationResult> remedialActionActivationResults) {
        ObjectiveFunctionResult worstResult = null;
        String worstScenario = null;
        for (String scenario : objectiveFunctions.getScenarios()) {
            ObjectiveFunctionResult sumOnAllTimestamps = null;
            for (OffsetDateTime ts : objectiveFunctions.getTimestamps()) {
                sumOnAllTimestamps = sum(sumOnAllTimestamps, objectiveFunctions.get(scenario, ts).orElseThrow().evaluate(flowResults.get(scenario, ts).orElseThrow(), remedialActionActivationResults == null ? new EmptyRemedialActionActivationResult() : remedialActionActivationResults.getData(ts).orElseThrow()));
            }
            if ((worstResult == null) || (sumOnAllTimestamps.getCost() > worstResult.getCost())) {
                worstResult = sumOnAllTimestamps;
                worstScenario = scenario;
            }
        }
        TECHNICAL_LOGS.info(String.format("Worst scenario %s: cost = %.0f (functional: %.0f, virtual: %.0f)", worstScenario, worstResult.getCost(), worstResult.getFunctionalCost(), worstResult.getVirtualCost()));
        return worstResult;
    }

    public static MultiScenarioObjectiveFunction build(
        Set<FlowCnec> flowCnecs,
        Set<FlowCnec> loopFlowCnecs,
        MultiScenarioTemporalData<? extends FlowResult> initialFlowResults, // we actually only need the FlowResult
        MultiScenarioTemporalData<? extends FlowResult> prePerimeterFlowResults, // we actually only need the FlowResult
        Set<String> operatorsNotToOptimizeInCurative,
        RaoParameters raoParameters,
        Set<State> optimizedStates
    ) {
        MultiScenarioTemporalData<ObjectiveFunction> objectiveFunctions = new MultiScenarioTemporalData<>();
        for (String scenario : initialFlowResults.getScenarios()) {
            for (OffsetDateTime ts : initialFlowResults.getTimestamps()) {
                Set<FlowCnec> tsFlowCnecs = flowCnecs.stream().filter(cnec -> cnec.getState().getTimestamp().isPresent() && cnec.getState().getTimestamp().get().equals(ts)).collect(Collectors.toSet());
                Set<FlowCnec> tsLoopFlowCnecs = loopFlowCnecs.stream().filter(cnec -> cnec.getState().getTimestamp().isPresent() && cnec.getState().getTimestamp().get().equals(ts)).collect(Collectors.toSet());
                Set<State> tsStates = optimizedStates.stream().filter(state -> state.getTimestamp().isPresent() && state.getTimestamp().get().equals(ts)).collect(Collectors.toSet());
                ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
                    tsFlowCnecs, tsLoopFlowCnecs, initialFlowResults.get(scenario, ts).orElseThrow(), prePerimeterFlowResults.get(scenario, ts).orElseThrow(), operatorsNotToOptimizeInCurative, raoParameters, tsStates
                );
                objectiveFunctions.put(scenario, ts, objectiveFunction);
            }
        }
        return new MultiScenarioObjectiveFunction(objectiveFunctions);
    }

    private static ObjectiveFunctionResult sum(ObjectiveFunctionResult result1, ObjectiveFunctionResult result2) {
        if (result1 == null) {
            return result2;
        }
        if (result2 == null) {
            return result1;
        }
        return new ObjectiveFunctionResultImpl
            (
                result1.getFunctionalCost() + result2.getFunctionalCost(),
                sum(getVirtualCostMap(result1), getVirtualCostMap(result2)),
                List.of(), // TODO ?
                List.of() // TODO ?
            );
    }

    private static Map<String, Double> getVirtualCostMap(ObjectiveFunctionResult result) {
        Map<String, Double> virtualCosts = new HashMap<>();
        result.getVirtualCostNames().forEach(name -> virtualCosts.put(name, result.getVirtualCost(name)));
        return virtualCosts;
    }

    private static Map<String, Double> sum(Map<String, Double> map1, Map<String, Double> map2) {
        if (!map1.keySet().containsAll(map2.keySet()) || !map2.keySet().containsAll(map1.keySet())) {
            throw new OpenRaoException("map1 and map2 must have same keys");
        }
        Map<String, Double> result = new HashMap<>();
        for (String key : map1.keySet()) {
            result.put(key, map1.get(key) + map2.get(key));
        }
        return result;
    }
}
