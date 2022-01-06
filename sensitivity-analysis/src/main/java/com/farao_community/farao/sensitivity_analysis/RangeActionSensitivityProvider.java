/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.HvdcSetpointIncrease;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;

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

        if (rangeAction instanceof PstRangeAction) {
            return Collections.singletonList(pstRangeActionToSensitivityVariable(network, (PstRangeAction) rangeAction));
        } else if (rangeAction instanceof HvdcRangeAction) {
            return Collections.singletonList(hvdcRangeActionToSensitivityVariable(network, (HvdcRangeAction) rangeAction));
        } else if (rangeAction instanceof InjectionRangeAction) {
            // todo: if InjectionRangeAction only create one variable, make this method return a SensitivityVariable
            //      and not a List<SensitivityVariable>
            return injectionRangeActionToSensitivityVariable(network, (InjectionRangeAction) rangeAction);
        } else {
            throw new SensitivityAnalysisException(String.format("RangeAction implementation %s not handled by sensitivity analysis", rangeAction.getClass()));
        }
    }

    private SensitivityVariable pstRangeActionToSensitivityVariable(Network network, PstRangeAction pstRangeAction) {
        String elementId = pstRangeAction.getNetworkElement().getId();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof TwoWindingsTransformer) {
            return new PhaseTapChangerAngle(elementId, elementId, elementId);
        } else {
            throw new SensitivityAnalysisException(String.format("Unable to create sensitivity variable for PstRangeAction %s, on element %s", pstRangeAction.getId(), elementId));
        }
    }

    private SensitivityVariable hvdcRangeActionToSensitivityVariable(Network network, HvdcRangeAction hvdcRangeAction) {
        String elementId = hvdcRangeAction.getNetworkElement().getId();
        Identifiable<?> networkIdentifiable = network.getIdentifiable(elementId);
        if (networkIdentifiable instanceof HvdcLine) {
            return new HvdcSetpointIncrease(elementId, elementId, elementId);
        } else {
            throw new SensitivityAnalysisException(String.format("Unable to create sensitivity variable for HvdcRangeAction %s, on element %s", hvdcRangeAction.getId(), elementId));
        }
    }

    private List<SensitivityVariable> injectionRangeActionToSensitivityVariable(Network network, InjectionRangeAction injectionRangeAction) {

        // todo: ensure that it works, not sure it is that easy, notably not sure that GLSK handle negative
        //  values in Hades. We might have to build two LinearGlsk, one for positive generator/negative load,
        //  and one for negative generator/positive load

        // + care: BranchIntensityPerLinearGLsk do not exist in Hades. Can be tested for now in MEGAWATT, but
        //  necessary devs will have to be made in powsybl-core-rte to make it work in AMPERE

        // + care-update: not sure it is absolutely necessary to make this dev, we just need the sensi in MEGAWATT,
        // only the reference flows can be useful in AMPERE

        List<SensitivityVariable> sensitivityVariables = new ArrayList<>();

        // GLSK which concatenate the change of all injections

        Map<String, Float> glskMap = new HashMap<>();
        Map<String, Float> glskMap1 = new HashMap<>();
        Map<String, Float> glskMap2 = new HashMap<>();
        Map<String, Float> glskMap1abs = new HashMap<>();
        Map<String, Float> glskMap2abs = new HashMap<>();
        Map<String, Float> glskMap1demi = new HashMap<>();

        int i = 1;
        for (Map.Entry<NetworkElement, Double> entry : injectionRangeAction.getInjectionDistributionKeys().entrySet()) {
            NetworkElement key = entry.getKey();
            Double value = entry.getValue();
            Identifiable<?> networkIdentifiable = network.getIdentifiable(key.getId());
            if (networkIdentifiable instanceof Load || networkIdentifiable instanceof Generator) {
                glskMap.put(key.getId(), value.floatValue());
                if (i == 1) {
                    glskMap1.put(key.getId(), value.floatValue());
                    glskMap1abs.put(key.getId(), Math.abs(value.floatValue()));
                    glskMap1demi.put(key.getId(), Math.abs(value.floatValue()) / 2);

                } else if (i == 2) {
                    glskMap2.put(key.getId(), value.floatValue());
                    glskMap2abs.put(key.getId(), Math.abs(value.floatValue()));
                }
                i++;
            } else {
                throw new SensitivityAnalysisException(String.format("Unable to create sensitivity variable for InjectionRangeAction %s, on element %s", injectionRangeAction.getId(), key.getId()));
            }
        }

        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId(), injectionRangeAction.getId(), glskMap));

        // for test: glsk on first generator
        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId() + "-gen1", injectionRangeAction.getId() + "-gen1", glskMap1));
        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId() + "-gen1abs", injectionRangeAction.getId() + "-gen1abs", glskMap1abs));
        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId() + "-gen1absdemi", injectionRangeAction.getId() + "-gen1absdemi", glskMap1demi));

        // for test: glsk on second generator
        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId() + "-gen2", injectionRangeAction.getId() + "-gen2", glskMap2));
        sensitivityVariables.add(new LinearGlsk(injectionRangeAction.getId() + "-gen2abs", injectionRangeAction.getId() + "-gen2abs", glskMap2abs));

        return sensitivityVariables;
    }
}
