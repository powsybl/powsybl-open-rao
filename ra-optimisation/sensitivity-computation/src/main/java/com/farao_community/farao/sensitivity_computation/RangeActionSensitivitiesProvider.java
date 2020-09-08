/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
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
public class RangeActionSensitivitiesProvider extends AbstractSimpleSensitivityProvider {
    private final Crac crac;
    private List<RangeAction> rangeActions;

    RangeActionSensitivitiesProvider(Crac crac) {
        super();
        this.crac = Objects.requireNonNull(crac);
        rangeActions = new ArrayList<>();
    }

    void addSensitivityFactors(Set<RangeAction> rangeActions, Set<Cnec> cnecs) {
        this.rangeActions.addAll(rangeActions);
        super.addCnecs(cnecs);
    }

    private List<SensitivityVariable> rangeActionToSensitivityVariables(Network network, RangeAction rangeAction) {
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        return networkElements.stream()
            .map(el -> networkElementToSensitivityVariable(network, el))
            .collect(Collectors.toList());
    }

    private SensitivityVariable networkElementToSensitivityVariable(Network network, NetworkElement networkElement) {
        String elementId = networkElement.getId();
        Identifiable networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof TwoWindingsTransformer) {
            return new PhaseTapChangerAngle(elementId, elementId, elementId);
        } else {
            throw new FaraoException("Unable to create sensitivity variable for " + elementId);
        }
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
        throw new FaraoException(String.format("Unable to create sensitivity factors for CRAC '%s'. Did not find any varying element in network '%s'.", crac.getId(), network.getId()));
    }

    private List<SensitivityFunction> cnecToSensitivityFunctions(Network network, NetworkElement networkElement) {
        String id = networkElement.getId();
        String name = networkElement.getName();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(id);
        if (networkIdentifiable instanceof Branch) {

            /*
             todo : do not create the BranchIntensity here if the sensi is run in DC. Otherwise PowSyBl
              returns tons of ERROR logs in DC mode as it cannot handle those sensitivity functions in DC mode.
              (it is not possible to check this for now as the PowSyBl API does not allow yet to retrieve
              the AC/DC information of the sensi).
             */

            return Arrays.asList(new BranchFlow(id, name, id), new BranchIntensity(id, name, id));
        } else {
            throw new FaraoException("Unable to create sensitivity function for " + id);
        }
    }

    private SensitivityFactor sensitivityFactorMapping(SensitivityFunction function, SensitivityVariable variable) {
        if (function instanceof BranchFlow) {
            if (variable instanceof PhaseTapChangerAngle) {
                return new BranchFlowPerPSTAngle((BranchFlow) function, (PhaseTapChangerAngle) variable);
            } else if (variable instanceof InjectionIncrease) {
                return new BranchFlowPerInjectionIncrease((BranchFlow) function, (InjectionIncrease) variable);
            } else {
                throw new FaraoException("Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName());
            }
        } else if (function instanceof BranchIntensity) {
            if (variable instanceof PhaseTapChangerAngle) {
                return new BranchIntensityPerPSTAngle((BranchIntensity) function, (PhaseTapChangerAngle) variable);
            } else {
                throw new FaraoException("Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName());
            }
        } else {
            throw new FaraoException("Unable to create sensitivity factor for function of type " + function.getClass().getTypeName() + " and variable of type " + variable.getClass().getTypeName());
        }
    }

    @Override
    public List<SensitivityFactor> getFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        List<SensitivityVariable> sensitivityVariables = rangeActions.stream()
            .map(ra -> rangeActionToSensitivityVariables(network, ra))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            sensitivityVariables.add(defaultSensitivityVariable(network));
        }

        Set<NetworkElement> networkElements = new HashSet<>();
        crac.getCnecs().forEach(cnec -> networkElements.add(cnec.getNetworkElement()));
        List<SensitivityFunction> sensitivityFunctions = networkElements.stream()
            .map(networkElement -> cnecToSensitivityFunctions(network, networkElement))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        sensitivityFunctions.forEach(fun -> sensitivityVariables.forEach(var -> factors.add(sensitivityFactorMapping(fun, var))));
        return factors;
    }
}
