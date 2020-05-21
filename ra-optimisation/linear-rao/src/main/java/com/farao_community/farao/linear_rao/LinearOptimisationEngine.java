/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.linear_rao.optimisation.*;
import com.farao_community.farao.linear_rao.optimisation.fillers.ProblemFiller;
import com.farao_community.farao.linear_rao.optimisation.fillers.CoreProblemFiller;
import com.farao_community.farao.linear_rao.optimisation.fillers.MaxLoopFlowFiller;
import com.farao_community.farao.linear_rao.optimisation.fillers.MaxMinMarginFiller;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A computation engine dedicated to the construction and solving of the linear
 * optimisation problem of the LinearRao.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LinearOptimisationEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinearOptimisationEngine.class);

    /**
     * Linear optimisation problem, core object the LinearOptimisationEngine that
     * is solved each time the run method of this class is called.
     */
    private LinearRaoProblem linearRaoProblem;

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

    static final String SOLVER_RESULT_STATUS_UNKNOWN = "UNKNOWN";
    private String solverResultStatusString;

    /**
     * Constructor
     */
    LinearOptimisationEngine(RaoParameters raoParameters) {
        this.lpInitialised = false;

        // TODO : load the filler list from the config file and make sure they are ordered properly
        this.fillerList = createFillerList(raoParameters);

        this.solverResultStatusString = SOLVER_RESULT_STATUS_UNKNOWN;
    }

    /**
     * The run method of the LinearOptimisationEngine creates and solves the core
     * optimisation problem of the LinearRao. It updates the LinearRaoData with optimisation result in the CRAC
     * and apply the new range action set points on the netork.
     *
     * @param linearRaoData defines the data on which the creation of the optimisation problem
     *                    is based (i.e. a given Network situation with associated Crac
     *                    and sensitivities).
     *
     * @throws LinearOptimisationException if the method fails
     */
    void run(LinearRaoData linearRaoData, LinearRaoParameters linearRaoParameters) {
        // prepare optimisation problem
        if (!lpInitialised) {
            this.linearRaoProblem = createLinearRaoProblem();
            buildProblem(linearRaoData, linearRaoParameters);
            lpInitialised = true;
        } else {
            updateProblem(linearRaoData, linearRaoParameters);
        }

        solveLinearProblem();
        if (getSolverResultStatusString().equals("OPTIMAL")) {
            fillCracResults(linearRaoProblem, linearRaoData);
            linearRaoData.applyRangeActionResultsOnNetwork();
        }
    }

    private void buildProblem(LinearRaoData linearRaoData, LinearRaoParameters linearRaoParameters) {
        try {
            fillerList.forEach(problemFiller -> problemFiller.fill(linearRaoData, linearRaoProblem, linearRaoParameters));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when building the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void updateProblem(LinearRaoData linearRaoData, LinearRaoParameters linearRaoParameters) {
        try {
            fillerList.forEach(problemFiller -> problemFiller.update(linearRaoData, linearRaoProblem, linearRaoParameters));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when updating the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void solveLinearProblem() {
        try {
            Enum solverResultStatus = linearRaoProblem.solve();
            setSolverResultStatusString(solverResultStatus.name());
            if (!getSolverResultStatusString().equals("OPTIMAL")) {
                String errorMessage = String.format("Solving of the linear problem failed with MPSolver status %s", getSolverResultStatusString());
                LOGGER.warn(errorMessage);
                //Do not throw an exception is solver solution not "OPTIMAL". Handle the status in LinearRao.runLinearRao
            }
        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    List<ProblemFiller> createFillerList(RaoParameters raoParameters) {
        fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller());
        fillerList.add(new MaxMinMarginFiller());
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillerList.add(new MaxLoopFlowFiller());
        }
        return fillerList;
    }

    LinearRaoProblem createLinearRaoProblem() {
        return new LinearRaoProblem();
    }

    public static void fillCracResults(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction: linearRaoData.getCrac().getRangeActions()) {
            if (rangeAction instanceof PstRange) {
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();
                double rangeActionVal = linearRaoProblem.getRangeActionSetPointVariable(rangeAction).solutionValue();
                PstRange pstRange = (PstRange) rangeAction;
                TwoWindingsTransformer transformer = linearRaoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                int approximatedPostOptimTap = pstRange.computeTapPosition(rangeActionVal);
                double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(linearRaoData.getWorkingVariantId());
                pstRangeResult.setSetPoint(preventiveState, approximatedPostOptimAngle);
                pstRangeResult.setTap(preventiveState, approximatedPostOptimTap);
            }
        }
    }

    public String getSolverResultStatusString() {
        return solverResultStatusString;
    }

    public void setSolverResultStatusString(String solverResultStatusString) {
        this.solverResultStatusString = solverResultStatusString;
    }
}
