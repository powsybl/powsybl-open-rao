/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.RangeActionSensiHandler;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RangeActionSensitivityProvider extends LoadflowProvider {
    private final Set<RangeAction<?>> rangeActions;

    RangeActionSensitivityProvider(Set<RangeAction<?>> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
        this.rangeActions = rangeActions;
    }

    @Override
    public List<SensitivityFactor> getCommonFactors(Network network) {
        return new ArrayList<>();
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        List<SensitivityVariable> sensitivityVariables = rangeActions.stream()
            .map(ra -> rangeActionToSensitivityVariables(network, ra))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            sensitivityVariables.add(defaultSensitivityVariable(network));
        }

        getSensitivityFunctions(network, null).forEach(fun -> sensitivityVariables.forEach(var -> factors.add(sensitivityFactorMapping(fun, var))));
        return factors;
    }

    @Override
    public List<SensitivityFactor> getAdditionalFactors(Network network, String contingencyId) {
        List<SensitivityFactor> factors = new ArrayList<>();
        List<SensitivityVariable> sensitivityVariables = rangeActions.stream()
            .map(ra -> rangeActionToSensitivityVariables(network, ra))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            sensitivityVariables.add(defaultSensitivityVariable(network));
        }

        getSensitivityFunctions(network, contingencyId).forEach(fun -> sensitivityVariables.forEach(var -> factors.add(sensitivityFactorMapping(fun, var))));
        return factors;
    }

    private List<SensitivityVariable> rangeActionToSensitivityVariables(Network network, RangeAction<?> rangeAction) {
        RangeActionSensiHandler sensiHandler = RangeActionSensiHandler.get(rangeAction);
        sensiHandler.checkConsistency(network);
        return sensiHandler.rangeActionToSensitivityVariable();
    }
}
