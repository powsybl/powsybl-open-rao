/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityValue;

import java.util.Set;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoData {
    private Crac crac;
    private Network network;
    private SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;

    public LinearRaoData(Crac crac, Network network, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        this.crac = crac;
        this.network = network;
        this.systematicSensitivityAnalysisResult = systematicSensitivityAnalysisResult;
    }

    public double getSensitivity(Cnec cnec, RangeAction rangeAction) {
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        double sensitivity = 0;
        for (NetworkElement networkElement : networkElements) {
            SensitivityValue value = systematicSensitivityAnalysisResult.getStateSensiMap().get(crac.getPreventiveState()).getSensitivityValues().stream()
                .filter(sensitivityValue -> sensitivityValue.getFactor().getVariable().getId().equals(networkElement.getId()))
                .filter(sensitivityValue -> sensitivityValue.getFactor().getFunction().getId().equals(cnec.getNetworkElement().getId()))
                .findFirst()
                .orElseThrow(FaraoException::new);
            sensitivity += value.getValue();
        }
        return sensitivity;
    }

    public double getReferenceFlow(Cnec cnec) {
        double margin = systematicSensitivityAnalysisResult.getCnecMarginMap().get(cnec);
        double maxFlow = systematicSensitivityAnalysisResult.getCnecMaxThresholdMap().get(cnec);
        return maxFlow - margin;
    }

    public double getTargetValue(RangeAction rangeAction) {
        //todo
        return 0.0;
    }

    public Crac getCrac() {
        return this.crac;
    }

    public Network getNetwork() {
        return this.network;
    }
}
