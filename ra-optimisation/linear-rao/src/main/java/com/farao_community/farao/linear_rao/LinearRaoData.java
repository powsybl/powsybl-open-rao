/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;

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

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setSystematicSensitivityAnalysisResult(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        this.systematicSensitivityAnalysisResult = systematicSensitivityAnalysisResult;
    }

    public double getReferenceFlow(Cnec cnec) {
        return systematicSensitivityAnalysisResult.getFlow(cnec).orElseThrow(FaraoException::new);
    }

    public Crac getCrac() {
        return this.crac;
    }

    public Network getNetwork() {
        return this.network;
    }

    public double getSensitivity(Cnec cnec, RangeAction rangeAction) {
        return systematicSensitivityAnalysisResult.getSensitivity(cnec, cnec.getState(), rangeAction).orElse(Double.NaN);
    }
}
