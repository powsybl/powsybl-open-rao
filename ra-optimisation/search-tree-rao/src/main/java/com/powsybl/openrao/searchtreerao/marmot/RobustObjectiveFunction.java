/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.marmot.scenariobuilder.ScenarioRepo;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public class RobustObjectiveFunction {
    private final Map<String, ObjectiveFunction> objectiveFunctionPerScenario;

    public RobustObjectiveFunction(Map<String, ObjectiveFunction> objectiveFunctionPerScenario) {
        this.objectiveFunctionPerScenario = objectiveFunctionPerScenario;
    }

    public ObjectiveFunctionResult evaluate(Map<String, FlowResult> flowResults, RemedialActionActivationResult remedialActionActivationResult) {
        ObjectiveFunctionResult worstResult = null;
        String worstScenario = null;
        for (String scenario : objectiveFunctionPerScenario.keySet()) {
            ObjectiveFunctionResult result = objectiveFunctionPerScenario.get(scenario).evaluate(flowResults.get(scenario), remedialActionActivationResult);
            if ((worstResult == null) || (result.getCost() > worstResult.getCost())) {
                worstResult = result;
                worstScenario = scenario;
            }
        }
        TECHNICAL_LOGS.info(String.format("Worst scenario: %s, cost = %.2f", worstScenario, worstResult.getCost()));
        return worstResult;
    }

    public static RobustObjectiveFunction buildForInitialSensitivityComputation(ScenarioRepo scenarioRepo, Set<FlowCnec> flowCnecs, RaoParameters raoParameters, Set<State> optimizedStates) {
        Map<String, ObjectiveFunction> objectiveFunctionPerScenario = new HashMap<>();
        for (String scenario : scenarioRepo.getScenarios()) {
            ObjectiveFunction objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(
                flowCnecs, raoParameters, optimizedStates
            );
            objectiveFunctionPerScenario.put(scenario, objectiveFunction);
        }
        return new RobustObjectiveFunction(objectiveFunctionPerScenario);
    }

    public static RobustObjectiveFunction build(ScenarioRepo scenarioRepo,
                                                Set<FlowCnec> flowCnecs,
                                                Set<FlowCnec> loopFlowCnecs,
                                                Map<String, FlowResult> initialFlowResult,
                                                Map<String, FlowResult> prePerimeterFlowResult,
                                                Set<String> operatorsNotToOptimizeInCurative,
                                                RaoParameters raoParameters,
                                                Set<State> optimizedStates
    ) {
        Map<String, ObjectiveFunction> objectiveFunctionPerScenario = new HashMap<>();
        for (String scenario : scenarioRepo.getScenarios()) {
            ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
                flowCnecs, loopFlowCnecs, initialFlowResult.get(scenario), prePerimeterFlowResult.get(scenario), operatorsNotToOptimizeInCurative, raoParameters, optimizedStates
            );
            objectiveFunctionPerScenario.put(scenario, objectiveFunction);
        }
        return new RobustObjectiveFunction(objectiveFunctionPerScenario);
    }
}
