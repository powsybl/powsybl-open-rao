/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.BranchIntensityPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class EmptySensitivityProvider extends RangeActionSensitivityProvider {
    private List<RangeAction> rangeActions;

    EmptySensitivityProvider() {
        super();
    }

    private boolean willBeKeptInSensi(TwoWindingsTransformer twoWindingsTransformer) {
        return twoWindingsTransformer.getTerminal1().isConnected() && twoWindingsTransformer.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getTerminal2().isConnected() && twoWindingsTransformer.getTerminal2().getBusBreakerView().getBus().isInMainSynchronousComponent() &&
            twoWindingsTransformer.getPhaseTapChanger() != null;
    }

    private boolean willBeKeptInSensi(Generator gen) {
        return gen.getTerminal().isConnected() && gen.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent();
    }

    private SensitivityVariable defaultSensitivityVariable(Network network) {
        // First try to get a PST angle
        Optional<TwoWindingsTransformer> optionalPst = network.getTwoWindingsTransformerStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalPst.isPresent()) {
            TwoWindingsTransformer pst = optionalPst.get();
            return new PhaseTapChangerAngle(pst.getId(), pst.getNameOrId(), pst.getId());
        }

        // If no one found, pick a Generator injection
        Optional<Generator> optionalGen = network.getGeneratorStream()
            .filter(this::willBeKeptInSensi)
            .findAny();

        if (optionalGen.isPresent()) {
            Generator gen = optionalGen.get();
            return new InjectionIncrease(gen.getId(), gen.getNameOrId(), gen.getId());
        }
        throw new FaraoException(String.format("Unable to create sensitivity factors. Did not find any varying element in network '%s'.", network.getId()));
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        List<SensitivityVariable> sensitivityVariables = new ArrayList<>();
        sensitivityVariables.add(defaultSensitivityVariable(network));

        Set<NetworkElement> networkElements = new HashSet<>();
        cnecs.forEach(cnec -> networkElements.add(cnec.getNetworkElement()));
        List<SensitivityFunction> sensitivityFunctions = networkElements.stream()
            .map(networkElement -> cnecToSensitivityFunctions(network, networkElement))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        sensitivityFunctions.forEach(fun -> sensitivityVariables.forEach(var -> factors.add(sensitivityFactorMapping(fun, var))));
        return factors;
    }
}
