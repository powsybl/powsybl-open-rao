/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RangeActionSensitivityProvider extends LoadflowProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RangeActionSensitivityProvider.class);
    private List<RangeAction> rangeActions;

    public RangeActionSensitivityProvider() {
        super();
        rangeActions = new ArrayList<>();
    }

    public void addSensitivityFactors(Set<RangeAction> rangeActions, Set<Cnec> cnecs) {
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
            throw new SensitivityComputationException("Unable to create sensitivity variable for " + elementId);
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
            LOGGER.warn("No range action provided. You may wish to use  an EmptySensitivityProvider if this is the intended behaviour.");
            sensitivityVariables.add(defaultSensitivityVariable(network));
        }

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
