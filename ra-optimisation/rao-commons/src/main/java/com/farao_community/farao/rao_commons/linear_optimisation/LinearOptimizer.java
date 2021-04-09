/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.ValidationException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
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

    private LinearOptimizerInput linearOptimizerInput;

    private LinearOptimizerParameters linearOptimizerParameters;

    public LinearOptimizer(LinearOptimizerInput linearOptimizerInput, LinearOptimizerParameters linearOptimizerParameters) {
        this.linearOptimizerInput = linearOptimizerInput;
        this.linearOptimizerParameters = linearOptimizerParameters;
        linearProblem = createLinearRaoProblem();

        fillers.add(new CoreProblemFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
        if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
            fillers.add(new MnecFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
        } else if (linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                || linearOptimizerParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinRelativeMarginFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
            fillers.add(new MnecFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
        }
        if (!Objects.isNull(linearOptimizerParameters.getOperatorsNotToOptimize()) && !linearOptimizerParameters.getOperatorsNotToOptimize().isEmpty()) {
            fillers.add(new OperatorsNotToOptimizeFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
        }
        if (linearOptimizerParameters.getLoopFlowParameters().isRaoWithLoopFlowLimitation()) {
            fillers.add(new MaxLoopFlowFiller(linearProblem, linearOptimizerInput, linearOptimizerParameters));
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

    public LinearOptimizer(List<ProblemFiller> fillers) {
        this.fillers = fillers;
    }

    // Methods for tests
    LinearOptimizer() {
        this(Arrays.asList(new CoreProblemFiller(), new MaxMinMarginFiller(new LinearProblem(), null, null)));
    }

    LinearProblem createLinearRaoProblem() {
        return new LinearProblem();
    }
    // End of methods for tests

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

    private static void checkSensitivityValues(RaoData raoData) {
        if (!raoData.hasSensitivityValues()) {
            String msg = format(NO_SENSITIVITY_VALUES, raoData.getWorkingVariantId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    private LinearOptimizerOutput generateOutput(LinearOptimizerOutput.SolveStatus solveStatus, SystematicSensitivityResult sensitivityResult) {
        if (!solveStatus.equals(LinearOptimizerOutput.SolveStatus.OPTIMAL)) {
            // TODO : return initial values?
            return new LinearOptimizerOutput(solveStatus, null, null);
        } else {
            Map<PstRangeAction, Integer> optimalTaps = computeBestTaps(sensitivityResult);
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

    /**
     * This function computes the best tap positions for PstRangeActions that were optimized in the linear problem.
     * It is a little smarter than just rounding the optimal angle to the closest tap position:
     * if the optimal angle is close to the limit between two tap positions, it will chose the one that maximizes the
     * minimum margin on the 10 most limiting elements (pre-optim)
     * Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
     * enough (current threshold of 10%), then the closest tap is kept
     *
     * @return a map containing the best tap position for every PstRangeAction that was optimized in the linear problem
     */
    Map<PstRangeAction, Integer> computeBestTaps(SystematicSensitivityResult sensitivityResult) {
        Map<PstRangeAction, Integer> bestTaps = new HashMap<>();
        Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap = new HashMap<>();

        Set<PstRangeAction> pstRangeActions = linearOptimizerInput.getRangeActions().stream()
                .filter(ra -> ra instanceof PstRangeAction && linearProblem.getRangeActionSetPointVariable(ra) != null)
                .map(PstRangeAction.class::cast).collect(Collectors.toSet());
        for (PstRangeAction pstRangeAction : pstRangeActions) {
            double rangeActionVal = linearProblem.getRangeActionSetPointVariable(pstRangeAction).solutionValue();
            minMarginPerTap.put(pstRangeAction, computeMinMarginsForBestTaps(pstRangeAction, rangeActionVal, linearOptimizerInput.getMostLimitingElements(), sensitivityResult));
        }

        Map<String, Integer> bestTapPerPstGroup = computeBestTapPerPstGroup(minMarginPerTap);

        for (PstRangeAction pstRangeAction : pstRangeActions) {
            if (pstRangeAction.getGroupId().isPresent()) {
                bestTaps.put(pstRangeAction, bestTapPerPstGroup.get(pstRangeAction.getGroupId().get()));
            } else {
                int bestTap = minMarginPerTap.get(pstRangeAction).entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
                bestTaps.put(pstRangeAction, bestTap);
            }
        }
        return bestTaps;
    }

    /**
     * This function computes, for every group of PSTs, the common tap position that maximizes the minimum margin
     *
     * @param minMarginPerTap: a map containing for each PstRangeAction, a map with tap positions and resulting minimum margin
     * @return a map containing for each group ID, the best common tap position for the PSTs
     */
    static Map<String, Integer> computeBestTapPerPstGroup(Map<PstRangeAction, Map<Integer, Double>> minMarginPerTap) {
        Map<String, Integer> bestTapPerPstGroup = new HashMap<>();
        Set<PstRangeAction> pstRangeActions = minMarginPerTap.keySet();
        Set<String> pstGroups = pstRangeActions.stream().map(PstRangeAction::getGroupId).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String pstGroup : pstGroups) {
            Set<PstRangeAction> pstsOfGroup = pstRangeActions.stream()
                    .filter(pstRangeAction -> pstRangeAction.getGroupId().isPresent() && pstRangeAction.getGroupId().get().equals(pstGroup))
                    .collect(Collectors.toSet());
            Map<Integer, Double> groupMinMarginPerTap = new HashMap<>();
            for (PstRangeAction pstRangeAction : pstsOfGroup) {
                Map<Integer, Double> pstMinMarginPerTap = minMarginPerTap.get(pstRangeAction);
                for (Map.Entry<Integer, Double> entry : pstMinMarginPerTap.entrySet()) {
                    int tap = entry.getKey();
                    if (groupMinMarginPerTap.containsKey(tap)) {
                        groupMinMarginPerTap.put(tap, Math.min(entry.getValue(), groupMinMarginPerTap.get(tap)));
                    } else {
                        groupMinMarginPerTap.put(tap, entry.getValue());
                    }
                }
            }
            int bestGroupTap = groupMinMarginPerTap.entrySet().stream().max(Comparator.comparing(Map.Entry<Integer, Double>::getValue)).orElseThrow().getKey();
            bestTapPerPstGroup.put(pstGroup, bestGroupTap);
        }
        return bestTapPerPstGroup;
    }

    /**
     * This function computes the best tap positions for an optimized PST range action, using the optimal angle
     * computed by the linear problem
     * It first chooses the closest tap position to the angle, then the second closest one, if the angle is close enough
     * (15% threshold) to the limit between two tap positions
     * It computes the minimum margin among the most limiting cnecs for both tap positions and returns them in a map
     * Exceptions:
     * - if the closest tap position is at a min or max limit, and the angle is close to the angle limit, then only
     * the closest tap is returned. The margin is not computed but replaced with Double.MAX_VALUE
     * - if the angle is not close enough to the limit between two tap positions, only the closest tap is returned
     * with a Double.MAX_VALUE margin
     * - if the second closest tap position does not improve the margin enough (10% threshold), then only the closest
     * tap is returned with a Double.MAX_VALUE margin
     *
     * @param pstRangeAction:    the PstRangeAction for which we need the best taps and margins
     * @param angle:             the optimal angle computed by the linear problem
     * @param mostLimitingCnecs: the cnecs upon which we compute the minimum margin
     * @return a map containing the minimum margin for each best tap position (one or two taps)
     */
    Map<Integer, Double> computeMinMarginsForBestTaps(PstRangeAction pstRangeAction, double angle, List<BranchCnec> mostLimitingCnecs, SystematicSensitivityResult sensitivityResult) {
        int closestTap = pstRangeAction.computeTapPosition(angle);
        double closestAngle = pstRangeAction.convertTapToAngle(closestTap);

        Integer otherTap = null;

        // We don't have access to min and max tap positions directly
        // We have access to min and max angles, but angles and taps do not necessarily increase/decrese in the same direction
        // So we have to try/catch in order to know if we're at the tap limits
        boolean testTapPlus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap + 1);
        } catch (ValidationException e) {
            testTapPlus1 = false;
        }
        boolean testTapMinus1 = true;
        try {
            pstRangeAction.convertTapToAngle(closestTap - 1);
        } catch (ValidationException e) {
            testTapMinus1 = false;
        }

        if (testTapPlus1 && testTapMinus1) {
            // We can test tap+1 and tap-1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            otherTap = (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) ? closestTap + 1 : closestTap - 1;
        } else if (testTapPlus1) {
            // We can only test tap+1, if the optimal angle is between the closest angle and the angle of tap+1
            double angleOfTapPlus1 = pstRangeAction.convertTapToAngle(closestTap + 1);
            if (Math.signum(angleOfTapPlus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap + 1;
            }
        } else if (testTapMinus1) {
            // We can only test tap-1, if the optimal angle is between the closest angle and the angle of tap-1
            double angleOfTapMinus1 = pstRangeAction.convertTapToAngle(closestTap - 1);
            if (Math.signum(angleOfTapMinus1 - closestAngle) * Math.signum(angle - closestAngle) > 0) {
                otherTap = closestTap - 1;
            }
        }

        // Default case
        if (otherTap == null) {
            return Map.of(closestTap, Double.MAX_VALUE);
        }

        double otherAngle = pstRangeAction.convertTapToAngle(otherTap);
        double approxLimitAngle = 0.5 * (closestAngle + otherAngle);
        if (Math.abs(angle - approxLimitAngle) / Math.abs(closestAngle - otherAngle) < 0.15) {
            // Angle is too close to the limit between two tap positions
            // Chose the tap that maximizes the margin on the most limiting element
            Pair<Double, Double> margins = computeMinMargins(pstRangeAction, mostLimitingCnecs, closestAngle, otherAngle, sensitivityResult);
            // Exception: if choosing the tap that is not the closest one to the optimal angle does not improve the margin
            // enough (current threshold of 10%), then only the closest tap is kept
            // This is actually a workaround that mitigates adverse effects of this rounding on virtual costs
            // TODO : we can remove it when we use cost evaluators directly here
            if (margins.getRight() > margins.getLeft() + 0.1 * Math.abs(margins.getLeft())) {
                return Map.of(closestTap, margins.getLeft(), otherTap, margins.getRight());
            }
        }

        // Default case
        return Map.of(closestTap, Double.MAX_VALUE);
    }

    /**
     * This method estimates the minimum margin upon a given set of cnecs, for two angles of a given PST
     *
     * @param pstRangeAction: the PstRangeAction that we should test on two angles
     * @param cnecs:          the set of cnecs to compute the minimum margin
     * @param angle1:         the first angle for the PST
     * @param angle2:         the second angle for the PST
     * @return a pair of two minimum margins (margin for angle1, margin for angle2)
     */
    Pair<Double, Double> computeMinMargins(PstRangeAction pstRangeAction, List<BranchCnec> cnecs, double angle1, double angle2, SystematicSensitivityResult sensitivityResult) {
        double minMargin1 = Double.MAX_VALUE;
        double minMargin2 = Double.MAX_VALUE;
        for (BranchCnec cnec : cnecs) {
            double sensitivity = sensitivityResult.getSensitivityOnFlow(pstRangeAction, cnec);
            double currentSetPoint = pstRangeAction.getCurrentValue(linearOptimizerInput.getNetwork());
            double referenceFlow = sensitivityResult.getReferenceFlow(cnec);

            double flow1 = sensitivity * (angle1 - currentSetPoint) + referenceFlow;
            double flow2 = sensitivity * (angle2 - currentSetPoint) + referenceFlow;

            Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            if (minFlow.isPresent()) {
                minMargin1 = Math.min(minMargin1, flow1 - minFlow.get());
                minMargin2 = Math.min(minMargin2, flow2 - minFlow.get());
            }
            Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
            if (maxFlow.isPresent()) {
                minMargin1 = Math.min(minMargin1, maxFlow.get() - flow1);
                minMargin2 = Math.min(minMargin2, maxFlow.get() - flow2);
            }
        }
        return Pair.of(minMargin1, minMargin2);
    }
}
