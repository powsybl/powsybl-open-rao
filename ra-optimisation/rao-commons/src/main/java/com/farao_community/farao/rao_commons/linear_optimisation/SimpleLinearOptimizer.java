/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblemParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.core.ProblemFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.core.fillers.*;
import com.farao_community.farao.rao_api.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
    private static final String PARAMETERS_NOT_CONSISTENT = "Simple linear optimizer cannot perform optimization with loop flows on CRAC %s because it does not have loop flow extension";
    private static final String NO_SENSITIVITY_VALUES = "Simple linear optimizer cannot perform optimization because no sensitivity computation has been performed on variant %s";

    /**
     * Linear optimisation problem, core object the LinearOptimisationEngine that
     * is solved each time the run method of this class is called.
     */
    private LinearProblem linearProblem;

    /**
     * Boolean indicating whether the linear problem has been already initialised
     * or not.
     */
    private boolean lpInitialised = false;

    /**
     * List of problem fillers used by the engine. Each filler is responsible for
     * the creation/update of one part of the optimisation problem (i.e. of some
     * variables and constraints of the optimisation problem.
     */
    private List<ProblemFiller> fillerList;

    static final String SOLVER_RESULT_STATUS_UNKNOWN = "UNKNOWN";
    private String solverResultStatusString = SOLVER_RESULT_STATUS_UNKNOWN;

    /**
     * These RAO parameters have to be kept to check consistency between parameters and data for loopflow computation
     * at optimization time.
     */
    private RaoParameters raoParameters;

    public SimpleLinearOptimizer(RaoParameters raoParameters) {
        this.raoParameters = checkRaoParameters(raoParameters);
        this.fillerList = createFillerList(raoParameters);
    }

    public LinearProblemParameters getParameters() {
        return raoParameters.getExtension(LinearProblemParameters.class);
    }

    public String getSolverResultStatusString() {
        return solverResultStatusString;
    }

    public void setSolverResultStatusString(String solverResultStatusString) {
        this.solverResultStatusString = solverResultStatusString;
    }

    // Used to mock LinearProblem in tests
    LinearProblem createLinearRaoProblem() {
        return new LinearProblem();
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
        checkDataConsistencyWithParameters(raoParameters, raoData);
        checkSensitivityValues(raoData);

        // prepare optimisation problem
        if (!lpInitialised) {
            this.linearProblem = createLinearRaoProblem();
            buildProblem(raoData, raoParameters.getExtension(LinearProblemParameters.class));
            lpInitialised = true;
        } else {
            updateProblem(raoData, raoParameters.getExtension(LinearProblemParameters.class));
        }

        solveProblem();
        if (getSolverResultStatusString().equals("OPTIMAL")) {
            raoData.getRaoDataManager().fillRangeActionResultsWithLinearProblem(linearProblem);
            raoData.getRaoDataManager().applyRangeActionResultsOnNetwork();
        }
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

    private void solveProblem() {
        try {
            Enum solverResultStatus = linearProblem.solve();
            setSolverResultStatusString(solverResultStatus.name());
            if (!getSolverResultStatusString().equals("OPTIMAL")) {
                LOGGER.warn(format("Solving of the linear problem failed with MPSolver status %s", getSolverResultStatusString()));
                //Do not throw an exception is solver solution not "OPTIMAL". Handle the status in LinearRao.runLinearRao
            }
        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private static RaoParameters checkRaoParameters(RaoParameters raoParameters) {
        if (Objects.isNull(raoParameters.getExtension(LinearProblemParameters.class))) {
            raoParameters.addExtension(LinearProblemParameters.class, new LinearProblemParameters());
        }
        return raoParameters;
    }

    private static List<ProblemFiller> createFillerList(RaoParameters raoParameters) {
        // TODO : load the filler list from the config file and make sure they are ordered properly
        List<ProblemFiller> fillerList = Arrays.asList(new CoreProblemFiller(), new MaxMinMarginFiller());
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillerList.add(new MaxLoopFlowFiller());
        }
        return fillerList;
    }

    private static void checkDataConsistencyWithParameters(RaoParameters raoParameters, RaoData raoData) {
        if (raoParameters.isRaoWithLoopFlowLimitation()
            && Objects.isNull(raoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            String msg = format(PARAMETERS_NOT_CONSISTENT, raoData.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    private static void checkSensitivityValues(RaoData raoData) {
        if (!raoData.hasSensitivityValues()) {
            String msg = format(NO_SENSITIVITY_VALUES, raoData.getWorkingVariantId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }
}
