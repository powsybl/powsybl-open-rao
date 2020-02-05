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
import com.farao_community.farao.linear_rao.post_processors.PstTapPostProcessor;
import com.farao_community.farao.linear_rao.post_processors.RaoResultPostProcessor;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoOptimizer {

    private Crac crac;
    private Network network;
    private ComputationManager computationManager;
    private LinearRaoModeller linearRaoModeller;

    public LinearRaoOptimizer(Crac crac,
                              Network network,
                              SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult,
                              ComputationManager computationManager,
                              RaoParameters raoParameters) {
        this.crac = crac;
        this.network = network;
        this.computationManager = computationManager;

        LinearRaoProblem linearRaoProblem = new LinearRaoProblem();

        LinearRaoData linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);

        List<AbstractProblemFiller> fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller(linearRaoProblem, linearRaoData));
        fillerList.add(new PositiveMinMarginFiller(linearRaoProblem, linearRaoData));

        List <AbstractPostProcessor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new PstTapPostProcessor());
        postProcessorList.add(new RaoResultPostProcessor());

        linearRaoModeller = new LinearRaoModeller(linearRaoProblem, linearRaoData, fillerList, postProcessorList, raoParameters);
    }

    public void update(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        LinearRaoData linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);
        linearRaoModeller.updateProblem(linearRaoData);
    }

    public RaoComputationResult run() {
        linearRaoModeller.buildProblem();
        return linearRaoModeller.solve();
    }
}
