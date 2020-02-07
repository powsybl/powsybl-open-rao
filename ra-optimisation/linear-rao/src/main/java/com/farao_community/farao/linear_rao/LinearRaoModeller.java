/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoModeller {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRaoModeller.class);

    private LinearRaoProblem linearRaoProblem;
    private LinearRaoData linearRaoData;
    private List<AbstractProblemFiller> fillerList;
    private List<AbstractPostProcessor> postProcessorList;
    private RaoParameters raoParameters;

    public LinearRaoModeller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, List<AbstractProblemFiller> fillerList, List<AbstractPostProcessor> postProcessorList, RaoParameters raoParameters) {
        this.linearRaoProblem = linearRaoProblem;
        this.linearRaoData = linearRaoData;
        this.fillerList = fillerList;
        this.postProcessorList = postProcessorList;
        this.raoParameters = raoParameters;
    }

    public void buildProblem() {
        fillerList.forEach(AbstractProblemFiller::fill);
    }

    public void updateProblem(LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
        fillerList.forEach(filler -> filler.update(linearRaoProblem, linearRaoData));
    }

    public RaoComputationResult solve() {
        Enum solverResultStatus = linearRaoProblem.solve();
        RaoComputationResult raoComputationResult;
        if (solverResultStatus.name().equals("OPTIMAL")) {
            RaoComputationResult.Status status = RaoComputationResult.Status.SUCCESS;
            raoComputationResult = new RaoComputationResult(status);
            postProcessorList.forEach(postProcessor -> postProcessor.process(linearRaoProblem, linearRaoData, raoComputationResult));
        } else {
            RaoComputationResult.Status status = RaoComputationResult.Status.FAILURE;
            raoComputationResult = new RaoComputationResult(status);
            LOGGER.warn(String.format("Linear rao computation failed: MPSolver status is %s", solverResultStatus.name()));
        }
        return raoComputationResult;
    }
}
