/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CnecViolationCostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.*;
import org.apache.commons.lang3.tuple.Pair;

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

    public ObjectiveFunctionResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        return new ObjectiveFunctionResultImpl(this, flowResult, remedialActionActivationResult);
    }

    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        return getFunctionalCostAndLimitingElements(flowResult, remedialActionActivationResult, Set.of());
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return Pair.of(functionalCostEvaluator.evaluate(flowResult, remedialActionActivationResult, contingenciesToExclude), FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult, contingenciesToExclude));
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, String virtualCostName, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> Pair.of(costEvaluator.evaluate(flowResult, remedialActionActivationResult, contingenciesToExclude), getVirtualCostCostlyElements(costEvaluator, flowResult, contingenciesToExclude)))
            .orElse(Pair.of(Double.NaN, new ArrayList<>()));
    }

    private static List<FlowCnec> getVirtualCostCostlyElements(CostEvaluator virtualCostEvaluator, FlowResult flowResult, Set<String> contingenciesToExclude) {
        return virtualCostEvaluator instanceof CnecViolationCostEvaluator violationCostEvaluator ? violationCostEvaluator.getElementsInViolation(flowResult, contingenciesToExclude) : new ArrayList<>();
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
