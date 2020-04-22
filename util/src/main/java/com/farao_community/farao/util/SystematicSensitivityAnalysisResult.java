/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisResult {
    private Map<State, SensitivityComputationResults> stateSensiMap;
    private Map<Cnec, Double> cnecFlowMap;
    private Map<Cnec, Double> cnecIntensityMap;

    public SystematicSensitivityAnalysisResult(Map<State, SensitivityComputationResults> stateSensiMap, Map<Cnec, Double> cnecFlowMap, Map<Cnec, Double> cnecIntensityMap) {
        this.stateSensiMap = stateSensiMap;
        this.cnecFlowMap = cnecFlowMap;
        this.cnecIntensityMap = cnecIntensityMap;
    }

    public Optional<Double> getFlow(Cnec cnec) {
        return Optional.ofNullable(cnecFlowMap.get(cnec));
    }

    public Optional<Double> getIntensity(Cnec cnec) {
        return Optional.ofNullable(cnecIntensityMap.get(cnec));
    }

    public Optional<Double> getSensitivity(Cnec cnec, RangeAction rangeAction) {
        State state = cnec.getState();
        if (!stateSensiMap.containsKey(state) || Objects.isNull(stateSensiMap.get(state))) {
            return Optional.empty();
        }
        return Optional.of(getSensitivityValues(cnec, rangeAction, stateSensiMap.get(state)).stream()
                .mapToDouble(SensitivityValue::getValue)
                .sum());
    }

    private List<SensitivityValue> getSensitivityValues(Cnec cnec, RangeAction rangeAction, SensitivityComputationResults stateResults) {
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        return networkElements.stream().map(netEl -> networkElementToSensitivityValue(cnec, netEl, stateResults)).collect(Collectors.toList());
    }

    private SensitivityValue networkElementToSensitivityValue(Cnec cnec, NetworkElement rangeElement, SensitivityComputationResults stateResults) {
        List<SensitivityValue> sensitivityValues;
        sensitivityValues = stateResults.getSensitivityValues().stream()
                .filter(sensitivityValue -> sensitivityValue.getFactor().getVariable().getId().equals(rangeElement.getId()))
                .filter(sensitivityValue -> sensitivityValue.getFactor().getFunction().getId().equals(cnec.getId()))
                .collect(Collectors.toList());

        if (sensitivityValues.size() > 1) {
            throw new FaraoException(String.format("More than one sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), rangeElement.getId()));
        }
        if (sensitivityValues.isEmpty()) {
            throw new FaraoException(String.format("No sensitivity value found for couple Cnec %s - RA %s", cnec.getId(), rangeElement.getId()));
        }
        return sensitivityValues.get(0);
    }

    public boolean anyStateDiverged() {
        return stateSensiMap.containsValue(null) || cnecFlowMap.isEmpty();
    }
}
