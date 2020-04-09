/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.linear_rao.optimisation.*;
import com.farao_community.farao.linear_rao.optimisation.fillers.CoreProblemFiller;
import com.farao_community.farao.linear_rao.optimisation.fillers.MaxLoopFlowFiller;
import com.farao_community.farao.linear_rao.optimisation.fillers.MaxMinMarginFiller;
import com.farao_community.farao.linear_rao.optimisation.post_processors.RaoResultPostProcessor;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.google.ortools.linearsolver.MPSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearOptimisationEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinearOptimisationEngine.class);

    private boolean initialised;

    private LinearRaoProblem linearRaoProblem;

    private LinearRaoData linearRaoData;

    private List<AbstractProblemFiller> fillerList;

    private List<AbstractPostProcessor> postProcessorList;

    LinearOptimisationEngine(RaoParameters raoParameters) {

        this.initialised = false;
        this.linearRaoProblem = new LinearRaoProblem();

        // TODO : load the filler list from the config file and make sure they are ordered properly
        fillerList = getFillerList(raoParameters);
        postProcessorList = getPostProcessorList();
    }

    OptimizedSituation run(AbstractSituation situationIn) {

        // update data
        // todo : remove linear data
        this.linearRaoData = new LinearRaoData(situationIn.getCrac(), situationIn.getNetwork(), situationIn.getSystematicSensitivityAnalysisResult());

        // prepare optimisation problem
        if (!initialised) {
            buildProblem();
            initialised = true;
        } else {
            updateProblem();
        }

        // solve optimisation problem
        solveProblem();

        OptimizedSituation situationOut = new OptimizedSituation(linearRaoData.getNetwork(), linearRaoData.getCrac());

        // todo : do not create a RaoResult anymore
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        postProcessorList.forEach(postProcessor -> postProcessor.process(linearRaoProblem, linearRaoData, raoResult, situationOut.getResultVariant()));

        // todo : check if it is still necessary to have two implementations of the AbstractSituation
        return situationOut;
    }

    private void buildProblem() {
        try {
            fillerList.forEach(AbstractProblemFiller::fill);
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when building the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void updateProblem() {
        try {
            fillerList.forEach(AbstractProblemFiller::fill);
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when updating the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void solveProblem() {
        try {

            MPSolver.ResultStatus solverResultStatus = linearRaoProblem.solve();

            if (solverResultStatus != MPSolver.ResultStatus.OPTIMAL) {
                String errorMessage = String.format("Solving of the linear problem failed failed with MPSolver status %s", solverResultStatus.toString());
                LOGGER.error(errorMessage);
                throw new LinearOptimisationException(errorMessage);
            }

        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private List<AbstractProblemFiller> getFillerList(RaoParameters raoParameters) {
        fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller(linearRaoProblem, linearRaoData));
        fillerList.add(new MaxMinMarginFiller(linearRaoProblem, linearRaoData));
        if (raoParameters.isRaoWithLoopFlowLimitation() && !Objects.isNull(linearRaoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            fillerList.add(new MaxLoopFlowFiller(linearRaoProblem, linearRaoData));
        }
        return fillerList;
    }

    private List<AbstractPostProcessor> getPostProcessorList() {
        postProcessorList = new ArrayList<>();
        postProcessorList.add(new RaoResultPostProcessor());
        return postProcessorList;
    }

}
