/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Handles constraints for maximum number od RAs to activate (ma-ra), maximum number of TSOs that can activate RAs (max-tso),
 * maximum number of RAs per TSO (max-ra-per-tso), and maximum number of PSTs per TSO (max-pst-per-tso).
 * Beware: this introduces binary variables to define if an RA is used.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaUsageLimitsFiller implements ProblemFiller {
    private final Set<RangeAction<?>> rangeActions;
    private final RangeActionResult prePerimeterRangeActionResult;
    private final Integer maxRa;
    private final Integer maxTso;
    private final Set<String> maxTsoExclusions;
    private final Map<String, Integer> maxPstPerTso;
    private final Map<String, Integer> maxRaPerTso;
    private boolean arePstSetpointsApproximated;
    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;

    public RaUsageLimitsFiller(Set<RangeAction<?>> rangeActions,
                               RangeActionResult prePerimeterRangeActionResult,
                               Integer maxRa,
                               Integer maxTso,
                               Set<String> maxTsoExclusions,
                               Map<String, Integer> maxPstPerTso,
                               Map<String, Integer> maxRaPerTso,
                               boolean arePstSetpointsApproximated) {
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
        this.maxRa = maxRa;
        this.maxTso = maxTso;
        this.maxTsoExclusions = maxTsoExclusions != null ? maxTsoExclusions : new HashSet<>();
        this.maxPstPerTso = maxPstPerTso;
        this.maxRaPerTso = maxRaPerTso;
        this.arePstSetpointsApproximated = arePstSetpointsApproximated;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        if ((maxRa == null || maxRa >= rangeActions.size())
            && (maxTso == null || maxTso >= rangeActions.stream().map(RemedialAction::getOperator).distinct().filter(tso -> !maxTsoExclusions.contains(tso)).count())
            && (maxPstPerTso == null || maxPstPerTso.isEmpty())
            && (maxRaPerTso == null || maxRaPerTso.isEmpty())) {
            return;
        }
        rangeActions.forEach(ra -> buildIsVariationVariableAndConstraints(linearProblem, ra));
        addMaxRaConstraint(linearProblem);
        addMaxTsoConstraint(linearProblem);
        addMaxRaPerTsoConstraint(linearProblem);
        addMaxPstPerTsoConstraint(linearProblem);
    }

    private double getAverageAbsoluteTapToAngleConversionFactor(PstRangeAction pstRangeAction) {
        int minTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().min(Integer::compareTo).orElseThrow();
        int maxTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        double minAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().min(Double::compareTo).orElseThrow();
        double maxAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().max(Double::compareTo).orElseThrow();
        return Math.abs((maxAngle - minAngle) / (maxTap - minTap));
    }

    /**
     *  Get relaxation term to add to correct the initial setpoint, to ensure problem feasibility depending on the approximations.
     *  If PSTs are modelled with approximate integers, make sure that the initial setpoint is feasible (it should be at
     *  a distance smaller then 0.3 * getAverageAbsoluteTapToAngleConversionFactor from a feasible setpoint in the MIP)
     */
    private double getInitialSetpointRelaxation(RangeAction rangeAction) {
        if (rangeAction instanceof PstRangeAction && arePstSetpointsApproximated) {
            // The BestTapFinder is accurate at 35% of the setpoint difference between 2 taps. Using 30% here to be safe.
            return 0.3 * getAverageAbsoluteTapToAngleConversionFactor((PstRangeAction) rangeAction);
        } else {
            return RANGE_ACTION_SETPOINT_EPSILON;
        }
    }

    private void buildIsVariationVariableAndConstraints(LinearProblem linearProblem, RangeAction<?> rangeAction) {
        MPVariable isVariationVariable = linearProblem.addRangeActionVariationBinary(rangeAction);
        double initialSetpoint = prePerimeterRangeActionResult.getOptimizedSetPoint(rangeAction);
        MPVariable setpointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction);
        if (setpointVariable == null) {
            throw new FaraoException(format("Range action setpoint variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double initialSetpointRelaxation = getInitialSetpointRelaxation(rangeAction);

        // range action setpoint <= intial setpoint + isVariationVariable * (max setpoint - initial setpoint)
        // RANGE_ACTION_SETPOINT_EPSILON is used to mitigate rounding issues, ensuring that the maximum setpoint is feasible
        // initialSetpointRelaxation is used to ensure that the initial setpoint is feasible
        double relaxedInitialSetpoint = initialSetpoint + initialSetpointRelaxation;
        MPConstraint constraintUp = linearProblem.addIsVariationInDirectionConstraint(-LinearProblem.infinity(), relaxedInitialSetpoint, rangeAction, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.UPWARD);
        constraintUp.setCoefficient(setpointVariable, 1);
        constraintUp.setCoefficient(isVariationVariable, -(rangeAction.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - relaxedInitialSetpoint));

        // range action setpoint >= intial setpoint - isVariationVariable * (initial setpoint - min setpoint)
        // RANGE_ACTION_SETPOINT_EPSILON is used to mitigate rounding issues, ensuring that the minimum setpoint is feasible
        // initialSetpointRelaxation is used to ensure that the initial setpoint is feasible
        relaxedInitialSetpoint = initialSetpoint - initialSetpointRelaxation;
        MPConstraint constraintDown = linearProblem.addIsVariationInDirectionConstraint(relaxedInitialSetpoint, LinearProblem.infinity(), rangeAction, LinearProblem.VariationReferenceExtension.PREPERIMETER, LinearProblem.VariationDirectionExtension.DOWNWARD);
        constraintDown.setCoefficient(setpointVariable, 1);
        constraintDown.setCoefficient(isVariationVariable, relaxedInitialSetpoint - (rangeAction.getMinAdmissibleSetpoint(initialSetpoint) - RANGE_ACTION_SETPOINT_EPSILON));
    }

    private void addMaxRaConstraint(LinearProblem linearProblem) {
        if (maxRa == null) {
            return;
        }
        MPConstraint maxRaConstraint = linearProblem.addMaxRaConstraint(0, maxRa);
        rangeActions.forEach(ra -> {
            MPVariable isVariationVariable = linearProblem.getRangeActionVariationBinary(ra);
            maxRaConstraint.setCoefficient(isVariationVariable, 1);
        });
    }

    private void addMaxTsoConstraint(LinearProblem linearProblem) {
        if (maxTso == null) {
            return;
        }
        Set<String> constraintTsos = rangeActions.stream()
            .map(RemedialAction::getOperator)
            .filter(Objects::nonNull)
            .filter(tso -> !maxTsoExclusions.contains(tso))
            .collect(Collectors.toSet());
        MPConstraint maxTsoConstraint = linearProblem.addMaxTsoConstraint(0, maxTso);
        constraintTsos.forEach(tso -> {
            // Create "is at least one RA for TSO used" binary variable ...
            MPVariable tsoRaUsedVariable = linearProblem.addTsoRaUsedVariable(0, 1, tso);
            maxTsoConstraint.setCoefficient(tsoRaUsedVariable, 1);
            // ... and the constraints that will define it
            // tsoRaUsed >= ra1_used, tsoRaUsed >= ra2_used + ...
            rangeActions.stream().filter(ra -> tso.equals(ra.getOperator()))
                .forEach(ra -> {
                    MPConstraint tsoRaUsedConstraint = linearProblem.addTsoRaUsedConstraint(0, LinearProblem.infinity(), tso, ra);
                    tsoRaUsedConstraint.setCoefficient(tsoRaUsedVariable, 1);
                    tsoRaUsedConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra), -1);
                });
        });
    }

    private void addMaxRaPerTsoConstraint(LinearProblem linearProblem) {
        if (maxRaPerTso == null) {
            return;
        }
        maxRaPerTso.forEach((tso, maxRaForTso) -> {
            MPConstraint maxRaPerTsoConstraint = linearProblem.addMaxRaPerTsoConstraint(0, maxRaForTso, tso);
            rangeActions.stream().filter(ra -> tso.equals(ra.getOperator()))
                .forEach(ra -> maxRaPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra), 1));
        });
    }

    private void addMaxPstPerTsoConstraint(LinearProblem linearProblem) {
        if (maxPstPerTso == null) {
            return;
        }
        maxPstPerTso.forEach((tso, maxPstForTso) -> {
            MPConstraint maxPstPerTsoConstraint = linearProblem.addMaxPstPerTsoConstraint(0, maxPstForTso, tso);
            rangeActions.stream().filter(ra -> ra instanceof PstRangeAction && tso.equals(ra.getOperator()))
                .forEach(ra -> maxPstPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra), 1));
        });
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        // nothing to do, we are only comparing optimal and pre-perimeter setpoints
    }
}
