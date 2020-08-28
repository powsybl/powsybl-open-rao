/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisResult.class);

    private final boolean isSuccess;
    private final SensitivityComputationResults results;

    public SystematicSensitivityAnalysisResult(SensitivityComputationResults results) {
        if (results == null) {
            this.isSuccess = false;
            this.results = null;
            return;
        }
        this.isSuccess = results.isOk();
        this.results = results;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public double getReferenceFlow(Cnec cnec) {

        Optional<SensitivityValue> optSensiMW = getSensitivityValues(cnec).filter(v -> v.getFactor().getFunction() instanceof BranchFlow).findFirst();

        if (!optSensiMW.isPresent()) {
            return Double.NaN;
        } else {

            double referenceFlow = optSensiMW.get().getFunctionReference();

            // TODO: remove this fix when reference function patched in case NaN and no divergence
            if (Double.isNaN(referenceFlow) && !Double.isNaN(optSensiMW.get().getValue())) {
                referenceFlow = 0.0;
            }

            return referenceFlow;
        }
    }

    public double getReferenceIntensity(Cnec cnec) {

        Optional<SensitivityValue> optSensiA = getSensitivityValues(cnec).filter(v -> v.getFactor().getFunction() instanceof BranchIntensity).findFirst();
        double flowInMW = getReferenceFlow(cnec);

        if (!optSensiA.isPresent() || Double.isNaN(flowInMW)) {
            return Double.NaN;
        } else {

            double referenceIntensity = flowInMW > 0 ? optSensiA.get().getFunctionReference() : -optSensiA.get().getFunctionReference();

            // TODO: remove this fix when reference function patched in case NaN and no divergence
            if (Double.isNaN(referenceIntensity) && !Double.isNaN(optSensiA.get().getValue())) {
                referenceIntensity = 0.0;
            }

            return referenceIntensity;
        }
    }

    public double getSensitivityOnFlow(RangeAction rangeAction, Cnec cnec) {
        return getSensitivityValues(cnec, rangeAction.getNetworkElements()).
            filter(v -> v.getFactor().getFunction() instanceof BranchFlow).
            mapToDouble(SensitivityValue::getValue).sum();
    }

    public double getSensitivityOnIntensity(RangeAction rangeAction, Cnec cnec) {
        throw new NotImplementedException("unecessary");
    }

    private Stream<SensitivityValue> getSensitivityValues(Cnec cnec, Set<NetworkElement> remedialActionElements) {
        List<String> raElementIds = remedialActionElements.stream().map(NetworkElement::getId).collect(Collectors.toList());
        return getSensitivityValues(cnec).filter(v -> raElementIds.contains(v.getFactor().getVariable().getId()));
    }

    private Stream<SensitivityValue> getSensitivityValues(Cnec cnec) {
        Collection<SensitivityValue> sensitivityValues = Objects.requireNonNull(getSensitivityValues(cnec.getState()));
        return sensitivityValues.stream().filter(v -> v.getFactor().getFunction().getId().equals(cnec.getNetworkElement().getId()));
    }

    private Collection<SensitivityValue> getSensitivityValues(State state) {
        if (state.getContingency().isPresent()) {
            return results.getSensitivityValuesContingencies().get(state.getContingency().get().getId());
        } else {
            return results.getSensitivityValues();
        }
    }
}
