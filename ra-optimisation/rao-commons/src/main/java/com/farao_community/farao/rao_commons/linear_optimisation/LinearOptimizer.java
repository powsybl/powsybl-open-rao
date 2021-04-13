/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;

/**
 * An optimizer dedicated to the construction and solving of a linear problem.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearOptimizer.class);

    /**
     * Linear optimisation problem, core object the LinearOptimizer that
     * is solved each time the optimize method of this class is called.
     */
    private final LinearProblem linearProblem;

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
    private final List<ProblemFiller> fillers = new ArrayList<>();

    private final LinearOptimizerInput linearOptimizerInput;

    public LinearOptimizer(LinearOptimizerInput linearOptimizerInput, LinearOptimizerParameters linearOptimizerParameters) {
        this.linearOptimizerInput = linearOptimizerInput;
        linearProblem = createLinearRaoProblem();

        Unit unit = linearOptimizerParameters.getObjectiveFunction().getUnit();

        fillers.add(new CoreProblemFiller(
            linearProblem,
            linearOptimizerInput.getNetwork(),
            linearOptimizerInput.getCnecs(),
            linearOptimizerInput.getPreperimeterSetpoints(),
            linearOptimizerParameters.getPstSensitivityThreshold()));
        if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(
                linearProblem,
                linearOptimizerInput.getCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
                linearOptimizerInput.getRangeActions(),
                new MaxMinMarginParameters(unit, linearOptimizerParameters.getPstPenaltyCost())));
            fillers.add(new MnecFiller(
                linearProblem,
                linearOptimizerInput.getCnecs().stream()
                    .filter(Cnec::isMonitored)
                    .collect(Collectors.toMap(
                        Function.identity(),
                        mnec -> linearOptimizerInput.getInitialFlow(mnec, unit))
                    ),
                unit,
                linearOptimizerParameters.getMnecParameters()));
        } else if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinRelativeMarginFiller(
                linearProblem,
                linearOptimizerInput.getCnecs().stream()
                    .filter(Cnec::isOptimized)
                    .collect(Collectors.toMap(Function.identity(), linearOptimizerInput::getInitialAbsolutePtdfSum)),
                linearOptimizerInput.getRangeActions(),
                new MaxMinRelativeMarginParameters(
                    unit,
                    linearOptimizerParameters.getPstPenaltyCost(),
                    linearOptimizerParameters.getNegativeMarginObjectiveCoefficient(),
                    linearOptimizerParameters.getPtdfSumLowerBound())));
            fillers.add(new MnecFiller(
                linearProblem,
                linearOptimizerInput.getCnecs().stream()
                    .filter(Cnec::isMonitored)
                    .collect(Collectors.toMap(
                        Function.identity(),
                        mnec -> linearOptimizerInput.getInitialFlow(mnec, unit))
                    ),
                unit,
                linearOptimizerParameters.getMnecParameters()));
        }
        if (!Objects.isNull(linearOptimizerParameters.getOperatorsNotToOptimize()) && !linearOptimizerParameters.getOperatorsNotToOptimize().isEmpty()) {
            fillers.add(new UnoptimizedCnecFiller(
                linearProblem,
                linearOptimizerInput.getCnecs().stream()
                    .filter(BranchCnec::isOptimized)
                    .filter(cnec -> linearOptimizerParameters.getOperatorsNotToOptimize().contains(cnec.getOperator()))
                    .collect(Collectors.toMap(
                        Function.identity(),
                        linearOptimizerInput::getPrePerimeterMarginInAbsoluteMW)),
                linearOptimizerInput.getCnecs()));
        }
        if (linearOptimizerParameters.getLoopFlowParameters().isRaoWithLoopFlowLimitation()) {
            fillers.add(new MaxLoopFlowFiller(
                linearProblem,
                linearOptimizerInput.getLoopflowCnecs().stream().collect(Collectors.toMap(Function.identity(), linearOptimizerInput::getInitialLoopflowInMW)),
                linearOptimizerParameters.getLoopFlowParameters()));
        }
    }

    /**
     * The optimize method of the LinearOptimizer creates (or updates) and solves a LinearProblem.
     * It fills the working RaoData variant with optimisation results in the CRAC (for range actions)
     * and apply the new range action set points on the network.
     *
     * @throws LinearOptimisationException if the optimization fails
     * @throws FaraoException              if sensitivity computation have not been performed on working raoData variant
     *                                     or if loop flow data are missing when loop flow filler is present
     */
    public LinearOptimizerOutput optimize(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        if (sensitivityAndLoopflowResults == null) {
            throw new FaraoException("");
        }

        // prepare optimisation problem
        if (!lpInitialised) {
            buildProblem(sensitivityAndLoopflowResults);
            lpInitialised = true;
        } else {
            updateProblem(sensitivityAndLoopflowResults);
        }

        LinearOptimizerOutput.SolveStatus solveStatus = solveProblem();

        return generateOutput(solveStatus, sensitivityAndLoopflowResults.getSystematicSensitivityResult());
    }

    LinearProblem createLinearRaoProblem() {
        return new LinearProblem();
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

    private LinearOptimizerOutput.SolveStatus solveProblem() {
        try {
            MPSolver.ResultStatus resultStatus = linearProblem.solve();
            LinearOptimizerOutput.SolveStatus solveStatus = getSolveStatus(resultStatus);
            if (!solveStatus.equals(LinearOptimizerOutput.SolveStatus.OPTIMAL)) {
                LOGGER.warn("Solving of the linear problem failed with MPSolver status {}", solveStatus.name());
                //Do not throw an exception is solver solution not "OPTIMAL". Handle the status in LinearRao.runLinearRao
            }
            return solveStatus;
        } catch (Exception e) {
            String errorMessage = "Solving of the linear problem failed.";
            LOGGER.error(errorMessage);
            throw new LinearOptimisationException(errorMessage, e);
        }
    }

    private LinearOptimizerOutput.SolveStatus getSolveStatus(MPSolver.ResultStatus resultStatus) {
        switch (resultStatus) {
            case OPTIMAL:
                return LinearOptimizerOutput.SolveStatus.OPTIMAL;
            case FEASIBLE:
                return LinearOptimizerOutput.SolveStatus.FEASIBLE;
            case INFEASIBLE:
                return LinearOptimizerOutput.SolveStatus.INFEASIBLE;
            case UNBOUNDED:
                return LinearOptimizerOutput.SolveStatus.UNBOUNDED;
            case NOT_SOLVED:
                return LinearOptimizerOutput.SolveStatus.NOT_SOLVED;
            case ABNORMAL:
            default:
                return LinearOptimizerOutput.SolveStatus.ABNORMAL;
        }
    }

    private LinearOptimizerOutput generateOutput(LinearOptimizerOutput.SolveStatus solveStatus, SystematicSensitivityResult sensitivityResult) {
        if (!solveStatus.equals(LinearOptimizerOutput.SolveStatus.OPTIMAL)) {
            // TODO : return initial values?
            return new LinearOptimizerOutput(solveStatus, null, null);
        } else {
            Map<PstRangeAction, Integer> optimalTaps = BestTapFinder.find(
                linearOptimizerInput.getNetwork(),
                linearOptimizerInput.getMostLimitingElements(),
                linearOptimizerInput.getRangeActions().stream()
                    .filter(ra -> ra instanceof PstRangeAction && linearProblem.getRangeActionSetPointVariable(ra) != null)
                    .map(PstRangeAction.class::cast)
                    .collect(Collectors.toMap(
                        Function.identity(),
                        pstRangeAction -> linearProblem.getRangeActionSetPointVariable(pstRangeAction).solutionValue())),
                sensitivityResult);
            Map<RangeAction, Double> optimalSetpoints = new HashMap<>();
            linearOptimizerInput.getRangeActions().stream().filter(rangeAction -> linearProblem.getRangeActionSetPointVariable(rangeAction) != null)
                    .forEach(rangeAction -> {
                        if (rangeAction instanceof PstRangeAction) {
                            PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
                            optimalSetpoints.put(rangeAction, pstRangeAction.convertTapToAngle(optimalTaps.get(pstRangeAction)));
                        } else {
                            optimalSetpoints.put(rangeAction, linearProblem.getRangeActionSetPointVariable(rangeAction).solutionValue());
                        }
                    });
            return new LinearOptimizerOutput(solveStatus, optimalSetpoints, optimalTaps);
        }
    }
}
