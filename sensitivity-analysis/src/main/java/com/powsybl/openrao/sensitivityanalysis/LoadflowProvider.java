/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * To run a systematic sensitivity analysis and evaluate the flows in all states at once,
 * hades requires sensitivities. We therefore extend RangeActionSensitivityProvider to use
 * some of its conversion methods, and use a random PST from the network to create "dummy"
 * sensitivities for each studied cnec.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LoadflowProvider extends AbstractSimpleSensitivityProvider {

    Pair<String, SensitivityVariableType> defaultSensitivityVariable;

    LoadflowProvider(Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        if (afterContingencyOnly) {
            return factors;
        }
        Map<String, SensitivityVariableType> sensitivityVariables = new LinkedHashMap<>();
        addDefaultSensitivityVariable(network, sensitivityVariables);

        List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, null);

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);
        sensitivityFunctions.forEach(function ->
            sensitivityVariables.forEach((key, value) -> factors.add(new SensitivityFactor(function.getValue(), function.getKey(), value, key, false, preContingencyContext)))
        );
        return factors;
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            Map<String, SensitivityVariableType> sensitivityVariables = new LinkedHashMap<>();
            addDefaultSensitivityVariable(network, sensitivityVariables);

            List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, contingencyId);

            ContingencyContext contingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            sensitivityFunctions.forEach(function ->
                sensitivityVariables.forEach((key, value) -> factors.add(new SensitivityFactor(function.getValue(), function.getKey(), value, key, false, contingencyContext)))
            );
        }
        return factors;
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return new ArrayList<>();
    }

    private boolean willBeKeptInSensi(TwoWindingsTransformer twoWindingsTransformer) {
        return twoWindingsTransformer.getTerminal1().isConnected() && twoWindingsTransformer.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getTerminal2().isConnected() && twoWindingsTransformer.getTerminal2().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getPhaseTapChanger() != null;
    }

    @Override
    public Map<String, HvdcRangeAction> getHvdcs() {
        return new HashMap<>();
    }

    private boolean willBeKeptInSensi(Generator gen) {
        return gen.getTerminal().isConnected() && gen.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent();
    }

    void addDefaultSensitivityVariable(Network network, Map<String, SensitivityVariableType> sensitivityVariables) {
        if (!Objects.isNull(defaultSensitivityVariable)) {
            sensitivityVariables.put(defaultSensitivityVariable.getLeft(), defaultSensitivityVariable.getRight());
            return;
        }

        // First try to get a PST angle
        Optional<TwoWindingsTransformer> optionalPst = network.getTwoWindingsTransformerStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalPst.isPresent()) {
            sensitivityVariables.put(optionalPst.get().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
            defaultSensitivityVariable = Pair.of(optionalPst.get().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
            return;
        }

        // If no one found, pick a Generator injection
        Optional<Generator> optionalGen = network.getGeneratorStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalGen.isPresent()) {
            sensitivityVariables.put(optionalGen.get().getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            defaultSensitivityVariable = Pair.of(optionalGen.get().getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            return;
        }
        throw new OpenRaoException(String.format("Unable to create sensitivity factors. Did not find any varying element in network '%s'.", network.getId()));
    }

    List<Pair<String, SensitivityFunctionType>> getSensitivityFunctions(Network network, String contingencyId) {
        Set<FlowCnec> flowCnecs;
        if (Objects.isNull(contingencyId)) {
            flowCnecs = cnecsPerContingencyId.getOrDefault(null, new ArrayList<>()).stream()
                .filter(cnec -> cnec.isConnected(network))
                .collect(Collectors.toSet());
        } else {
            flowCnecs = cnecsPerContingencyId.getOrDefault(contingencyId, new ArrayList<>()).stream()
                .filter(cnec -> cnec.isConnected(network))
                .collect(Collectors.toSet());
        }

        Map<String, Set<TwoSides>> networkElementsAndSides = new HashMap<>();
        flowCnecs.forEach(flowCnec ->
            networkElementsAndSides.computeIfAbsent(flowCnec.getNetworkElement().getId(), k -> new HashSet<>()).addAll(flowCnec.getMonitoredSides())
        );

        List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = new ArrayList<>();
        networkElementsAndSides.forEach((neId, sides) -> sensitivityFunctions.addAll(cnecToSensitivityFunctions(network, neId, sides)));
        return sensitivityFunctions;
    }

    private List<Pair<String, SensitivityFunctionType>> cnecToSensitivityFunctions(Network network, String networkElementId, Set<TwoSides> sides) {
        Identifiable<?> networkIdentifiable = network.getIdentifiable(networkElementId);
        if (networkIdentifiable instanceof Branch || networkIdentifiable instanceof DanglingLine) {
            return getSensitivityFunctionTypes(sides).stream().map(functionType -> Pair.of(networkElementId, functionType)).toList();
        } else {
            throw new OpenRaoException("Unable to create sensitivity function for " + networkElementId);
        }
    }

    private Set<SensitivityFunctionType> getSensitivityFunctionTypes(Set<TwoSides> sides) {
        Set<SensitivityFunctionType> sensitivityFunctionTypes = new HashSet<>();
        if (factorsInMegawatt && sides.contains(TwoSides.ONE)) {
            sensitivityFunctionTypes.add(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1);
        }
        if (factorsInMegawatt && sides.contains(TwoSides.TWO)) {
            sensitivityFunctionTypes.add(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2);
        }
        if (factorsInAmpere && sides.contains(TwoSides.ONE)) {
            sensitivityFunctionTypes.add(SensitivityFunctionType.BRANCH_CURRENT_1);
        }
        if (factorsInAmpere && sides.contains(TwoSides.TWO)) {
            sensitivityFunctionTypes.add(SensitivityFunctionType.BRANCH_CURRENT_2);
        }
        return sensitivityFunctionTypes;
    }

}
