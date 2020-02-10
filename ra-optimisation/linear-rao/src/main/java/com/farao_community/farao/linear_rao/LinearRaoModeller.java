/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.linear_rao.fillers.CoreProblemFiller;
import com.farao_community.farao.linear_rao.fillers.PositiveMinMarginFiller;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoModeller {
    private LinearRaoProblem linearRaoProblem;
    private Crac crac;
    private Network network;
    private LinearRaoData linearRaoData;
    private List<AbstractProblemFiller> fillerList;

    public LinearRaoModeller(Crac crac,
                             Network network,
                             SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult,
                             LinearRaoProblem linearRaoProblem) {
        this.crac = crac;
        this.network = network;
        this.linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);
        this.linearRaoProblem = linearRaoProblem;

        fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller(linearRaoProblem, linearRaoData));
        fillerList.add(new PositiveMinMarginFiller(linearRaoProblem, linearRaoData));
    }

    public LinearRaoProblem buildProblem() {
        fillerList.forEach(AbstractProblemFiller::fill);
        return linearRaoProblem;
    }

    LinearRaoData getData() {
        return linearRaoData;
    }

    public void updateProblem(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        this.linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);
        fillerList.forEach(filler -> filler.update(linearRaoProblem, linearRaoData));
    }

    public LinearRaoProblem getLinearRaoProblem() {
        return linearRaoProblem;
    }
}
