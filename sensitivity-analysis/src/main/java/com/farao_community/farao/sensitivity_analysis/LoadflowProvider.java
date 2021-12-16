/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
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

        List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = getSensitivityFunctions(network, null);

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);
        sensitivityFunctions.forEach(function -> {
            sensitivityVariables.entrySet().forEach(variable -> {
                factors.add(new SensitivityFactor(function.getValue(), function.getKey(), variable.getValue(), variable.getKey(), false, preContingencyContext));
            });
        });
        return factors;
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            Map<String, SensitivityVariableType> sensitivityVariables = new LinkedHashMap<>();
            addDefaultSensitivityVariable(network, sensitivityVariables);

            List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = getSensitivityFunctions(network, contingencyId);

            ContingencyContext contingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            sensitivityFunctions.forEach(function -> {
                sensitivityVariables.entrySet().forEach(variable -> {
                    factors.add(new SensitivityFactor(function.getValue(), function.getKey(), variable.getValue(), variable.getKey(), false, contingencyContext));
                });
            });
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

    private boolean willBeKeptInSensi(Generator gen) {
        return gen.getTerminal().isConnected() && gen.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent();
    }

    void addDefaultSensitivityVariable(Network network, Map<String, SensitivityVariableType> sensitivityVariables) {
        // First try to get a PST angle
        Optional<TwoWindingsTransformer> optionalPst = network.getTwoWindingsTransformerStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalPst.isPresent()) {
            sensitivityVariables.put(optionalPst.get().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
            return;
        }

        // If no one found, pick a Generator injection
        Optional<Generator> optionalGen = network.getGeneratorStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalGen.isPresent()) {
            sensitivityVariables.put(optionalGen.get().getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            return;
        }
        throw new FaraoException(String.format("Unable to create sensitivity factors. Did not find any varying element in network '%s'.", network.getId()));
    }

    List<Pair<String, SensitivityFunctionType> > getSensitivityFunctions(Network network, String contingencyId) {
        Set<NetworkElement> networkElements;
        if (Objects.isNull(contingencyId)) {
            networkElements = cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isEmpty())
                .map(Cnec::getNetworkElement)
                .collect(Collectors.toSet());
        } else {
            networkElements = cnecs.stream()
                .filter(cnec -> cnec.getState().getContingency().isPresent() && cnec.getState().getContingency().get().getId().equals(contingencyId))
                .map(Cnec::getNetworkElement)
                .collect(Collectors.toSet());
        }
        List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = new ArrayList<>();
        networkElements.forEach(networkElement -> sensitivityFunctions.addAll(cnecToSensitivityFunctions(network, networkElement)));
        return sensitivityFunctions;
    }

    private List<Pair<String, SensitivityFunctionType>> cnecToSensitivityFunctions(Network network, NetworkElement networkElement) {
        String id = networkElement.getId();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(id);

        if (networkIdentifiable instanceof Branch) {
            List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = new ArrayList<>();
            if (factorsInMegawatt) {
                sensitivityFunctions.add(Pair.of(id, SensitivityFunctionType.BRANCH_ACTIVE_POWER));
            }
            if (factorsInAmpere) {
                sensitivityFunctions.add(Pair.of(id, SensitivityFunctionType.BRANCH_CURRENT));
            }
            return sensitivityFunctions;
        } else {
            throw new FaraoException("Unable to create sensitivity function for " + id);
        }
    }

}
