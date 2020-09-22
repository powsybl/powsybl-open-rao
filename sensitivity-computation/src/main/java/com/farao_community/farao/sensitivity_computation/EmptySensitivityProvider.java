/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityVariable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class EmptySensitivityProvider extends RangeActionSensitivityProvider {
    public EmptySensitivityProvider() {
        super();
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
