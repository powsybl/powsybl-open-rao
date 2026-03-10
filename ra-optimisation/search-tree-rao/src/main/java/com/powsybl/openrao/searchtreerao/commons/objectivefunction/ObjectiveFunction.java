/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class ObjectiveFunction {
    private final CostEvaluator functionalCostEvaluator;
    private final List<CostEvaluator> virtualCostEvaluators;
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;

    ObjectiveFunction(CostEvaluator functionalCostEvaluator, List<CostEvaluator> virtualCostEvaluators, Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        Objects.requireNonNull(functionalCostEvaluator);
        this.functionalCostEvaluator = functionalCostEvaluator;
        this.virtualCostEvaluators = virtualCostEvaluators;
        this.flowCnecs = new HashSet<>(flowCnecs);
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    public ObjectiveFunctionResult evaluate(final FlowResult flowResult,
                                            final RemedialActionActivationResult remedialActionActivationResult,
                                            final ReportNode reportNode) {
        return new ObjectiveFunctionResultImpl(
            functionalCostEvaluator.evaluate(flowResult, remedialActionActivationResult, reportNode),
            virtualCostEvaluators.stream().collect(Collectors.toMap(CostEvaluator::getName, virtualCost -> virtualCost.evaluate(flowResult, remedialActionActivationResult, reportNode))),
            FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult));
    }

    public static ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs, RaoParameters raoParameters, Set<State> optimizedStates) {
        return new InitialSensitivityAnalysisObjectiveFunctionCreator(flowCnecs, optimizedStates, raoParameters).create();
    }

    public static ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                          Set<FlowCnec> loopFlowCnecs,
                                          FlowResult initialFlowResult,
                                          FlowResult prePerimeterFlowResult,
                                          Set<String> operatorsNotToOptimizeInCurative,
                                          RaoParameters raoParameters,
                                          Set<State> optimizedStates) {
        return new ObjectiveFunctionCreator(flowCnecs, optimizedStates, raoParameters, loopFlowCnecs, initialFlowResult, prePerimeterFlowResult, operatorsNotToOptimizeInCurative).create();
    }
}
