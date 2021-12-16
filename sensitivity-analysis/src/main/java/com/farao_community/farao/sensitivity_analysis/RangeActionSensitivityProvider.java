/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RangeActionSensitivityProvider extends LoadflowProvider {
    private Set<RangeAction> rangeActions;

    RangeActionSensitivityProvider(Set<RangeAction> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
        this.rangeActions = rangeActions;
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        Map<String, SensitivityVariableType> sensitivityVariables = rangeActions.stream()
            .collect(Collectors.toMap(ra -> ra.getNetworkElements().iterator().next().getId(), ra -> {
                if (ra instanceof PstRangeAction) {
                    return SensitivityVariableType.TRANSFORMER_PHASE;
                } else if (ra instanceof HvdcRangeAction) {
                    return SensitivityVariableType.HVDC_LINE_ACTIVE_POWER;
                } else {
                    throw new SensitivityAnalysisException(String.format("Range action type of %s not implemented yet", ra.getId()));
                }
            }));

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            addDefaultSensitivityVariable(network, sensitivityVariables);
        }

        List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, null);

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
            Map<String, SensitivityVariableType> sensitivityVariables = rangeActions.stream()
                .collect(Collectors.toMap(ra -> ra.getNetworkElements().iterator().next().getId(), ra -> {
                    if (ra instanceof PstRangeAction) {
                        return SensitivityVariableType.TRANSFORMER_PHASE;
                    } else if (ra instanceof HvdcRangeAction) {
                        return SensitivityVariableType.HVDC_LINE_ACTIVE_POWER;
                    } else {
                        throw new SensitivityAnalysisException(String.format("Range action type of %s not implemented yet", ra.getId()));
                    }
                }));

            // Case no RangeAction is provided, we still want to get reference flows
            if (sensitivityVariables.isEmpty()) {
                addDefaultSensitivityVariable(network, sensitivityVariables);
            }

            List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = getSensitivityFunctions(network, contingencyId);

            //According to ContingencyContext doc, contingencyId should be null for preContingency context
            ContingencyContext contingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            sensitivityFunctions.forEach(function -> {
                sensitivityVariables.entrySet().forEach(variable -> {
                    factors.add(new SensitivityFactor(function.getValue(), function.getKey(), variable.getValue(), variable.getKey(), false, contingencyContext));
                });
            });
        }
        return factors;
    }
}
