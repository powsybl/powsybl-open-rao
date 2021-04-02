/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_PENALTY_COST;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
import static java.lang.String.format;

/**
 * An optimizer dedicated to the construction and solving of a linear problem.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearOptimizer.class);
    private static final String NO_SENSITIVITY_VALUES = "Linear optimizer cannot perform optimization because no sensitivity computation has been performed on variant %s";

    /**
     * Linear optimisation problem, core object the LinearOptimizer that
     * is solved each time the optimize method of this class is called.
     */
    private LinearProblem linearProblem;

    /**
     * Boolean indicating whether the linear problem has been already initialised
     * or not.
     */
    private boolean lpInitialised = false;

    /**
     * List of problem fillers used by the optimizer. Each filler is responsible for
     * the creation/update of one part of the optimisation problem (i.e. of some
     * variables and constraints of the optimisation problem.
     */
    private List<ProblemFiller> fillers = new ArrayList<>();

    private String solverResultStatusString = "UNKNOWN";

    private LinearOptimizerInput linearOptimizerInput;

    private LinearOptimizerParameters linearOptimizerParameters;

    public LinearOptimizer(LinearOptimizerInput linearOptimizerInput, LinearOptimizerParameters linearOptimizerParameters) {
        this.linearOptimizerInput = linearOptimizerInput;
        this.linearOptimizerParameters = linearOptimizerParameters;
        linearProblem = createLinearRaoProblem();
        addCoreProblemFiller();
        if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            addMaxMinMarginFiller();
            addMnecFiller();
        } else if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            addMaxMinRelativeMarginFiller();
            addMnecFiller();
        }
        if (!Objects.isNull(linearOptimizerParameters.getOperatorsNotToOptimize()) && !linearOptimizerParameters.getOperatorsNotToOptimize().isEmpty()) {
            addOperatorsNotToOptimizeFiller();
        }
        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            addMaxLoopFlowFiller();
        }
    }

    private void addCoreProblemFiller() {
        fillers.add(new CoreProblemFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters.getPstSensitivityThreshold(), linearOptimizerParameters.getMaxPstPerTso()));

    }

    private void addMaxMinMarginFiller() {
        fillers.add(new MaxMinMarginFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters.getObjectiveFunction().getUnit(), linearOptimizerParameters.getPstPenaltyCost()));
    }

    private void addMnecFiller() {
        fillers.add(new MnecFiller(linearProblem,
                linearOptimizerInput,
                linearOptimizerParameters.getObjectiveFunction().getUnit(),
                linearOptimizerParameters.getMnecAcceptableMarginDiminution(),
                linearOptimizerParameters.getMnecViolationCost(),
                linearOptimizerParameters.getMnecConstraintAdjustmentCoefficient()));
    }

    private void addMaxMinRelativeMarginFiller() {
        fillers.add(new MaxMinRelativeMarginFiller(linearProblem,
                linearOptimizerInput,
                linearOptimizerParameters.getObjectiveFunction().getUnit(),
                linearOptimizerParameters.getPstPenaltyCost(),
                linearOptimizerParameters.getNegativeMarginObjectiveCoefficient(),
                linearOptimizerParameters.getPtdfSumLowerBound()));
    }

    private void addOperatorsNotToOptimizeFiller() {
        fillers.add(new OperatorsNotToOptimizeFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters.getOperatorsNotToOptimize()));
    }

    private void addMaxLoopFlowFiller() {
        fillers.add(new MaxLoopFlowFiller(linearProblem,
                linearOptimizerInput,
                linearOptimizerParameters.getLoopFlowConstraintAdjustmentCoefficient(),
                linearOptimizerParameters.getLoopFlowViolationCost(),
                linearOptimizerParameters.getLoopFlowApproximationLevel(),
                linearOptimizerParameters.getLoopFlowAcceptableAugmentation()));
    }


    /**
     * The optimize method of the LinearOptimizer creates (or updates) and solves a LinearProblem.
     * It fills the working RaoData variant with optimisation results in the CRAC (for range actions)
     * and apply the new range action set points on the network.
     *
     * @throws LinearOptimisationException if the optimization fails
     * @throws FaraoException if sensitivity computation have not been performed on working raoData variant
     * or if loop flow data are missing when loop flow filler is present
     */
    public void optimize(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        // TODO : if (loopFlowApproximationLevel.shouldUpdatePtdfWithPstChange()) => recompute commercial flows

        /*
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram());
            LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getNetwork(), raoData.getSystematicSensitivityResult(), raoData.getLoopflowCnecs());
            raoData.getCracResultManager().fillCnecResultsWithLoopFlows(lfResults);
         */

        if (sensitivityAndLoopflowResults == null) {
            throw new FaraoException("");
        }

        // prepare optimisation problem
        if (!lpInitialised) {
            linearProblem = createLinearRaoProblem();
            buildProblem(sensitivityAndLoopflowResults);
            lpInitialised = true;
        } else {
            updateProblem(sensitivityAndLoopflowResults);
        }

        solveProblem();

        // TODO : generate output
    }

    public LinearOptimizer(List<ProblemFiller> fillers) {
        this.fillers = fillers;
    }

    // Methods for tests
    LinearOptimizer() {
        this(Arrays.asList(new CoreProblemFiller(), new MaxMinMarginFiller(new LinearProblem(), null, MEGAWATT, DEFAULT_PST_PENALTY_COST)));
    }

    LinearProblem createLinearRaoProblem() {
        return new LinearProblem();
    }
    // End of methods for tests

    public String getSolverResultStatusString() {
        return solverResultStatusString;
    }

    public void setSolverResultStatusString(String solverResultStatusString) {
        this.solverResultStatusString = solverResultStatusString;
    }

    private void buildProblem(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        try {
            fillers.forEach(problemFiller -> problemFiller.fill(sensitivityAndLoopflowResults));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when building the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void updateProblem(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        try {
            fillers.forEach(problemFiller -> problemFiller.update(sensitivityAndLoopflowResults));
        } catch (Exception e) {
            String errorMessage = "Linear optimisation failed when updating the problem.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private void solveProblem() {
        try {
            String solverResultStatus = linearProblem.solve();
            setSolverResultStatusString(solverResultStatus);
            if (!getSolverResultStatusString().equals("OPTIMAL")) {
                LOGGER.warn("Solving of the linear problem failed with MPSolver status {}", getSolverResultStatusString());
                //Do not throw an exception is solver solution not "OPTIMAL". Handle the status in LinearRao.runLinearRao
            }
        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            setSolverResultStatusString("ABNORMAL");
            throw new LinearOptimisationException(errorMessage, e);
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
