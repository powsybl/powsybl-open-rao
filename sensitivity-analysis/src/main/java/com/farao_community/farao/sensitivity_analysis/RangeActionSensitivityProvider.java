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
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.InjectionRangeActionSensiHandler;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class RangeActionSensitivityProvider extends LoadflowProvider {
    private final Set<RangeAction<?>> rangeActions;
    private List<SensitivityVariableSet> glsks;

    RangeActionSensitivityProvider(Set<RangeAction<?>> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
        this.rangeActions = rangeActions;
        glsks = new ArrayList<>();
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        Map<String, SensitivityVariableType> sensitivityVariables = new HashMap<>();
        Set<String> glskIds = new HashSet<>();
        for (RangeAction<?> ra : rangeActions) {
            if (ra instanceof PstRangeAction) {
                sensitivityVariables.put(((PstRangeAction) ra).getNetworkElement().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
            } else if (ra instanceof HvdcRangeAction) {
                sensitivityVariables.put(((HvdcRangeAction) ra).getNetworkElement().getId(), SensitivityVariableType.HVDC_LINE_ACTIVE_POWER);
            } else if (ra instanceof InjectionRangeAction) {
                createPositiveAndNegativeGlsks((InjectionRangeAction) ra, sensitivityVariables, glskIds);
            } else {
                throw new SensitivityAnalysisException(String.format("Range action type of %s not implemented yet", ra.getId()));
            }
        }

        // Case no RangeAction is provided, we still want to get reference flows
        if (sensitivityVariables.isEmpty()) {
            addDefaultSensitivityVariable(network, sensitivityVariables);
        }

        List<Pair<String, SensitivityFunctionType>> sensitivityFunctions = getSensitivityFunctions(network, null);

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);
        sensitivityFunctions.forEach(function -> {
            sensitivityVariables.entrySet().forEach(variable -> {
                factors.add(new SensitivityFactor(function.getValue(), function.getKey(), variable.getValue(), variable.getKey(),
                    glskIds.contains(variable.getKey()), preContingencyContext));
            });
        });
        return factors;
    }

    private void createPositiveAndNegativeGlsks(InjectionRangeAction rangeAction, Map<String, SensitivityVariableType> sensitivityVariables, Set<String> glskIds) {
        InjectionRangeActionSensiHandler injectionRangeActionSensiHandler = new InjectionRangeActionSensiHandler(rangeAction);
        Map<String, Float> positiveGlskMap = injectionRangeActionSensiHandler.getPositiveGlskMap();
        Map<String, Float> negativeGlskMap = injectionRangeActionSensiHandler.getNegativeGlskMap();

        if (!positiveGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> positiveGlsk = injectionRangeActionSensiHandler.rescaleGlskMap(positiveGlskMap);
            sensitivityVariables.put(injectionRangeActionSensiHandler.getPositiveGlskMapId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.add(new SensitivityVariableSet(injectionRangeActionSensiHandler.getPositiveGlskMapId(), positiveGlsk));
            glskIds.add(injectionRangeActionSensiHandler.getPositiveGlskMapId());
        }

        if (!negativeGlskMap.isEmpty()) {
            List<WeightedSensitivityVariable> negativeGlsk = injectionRangeActionSensiHandler.rescaleGlskMap(negativeGlskMap);
            sensitivityVariables.put(injectionRangeActionSensiHandler.getNegativeGlskMapId(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
            glsks.add(new SensitivityVariableSet(injectionRangeActionSensiHandler.getNegativeGlskMapId(), negativeGlsk));
            glskIds.add(injectionRangeActionSensiHandler.getNegativeGlskMapId());
        }
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            Map<String, SensitivityVariableType> sensitivityVariables = new HashMap<>();
            Set<String> glskIds = new HashSet<>();
            for (RangeAction<?> ra : rangeActions) {
                if (ra instanceof PstRangeAction) {
                    sensitivityVariables.put(((PstRangeAction) ra).getNetworkElement().getId(), SensitivityVariableType.TRANSFORMER_PHASE);
                } else if (ra instanceof HvdcRangeAction) {
                    sensitivityVariables.put(((HvdcRangeAction) ra).getNetworkElement().getId(), SensitivityVariableType.HVDC_LINE_ACTIVE_POWER);
                } else if (ra instanceof InjectionRangeAction) {
                    createPositiveAndNegativeGlsks((InjectionRangeAction) ra, sensitivityVariables, glskIds);
                } else {
                    throw new SensitivityAnalysisException(String.format("Range action type of %s not implemented yet", ra.getId()));
                }
            }

            // Case no RangeAction is provided, we still want to get reference flows
            if (sensitivityVariables.isEmpty()) {
                addDefaultSensitivityVariable(network, sensitivityVariables);
            }

            List<Pair<String, SensitivityFunctionType> > sensitivityFunctions = getSensitivityFunctions(network, contingencyId);

            //According to ContingencyContext doc, contingencyId should be null for preContingency context
            ContingencyContext contingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            sensitivityFunctions.forEach(function -> {
                sensitivityVariables.entrySet().forEach(variable -> {
                    factors.add(new SensitivityFactor(function.getValue(), function.getKey(), variable.getValue(), variable.getKey(),
                        glskIds.contains(variable.getKey()), contingencyContext));
                });
            });
        }
        return factors;
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return glsks;
    }
}
