/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityState;
import com.powsybl.sensitivity.SensitivityStateStatus;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Requires provider component numbers to refer to the current IIDM base topology.
// Post-contingency topologies may differ from both IIDM and the provider's internal model.
final class RelevantBaseComponents {
    private record ComponentKey(int connected, int synchronous) {
    }

    private RelevantBaseComponents() {
    }

    static boolean areSuccessful(Network network, SensitivityAnalysisResult result, SensitivityStateStatus stateStatus, List<SensitivityVariableSet> variableSets) {
        if (network == null || !stateStatus.getState().equals(SensitivityState.PRE_CONTINGENCY)) {
            return false;
        }
        List<SensitivityStateStatus.ComponentStatus> statuses = stateStatus.getComponentsLoadFlowStatusList();
        if (statuses.isEmpty() || statuses.stream().anyMatch(status -> status.status() == LoadFlowResult.ComponentResult.Status.NO_CALCULATION)) {
            return false;
        }
        Set<ComponentKey> networkComponents = network.getBusView().getBusStream().map(RelevantBaseComponents::componentOf)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<ComponentKey> reportedComponents = statuses.stream()
            .map(status -> new ComponentKey(status.connectedComponentNum(), status.synchronousComponentNum())).collect(Collectors.toSet());
        if (reportedComponents.size() != statuses.size() || !networkComponents.containsAll(reportedComponents)) {
            return false;
        }
        Map<Integer, SensitivityValue> values = result.getPreContingencyValues().stream()
            .collect(Collectors.toMap(SensitivityValue::getFactorIndex, Function.identity(), (first, second) -> first));
        if (values.size() != result.getPreContingencyValues().size()) {
            return false;
        }
        List<SensitivityFactor> factors = result.getFactors();
        List<Integer> baseFactors = IntStream.range(0, factors.size()).filter(index -> {
            ContingencyContextType type = factors.get(index).getContingencyContext().getContextType();
            return type == ContingencyContextType.NONE || type == ContingencyContextType.ALL;
        }).boxed().toList();
        return !baseFactors.isEmpty() && baseFactors.stream().allMatch(index -> {
            SensitivityFactor factor = factors.get(index);
            SensitivityValue value = values.get(index);
            return value != null && Double.isFinite(value.getValue()) && Double.isFinite(value.getFunctionReference())
                && isSuccessful(functionTerminal(network, factor), statuses)
                && variableIsSuccessful(network, factor, statuses, variableSets);
        });
    }

    private static Terminal functionTerminal(Network network, SensitivityFactor factor) {
        var element = network.getIdentifiable(factor.getFunctionId());
        int side = factor.getFunctionType().getSide().orElse(0);
        if (element instanceof Branch<?> branch) {
            return side == 1 ? branch.getTerminal1() : side == 2 ? branch.getTerminal2() : null;
        }
        if (element instanceof BoundaryLine boundaryLine && (side == 1 || side == 2)) {
            return boundaryLine.getTerminal();
        }
        return null;
    }

    private static boolean variableIsSuccessful(Network network, SensitivityFactor factor, List<SensitivityStateStatus.ComponentStatus> statuses,
                                                List<SensitivityVariableSet> variableSets) {
        if (factor.isVariableSet()) {
            List<SensitivityVariableSet> matches = variableSets.stream().filter(set -> set.getId().equals(factor.getVariableId())).toList();
            return factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER
                && matches.size() == 1 && !matches.get(0).getVariables().isEmpty()
                && matches.get(0).getVariables().stream().allMatch(variable -> variableIsSuccessful(network, variable.getId(), statuses));
        }
        switch (factor.getVariableType()) {
            case INJECTION_ACTIVE_POWER, INJECTION_REACTIVE_POWER, TRANSFORMER_PHASE:
                break;
            default:
                return false;
        }
        return variableIsSuccessful(network, factor.getVariableId(), statuses);
    }

    private static boolean variableIsSuccessful(Network network, String variableId, List<SensitivityStateStatus.ComponentStatus> statuses) {
        var variable = network.getIdentifiable(variableId);
        return variable instanceof Connectable<?> connectable && !connectable.getTerminals().isEmpty()
            && connectable.getTerminals().stream().allMatch(terminal -> isSuccessful(terminal, statuses));
    }

    private static boolean isSuccessful(Terminal terminal, List<SensitivityStateStatus.ComponentStatus> statuses) {
        Bus bus = terminal == null ? null : terminal.getBusView().getBus();
        ComponentKey component = componentOf(bus);
        if (component == null) {
            return false;
        }
        List<SensitivityStateStatus.ComponentStatus> matching = statuses.stream()
            .filter(status -> status.connectedComponentNum() == component.connected() && status.synchronousComponentNum() == component.synchronous()).toList();
        return matching.size() == 1 && matching.get(0).status() == LoadFlowResult.ComponentResult.Status.CONVERGED;
    }

    private static ComponentKey componentOf(Bus bus) {
        if (bus == null || bus.getConnectedComponent() == null || bus.getSynchronousComponent() == null) {
            return null;
        }
        return new ComponentKey(bus.getConnectedComponent().getNum(), bus.getSynchronousComponent().getNum());
    }
}
