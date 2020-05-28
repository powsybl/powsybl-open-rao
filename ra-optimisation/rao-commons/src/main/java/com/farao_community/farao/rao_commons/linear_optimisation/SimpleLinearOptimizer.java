/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.rao_commons.ParametersException;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.core.parameters.LinearProblemParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.core.ProblemFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.core.fillers.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.*;

/**
 * A computation engine dedicated to the construction and solving of the linear
 * optimisation problem of the LinearRao.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleLinearOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLinearOptimizer.class);

    private RaoParameters raoParameters;

    /**
     * Linear optimisation problem, core object the LinearOptimisationEngine that
     * is solved each time the run method of this class is called.
     */
    private LinearProblem linearProblem;

    /**
     * Boolean indicating whether the linear problem has been already initialised
     * or not.
     */
    private boolean lpInitialised;

    /**
     * List of problem fillers used by the engine. Each filler is responsible for
     * the creation/update of one part of the optimisation problem (i.e. of some
     * variables and constraints of the optimisation problem.
     */
    private List<ProblemFiller> fillerList;

    /**
     * Constructor
     */
    public SimpleLinearOptimizer() {
        this.lpInitialised = false;
        this.raoParameters = new RaoParameters();
        LinearProblemParameters linearProblemParameters = new LinearProblemParameters();
        raoParameters.addExtension(LinearProblemParameters.class, linearProblemParameters);
        // TODO : load the filler list from the config file and make sure they are ordered properly
        this.fillerList = createFillerList(raoParameters);
    }

    public SimpleLinearOptimizer(RaoParameters raoParameters) {
        this.lpInitialised = false;
        this.raoParameters = checkRaoParameters(raoParameters);
        // TODO : load the filler list from the config file and make sure they are ordered properly
        this.fillerList = createFillerList(raoParameters);
    }

    private static RaoParameters checkRaoParameters(RaoParameters raoParameters) {
        if (raoParameters.getExtension(LinearProblemParameters.class) == null) {
            String msg = "Simple linear optimizer cannot be created because LinearProblemParameters extension is missing";
            LOGGER.error(msg);
            throw new ParametersException(msg);
        }
        return raoParameters;
    }

    private void checkDataConsistencyWithParameters(RaoData raoData) {
        if (raoParameters.isRaoWithLoopFlowLimitation()
            && Objects.isNull(raoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            String msg = format(
                "Simple linear optimizer cannot perform optimization with loop flows on CRAC %s because it does not have loop flow extension",
                raoData.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    private static void checkSensitivityValues(RaoData raoData) {
        if (!raoData.hasSensitivityValues()) {
            String msg = format(
                "Simple linear optimizer cannot perform optimization because no sensitivity computation has been performed on variant %s",
                raoData.getWorkingVariantId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    /**
     * The optimize method of the SimpleLinearOptimizer creates and solves a LinearProblem.
     * It updates the working RaoData variant with optimisation results in the CRAC
     * and apply the new range action set points on the network.
     *
     * @param raoData defines the data on which the creation of the optimisation problem
     *                    is based (i.e. a given Network situation with associated Crac
     *                    and sensitivities).
     *
     * @throws LinearOptimisationException if the method fails
     * @throws FaraoException if sensitivity computation have not been performed on working raoData variant
     * or if loop flow data are missing
     */
    public void optimize(RaoData raoData) {
        checkDataConsistencyWithParameters(raoData);
        checkSensitivityValues(raoData);

        // prepare optimisation problem
        if (!lpInitialised) {
            this.linearProblem = createLinearRaoProblem();
            buildProblem(raoData, raoParameters.getExtension(LinearProblemParameters.class));
            lpInitialised = true;
        } else {
            updateProblem(raoData, raoParameters.getExtension(LinearProblemParameters.class));
        }

        solveLinearProblem();
        fillCracResults(linearProblem, raoData);
        raoData.applyRangeActionResultsOnNetwork();
    }

    private void buildProblem(RaoData raoData, LinearProblemParameters linearProblemParameters) {
        try {
            fillerList.forEach(problemFiller -> problemFiller.fill(raoData, linearProblem, linearProblemParameters));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when building the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void updateProblem(RaoData raoData, LinearProblemParameters linearProblemParameters) {
        try {
            fillerList.forEach(problemFiller -> problemFiller.update(raoData, linearProblem, linearProblemParameters));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when updating the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void solveLinearProblem() {
        try {
            Enum solverResultStatus = linearProblem.solve();
            String solverResultStatusString = solverResultStatus.name();
            if (!solverResultStatusString.equals("OPTIMAL")) {
                String errorMessage = format("Solving of the linear problem failed failed with MPSolver status %s", solverResultStatusString);
                LOGGER.error(errorMessage);
                throw new LinearOptimisationException(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    public List<ProblemFiller> createFillerList(RaoParameters raoParameters) {
        fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller());
        fillerList.add(new MaxMinMarginFiller());
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillerList.add(new MaxLoopFlowFiller());
        }
        return fillerList;
    }

    public LinearProblem createLinearRaoProblem() {
        return new LinearProblem();
    }

    public static void fillCracResults(LinearProblem linearProblem, RaoData raoData) {
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        LOGGER.debug(format("Expected minimum margin: %f", linearProblem.getMinimumMarginVariable().solutionValue()));
        for (RangeAction rangeAction: raoData.getCrac().getRangeActions()) {
            if (rangeAction instanceof PstRange) {
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                double rangeActionVal = linearProblem.getRangeActionSetPointVariable(rangeAction).solutionValue();
                PstRange pstRange = (PstRange) rangeAction;
                TwoWindingsTransformer transformer = raoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                int approximatedPostOptimTap = pstRange.computeTapPosition(rangeActionVal);
                double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(raoData.getWorkingVariantId());
                pstRangeResult.setSetPoint(preventiveState, approximatedPostOptimAngle);
                pstRangeResult.setTap(preventiveState, approximatedPostOptimTap);
                LOGGER.debug(format("Range action %s has been set to %d", pstRange.getName(), approximatedPostOptimTap));
            }
        }
    }
}
