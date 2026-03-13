/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles constraints for maximum number od RAs to activate (max-ra), maximum number of TSOs that can activate RAs (max-tso),
 * maximum number of RAs per TSO (max-ra-per-tso), maximum number of PSTs per TSO (max-pst-per-tso) and
 * maximum number of elementary actions per TSO (max-elementary-actions-per-tso).
 * Beware: this introduces binary variables to define if an RA is used.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaUsageLimitsFiller implements ProblemFiller {

    private final Map<State, Set<RangeAction<?>>> rangeActions;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;
    private final RangeActionLimitationParameters rangeActionLimitationParameters;
    private final boolean arePstSetpointsApproximated;
    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-4;
    private final boolean costOptimization;

    public RaUsageLimitsFiller(Map<State, Set<RangeAction<?>>> rangeActions,
                               RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                               RangeActionLimitationParameters rangeActionLimitationParameters,
                               boolean arePstSetpointsApproximated,
                               boolean costOptimization) {
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
        this.rangeActionLimitationParameters = rangeActionLimitationParameters;
        this.arePstSetpointsApproximated = arePstSetpointsApproximated;
        this.costOptimization = costOptimization;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
typo        // We need to build all the variationVariable before adding the other constraint because the different states in multi curative are interdependent.
        // ex. to build MaxRaConstraint for a state in curative2 we might need the variables defined for a state in curative1
        List<Map.Entry<State, Set<RangeAction<?>>>> sortedEntries = rangeActions.entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> entry.getKey().getInstant().getOrder()))
            .collect(Collectors.toList());

        for (Map.Entry<State, Set<RangeAction<?>>> entry : sortedEntries) {
            State state = entry.getKey();
            Set<RangeAction<?>> rangeActionSet = entry.getValue();
            if (!rangeActionLimitationParameters.areRangeActionLimitedForState(state)) {
                continue;
            }
            // if cost optimization, variation variables are already defined
            rangeActionSet.forEach(ra -> buildIsVariationVariableAndConstraints(linearProblem, ra, state));

            if (rangeActionLimitationParameters.getMaxRangeActions(state) != null) {
                addMaxRaConstraint(linearProblem, state);
            }
            if (rangeActionLimitationParameters.getMaxTso(state) != null) {
                addMaxTsoConstraint(linearProblem, state);
            }

            if (!rangeActionLimitationParameters.getMaxRangeActionPerTso(state).isEmpty()) {
                addMaxRaPerTsoConstraint(linearProblem, state);
            }
            if (!rangeActionLimitationParameters.getMaxPstPerTso(state).isEmpty()) {
                addMaxPstPerTsoConstraint(linearProblem, state);
            }
            if (!rangeActionLimitationParameters.getMaxElementaryActionsPerTso(state).isEmpty()) {
                addMaxElementaryActionsPerTsoConstraint(linearProblem, state);
            }
        }
    }

    /**
     * if state is preventive, just consider the preventive state
     *
     * if state is curative, to be able to handle 2P in multi-curative we need to consider the given state
     * as well as all the previous curative states sharing the same contingency present in this.rangeActions.
     * (note. if we are not in 2P this.rangeActions only contain the rangeActions available for a given state)
     *
     * @param state the reference state used to filter curative states by
     *              contingency and temporal order
     * @return a map of states mapped to their available range actions
     */
    Map<State, Set<RangeAction<?>>> getAllRangeActionOfStateToConsider(State state) {

        if (state.getInstant().isCurative()) {
            return rangeActions.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().isCurative())
                .filter(entry -> entry.getKey().getInstant().comesBefore(state.getInstant()) || entry.getKey().getInstant().equals(state.getInstant()))
                .filter(entry -> entry.getKey().getContingency().equals(state.getContingency()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));

        } else if (state.getInstant().isPreventive()) {
            return rangeActions.entrySet().stream()
                .filter(entry -> entry.getKey().getInstant().isPreventive())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
        } else {
            throw new OpenRaoException("State " + state + " is neither preventive nor curative.");
        }

    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        rangeActions.forEach((state, rangeActionSet) -> {
            Map<String, Integer> maxElementaryActionsPerTso = rangeActionLimitationParameters.getMaxElementaryActionsPerTso(state);
            Map<String, Set<PstRangeAction>> pstRangeActionsPerTso = new HashMap<>();
            rangeActionSet.stream()
                .filter(PstRangeAction.class::isInstance)
                .filter(rangeAction -> maxElementaryActionsPerTso.containsKey(rangeAction.getOperator()))
                .map(PstRangeAction.class::cast)
                .forEach(pstRangeAction -> pstRangeActionsPerTso.computeIfAbsent(pstRangeAction.getOperator(), tso -> new HashSet<>()).add(pstRangeAction));

            for (String tso : maxElementaryActionsPerTso.keySet()) {
                for (PstRangeAction pstRangeAction : pstRangeActionsPerTso.getOrDefault(tso, Set.of())) {
                    // use pre-perimeter tap because PST's tap may be different from the initial tap after previous perimeter
                    int initialTap = prePerimeterRangeActionSetpoints.getTap(pstRangeAction);
                    int currentTap = rangeActionActivationResult.getOptimizedTap(pstRangeAction, state);

                    linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE).setLb((double) currentTap - initialTap);
                    linearProblem.getPstAbsoluteVariationFromInitialTapConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE).setLb((double) initialTap - currentTap);
                }
            }
        });
    }

    private double getAverageAbsoluteTapToAngleConversionFactor(PstRangeAction pstRangeAction) {
        int minTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().min(Integer::compareTo).orElseThrow();
        int maxTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        double minAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().min(Double::compareTo).orElseThrow();
        double maxAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().max(Double::compareTo).orElseThrow();
        return Math.abs((maxAngle - minAngle) / (maxTap - minTap));
    }

    /**
     * Get relaxation term to add to correct the initial setpoint, to ensure problem feasibility depending on the approximations.
     * If PSTs are modelled with approximate integers, make sure that the initial setpoint is feasible (it should be at
     * a distance smaller than 0.3 * getAverageAbsoluteTapToAngleConversionFactor from a feasible setpoint in the MIP)
     */
    private double getInitialSetpointRelaxation(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction && arePstSetpointsApproximated) {
            // The BestTapFinder is accurate at 35% of the setpoint difference between 2 taps. Using 30% here to be safe.
            return 0.3 * getAverageAbsoluteTapToAngleConversionFactor(pstRangeAction);
        } else {
            return RANGE_ACTION_SETPOINT_EPSILON;
        }
    }

    private void buildIsVariationVariableAndConstraints(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        if (!costOptimization) {
            OpenRaoMPVariable isVariationVariable = linearProblem.addRangeActionVariationBinary(rangeAction, state);

            OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
            OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(rangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);

            double initialSetpointRelaxation = getInitialSetpointRelaxation(rangeAction);

            // range action absolute variation <= isVariationVariable * (max setpoint - min setpoint) + initialSetpointRelaxation
            // RANGE_ACTION_SETPOINT_EPSILON is used to mitigate rounding issues, ensuring that the maximum setpoint is feasible
            // initialSetpointRelaxation is used to ensure that the initial setpoint is feasible
            OpenRaoMPConstraint constraint = linearProblem.addIsVariationConstraint(-linearProblem.infinity(), initialSetpointRelaxation, rangeAction, state);
            constraint.setCoefficient(upwardVariationVariable, 1);
            constraint.setCoefficient(downwardVariationVariable, 1);
            double initialSetpoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);
            constraint.setCoefficient(
                isVariationVariable,
                -(rangeAction.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - rangeAction.getMinAdmissibleSetpoint(initialSetpoint))
            );
        }
    }

    /**
     * Add constraint to limit the number of remedial action that can be activated
     *
     * @param linearProblem
     * @param state
     */
    private void addMaxRaConstraint(LinearProblem linearProblem, State state) {

        Integer maxRa = rangeActionLimitationParameters.getMaxRangeActions(state);
        Map<State, Set<RangeAction<?>>> rangeActionsPerPreviousCurativeState = getAllRangeActionOfStateToConsider(state);

        int numberOfRas = rangeActionsPerPreviousCurativeState.values().stream().mapToInt(ras -> ras.size()).sum();

        if (maxRa == null || maxRa >= numberOfRas) {
            return;
        }

        OpenRaoMPConstraint maxRaConstraint = linearProblem.addMaxRaConstraint(0, maxRa, state);

        // if the state is curative, we want to be able to handle the cumulative effect of the max ra usage limit in 2P
        rangeActionsPerPreviousCurativeState.entrySet().forEach(entry -> {
            entry.getValue().forEach(ra -> {
                OpenRaoMPVariable isVariationVariable = linearProblem.getRangeActionVariationBinary(ra, entry.getKey());
                maxRaConstraint.setCoefficient(isVariationVariable, 1);
            });
        });

    }

    /**
     * Add constraint to limit the number of TSOs that can be activated
     *
     * @param linearProblem
     * @param state
     */
    private void addMaxTsoConstraint(LinearProblem linearProblem, State state) {
        Integer maxTso = rangeActionLimitationParameters.getMaxTso(state);

        Map<State, Set<RangeAction<?>>> stateAndRangeActionsToConsider = getAllRangeActionOfStateToConsider(state);
        if (maxTso == null) {
            return;
        }

        Set<String> maxTsoExclusions = new HashSet<>();

        // For multi-curative add all the TSOs to exclude from previous curative states sharing same contingency as state
        stateAndRangeActionsToConsider.forEach((s, rangeActionsSet) -> maxTsoExclusions.addAll(rangeActionLimitationParameters.getMaxTsoExclusion(s)));

        Set<String> constraintTsos = new HashSet<>();

        // Filter out all the TSO in maxTsoExclusions
        stateAndRangeActionsToConsider.values()
            .forEach(raSet ->
                constraintTsos.addAll(
                    raSet.stream()
                        .map(RemedialAction::getOperator)
                        .filter(Objects::nonNull)
                        .filter(tso -> !maxTsoExclusions.contains(tso))
                        .collect(Collectors.toSet())
                )
            );

        // If rangeActions.size() > 1, we are in 2P situation, in this case we still need to create the variable and constraint
        // because the variable tsoRaUsedVariable will be used in other state in multi curative.
        if (maxTso >= constraintTsos.size() && rangeActions.size() == 1) {
            return;
        }

        OpenRaoMPConstraint maxTsoConstraint = linearProblem.addMaxTsoConstraint(0, maxTso, state);
        constraintTsos.forEach(tso -> {
            // Create a cumulative binary variable
            // -> indicate if the TSO activated one of its remedial action during state or in a previous curative state (sharing same contingency as state)
            OpenRaoMPVariable tsoRaUsedCumulativeVariable = linearProblem.addTsoRaUsedCumulativeVariable(0, 1, tso, state);
            maxTsoConstraint.setCoefficient(tsoRaUsedCumulativeVariable, 1);

            // Create TsoRaUsedVariable if state has at least one range action from this tso.
            // + define the constraint: tsoRaUsed >= ra1_used, tsoRaUsed >= ra2_used + ...
            Set<RangeAction> raSet = rangeActions.get(state).stream().filter(ra -> tso.equals(ra.getOperator())).collect(Collectors.toSet());
            if (!raSet.isEmpty()) {
                OpenRaoMPVariable tsoRaUsedVariable = linearProblem.addTsoRaUsedVariable(0, 1, tso, state);
                raSet.forEach(ra -> {
                    OpenRaoMPConstraint tsoRaUsedConstraint = linearProblem.addTsoRaUsedConstraint(0, linearProblem.infinity(), tso, ra, state);
                    tsoRaUsedConstraint.setCoefficient(tsoRaUsedVariable, 1);
                    tsoRaUsedConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state), -1);
                });
            }

            // add constraint tsoRaUsed<=tsoRaUsedCumulativeVariable
            stateAndRangeActionsToConsider.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(ra -> tso.equals(ra.getOperator())))
                .forEach(entry -> {
                    OpenRaoMPConstraint tsoRaUsedCumulativeConstraint = linearProblem.addTsoRaUsedCumulativeConstraint(0, linearProblem.infinity(), tso, state, entry.getKey());
                    tsoRaUsedCumulativeConstraint.setCoefficient(tsoRaUsedCumulativeVariable, 1);
                    tsoRaUsedCumulativeConstraint.setCoefficient(linearProblem.getTsoRaUsedVariable(tso, entry.getKey()), -1);
                });
        });
    }

    /**
     * Add constraint to limit the number of range actions per TSO in a state
     *
     * @param linearProblem
     * @param state
     */
    private void addMaxRaPerTsoConstraint(LinearProblem linearProblem, State state) {
        Map<String, Integer> maxRaPerTso = rangeActionLimitationParameters.getMaxRangeActionPerTso(state);
        Map<State, Set<RangeAction<?>>> stateAndRangeActionsToConsider = getAllRangeActionOfStateToConsider(state);
        if (maxRaPerTso.isEmpty()) {
            return;
        }
        maxRaPerTso.forEach((tso, maxRaForTso) -> {
            OpenRaoMPConstraint maxRaPerTsoConstraint = linearProblem.addMaxRaPerTsoConstraint(0, maxRaForTso, tso, state);
            stateAndRangeActionsToConsider
                .forEach((state1, raSet) ->
                    raSet.stream().filter(ra -> tso.equals(ra.getOperator()))
                        .forEach(ra -> maxRaPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state1), 1)));
        });
    }

    /**
     * Add constraint to limit the number of PSTs per TSO in a state
     *
     * @param linearProblem
     * @param state
     */
    private void addMaxPstPerTsoConstraint(LinearProblem linearProblem, State state) {
        Map<String, Integer> maxPstPerTso = rangeActionLimitationParameters.getMaxPstPerTso(state);
        Map<State, Set<RangeAction<?>>> stateAndRangeActionsToConsider = getAllRangeActionOfStateToConsider(state);

        if (maxPstPerTso == null) {
            return;
        }
        maxPstPerTso.forEach((tso, maxPstForTso) -> {
            OpenRaoMPConstraint maxPstPerTsoConstraint = linearProblem.addMaxPstPerTsoConstraint(0, maxPstForTso, tso, state);
            stateAndRangeActionsToConsider.forEach((state1, raSet) ->
                    raSet.stream().filter(ra -> ra instanceof PstRangeAction && tso.equals(ra.getOperator()))
                        .forEach(ra -> maxPstPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state1), 1)));
        });
    }

    /**
     * Add constraints and variables to limit the number of taps (elementary actions) that can be moved
     * Note: that if pst1 does 0 (initial) -> 2 (curative1) -> 0 (curative 2): the number of elementary action used in curative 2 is considered to be 4.
     *
     * @param linearProblem
     * @param state
     */
    private void addMaxElementaryActionsPerTsoConstraint(LinearProblem linearProblem, State state) {
        Map<String, Integer> maxElementaryActionsPerTso = rangeActionLimitationParameters.getMaxElementaryActionsPerTso(state);
        Map<State, Set<RangeAction<?>>> stateAndRangeActionsToConsider = getAllRangeActionOfStateToConsider(state);

        if (maxElementaryActionsPerTso == null) {
            return;
        }

        Map<String, Set<PstRangeAction>> pstRangeActionsPerTso = new HashMap<>();
        rangeActions.getOrDefault(state, Set.of()).stream()
            .filter(PstRangeAction.class::isInstance)
            .filter(rangeAction -> maxElementaryActionsPerTso.containsKey(rangeAction.getOperator()))
            .map(PstRangeAction.class::cast)
            .forEach(pstRangeAction -> pstRangeActionsPerTso.computeIfAbsent(pstRangeAction.getOperator(), tso -> new HashSet<>()).add(pstRangeAction));

        for (Map.Entry<String, Integer> maxElementaryActionsForTso : maxElementaryActionsPerTso.entrySet()) {
            String tso = maxElementaryActionsForTso.getKey();
            int maxElementaryActions = maxElementaryActionsForTso.getValue();
            OpenRaoMPConstraint maxElementaryActionsConstraint = linearProblem.addTsoMaxElementaryActionsConstraint(0, maxElementaryActions, tso, state);
            for (PstRangeAction pstRangeAction : pstRangeActionsPerTso.getOrDefault(tso, Set.of())) {
                stateAndRangeActionsToConsider.forEach((state1, raSet) -> {
                    if (raSet.stream().anyMatch(ra -> ra.equals(pstRangeAction))) {
                        OpenRaoMPVariable totalPstRangeActionTapUpwardVariationVariable = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, state1, LinearProblem.VariationDirectionExtension.UPWARD);
                        OpenRaoMPVariable totalPstRangeActionTapDownwardVariationVariable = linearProblem.getTotalPstRangeActionTapVariationVariable(pstRangeAction, state1, LinearProblem.VariationDirectionExtension.DOWNWARD);
                        maxElementaryActionsConstraint.setCoefficient(totalPstRangeActionTapUpwardVariationVariable, 1);
                        maxElementaryActionsConstraint.setCoefficient(totalPstRangeActionTapDownwardVariationVariable, 1);
                    }
                });
            }

        }
    }
}
