/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_SENSITIVITY_THRESHOLD;
import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller implements ProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProblemFiller.class);

    private final double pstSensitivityThreshold;
    private final Map<String, Integer> maxPstPerTso;
    private Set<RangeAction> availableRangeActions;
    private LinearOptimizerInput linearOptimizerInput;
    private LinearProblem linearProblem;

    public CoreProblemFiller(LinearProblem linearProblem, LinearOptimizerInput linearOptimizerInput, double pstSensitivityThreshold, Map<String, Integer> maxPstPerTso) {
        this.linearProblem = linearProblem;
        this.linearOptimizerInput = linearOptimizerInput;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        this.maxPstPerTso = maxPstPerTso;
    }

    // Method for tests
    public CoreProblemFiller() {
        this(null, null, DEFAULT_PST_SENSITIVITY_THRESHOLD, null);
    }

    @Override
    public void fill(SensitivityAndLoopflowResults sensitivityResult) {
        availableRangeActions = computeAvailableRangeActions(sensitivityResult.getSystematicSensitivityResult());
        // add variables
        buildFlowVariables();
        availableRangeActions.forEach(rangeAction -> {
            double prePerimeterSetpoint = linearOptimizerInput.getPreperimeterSetpoints().get(rangeAction);
            buildRangeActionSetPointVariables(linearOptimizerInput.getNetwork(), rangeAction, prePerimeterSetpoint);
            buildRangeActionAbsoluteVariationVariables(rangeAction);
            buildRangeActionGroupConstraint(rangeAction);
        });

        // add constraints
        buildFlowConstraints(sensitivityResult.getSystematicSensitivityResult());
        buildRangeActionConstraints();
    }

    @Override
    public void update(SensitivityAndLoopflowResults sensitivityResult) {
        // update reference flow and sensitivities of flow constraints
        updateFlowConstraints(sensitivityResult.getSystematicSensitivityResult());
    }

    /**
     * Filters out range actions that should not be used in the optimization, even if they are available in the perimeter
     */
    private Set<RangeAction> computeAvailableRangeActions(SystematicSensitivityResult sensitivityResult) {
        Set<RangeAction> rangeActions = removeRangeActionsWithWrongInitialSetpoint(linearOptimizerInput.getRangeActions(),
                linearOptimizerInput.getPreperimeterSetpoints(), linearOptimizerInput.getNetwork());
        rangeActions = removeRangeActionsIfMaxNumberReached(sensitivityResult, rangeActions, linearOptimizerInput.getMostLimitingElement(), maxPstPerTso);
        return rangeActions;
    }

    /**
     * If range action's initial setpoint does not respect its allowed range, this function filters it out
     */
    private static Set<RangeAction> removeRangeActionsWithWrongInitialSetpoint(Set<RangeAction> rangeActionsToFilter, Map<RangeAction, Double> initialSetpoints, Network network) {
        Set<RangeAction> filteredRangeActions = new HashSet<>(rangeActionsToFilter);
        rangeActionsToFilter.stream().forEach(rangeAction -> {
            double preperimeterSetPoint = initialSetpoints.get(rangeAction);
            double minSetPoint = rangeAction.getMinValue(network, preperimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxValue(network, preperimeterSetPoint);
            if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
                LOGGER.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                        rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
                filteredRangeActions.remove(rangeAction);
            }
        });
        return filteredRangeActions;
    }

    /**
     * If a TSO has a maximum number of usable ranges actions, this functions filters out the range actions with
     * the least impact on the most limiting element
     */
    private static Set<RangeAction> removeRangeActionsIfMaxNumberReached(SystematicSensitivityResult sensitivityResult, Set<RangeAction> rangeActionsToFilter, BranchCnec mostLimitingElement, Map<String, Integer> maxPstPerTso) {
        Set<RangeAction> filteredRangeActions = new HashSet<>(rangeActionsToFilter);
        if (!Objects.isNull(maxPstPerTso) && !maxPstPerTso.isEmpty()) {
            maxPstPerTso.forEach((String tso, Integer maxPst) -> {
                Set<RangeAction> pstsForTso = rangeActionsToFilter.stream()
                        .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && rangeAction.getOperator().equals(tso))
                        .collect(Collectors.toSet());
                if (pstsForTso.size() > maxPst) {
                    LOGGER.debug("{} range actions will be filtered out, in order to respect the maximum number of range actions of {} for TSO {}", pstsForTso.size() - maxPst, maxPst, tso);
                    pstsForTso.stream().sorted((ra1, ra2) -> compareAbsoluteSensitivities(ra1, ra2, mostLimitingElement, sensitivityResult))
                            .collect(Collectors.toList()).subList(0, pstsForTso.size() - maxPst)
                            .forEach(filteredRangeActions::remove);
                }
            });
        }
        return filteredRangeActions;
    }

    static int compareAbsoluteSensitivities(RangeAction ra1, RangeAction ra2, BranchCnec cnec, SystematicSensitivityResult sensitivityResult) {
        Double sensi1 = Math.abs(sensitivityResult.getSensitivityOnFlow(ra1, cnec));
        Double sensi2 = Math.abs(sensitivityResult.getSensitivityOnFlow(ra2, cnec));
        return sensi1.compareTo(sensi2);
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables() {
        linearOptimizerInput.getCnecs().forEach(cnec ->
                linearProblem.addFlowVariable(-linearProblem.infinity(), linearProblem.infinity(), cnec)
        );
    }

    /**
     * Build one set point variable S[r] for each RangeAction r
     * This variable describes the set point of the given RangeAction r, given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     * </ul>
     *
     * This set point of the a RangeAction is bounded between the min/max variations
     * of the RangeAction :
     *
     * initialSetPoint[r] - maxNegativeVariation[r] <= S[r]
     * S[r] >= initialSetPoint[r] + maxPositiveVariation[r]
     */
    private void buildRangeActionSetPointVariables(Network network, RangeAction rangeAction, double prePerimeterValue) {
        double minSetPoint = rangeAction.getMinValue(network, prePerimeterValue);
        double maxSetPoint = rangeAction.getMaxValue(network, prePerimeterValue);
        linearProblem.addRangeActionSetPointVariable(minSetPoint, maxSetPoint, rangeAction);
    }

    /**
     * Build one absolute variable variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action set point
     * and its initial value. It is given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     * </ul>
     */
    private void buildRangeActionAbsoluteVariationVariables(RangeAction rangeAction) {
        linearProblem.addAbsoluteRangeActionVariationVariable(0, linearProblem.infinity(), rangeAction);
    }

    /**
     * Build one flow constraint for each Cnec c.
     * This constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(SystematicSensitivityResult sensitivityResult) {
        linearOptimizerInput.getCnecs().forEach(cnec -> {
            // create constraint
            double referenceFlow = sensitivityResult.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec);

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(sensitivityResult, cnec);
        });
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints(SystematicSensitivityResult sensitivityResult) {
        linearOptimizerInput.getCnecs().forEach(cnec -> {
            double referenceFlow = sensitivityResult.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);
            if (flowConstraint == null) {
                throw new FaraoException(format("Flow constraint on %s has not been defined yet.", cnec.getId()));
            }

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(sensitivityResult, cnec);
        });
    }

    private void addImpactOfRangeActionOnCnec(SystematicSensitivityResult sensitivityResult, Cnec<?> cnec) {
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        availableRangeActions.forEach(rangeAction -> {
            if (rangeAction instanceof PstRangeAction) {
                addImpactOfPstOnCnec(sensitivityResult, rangeAction, cnec, flowConstraint);
            } else {
                throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
            }
        });
    }

    private void addImpactOfPstOnCnec(SystematicSensitivityResult sensitivityResult, RangeAction rangeAction, Cnec<?> cnec, MPConstraint flowConstraint) {
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        if (setPointVariable == null) {
            throw new FaraoException(format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double sensitivity = sensitivityResult.getSensitivityOnFlow(rangeAction, cnec);

        if (Math.abs(sensitivity) >= pstSensitivityThreshold) {
            double currentSetPoint = rangeAction.getCurrentValue(linearOptimizerInput.getNetwork());
            // care : might not be robust as getCurrentValue get the current setPoint from a network variant
            //        we need to be sure that this variant has been properly set
            flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -sensitivity);
        }
    }

    /**
     * Build two range action constraints for each RangeAction r.
     * These constraints link the set point variable of the RangeAction with its absolute
     * variation variable.
     *
     * AV[r] >= S[r] - initialSetPoint[r]     (NEGATIVE)
     * AV[r] >= initialSetPoint[r] - S[r]     (POSITIVE)
     */
    private void buildRangeActionConstraints() {
        availableRangeActions.forEach(rangeAction -> {
            double preperimeterSetPoint = linearOptimizerInput.getPreperimeterSetpoints().get(rangeAction);
            MPConstraint varConstraintNegative = linearProblem.addAbsoluteRangeActionVariationConstraint(-preperimeterSetPoint, linearProblem.infinity(), rangeAction, LinearProblem.AbsExtension.NEGATIVE);
            MPConstraint varConstraintPositive = linearProblem.addAbsoluteRangeActionVariationConstraint(preperimeterSetPoint, linearProblem.infinity(), rangeAction, LinearProblem.AbsExtension.POSITIVE);

            MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);

            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
        });
    }

    private void buildRangeActionGroupConstraint(RangeAction rangeAction) {
        Optional<String> optGroupId = rangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for set point has to be created
            if (linearProblem.getRangeActionGroupSetPointVariable(groupId) == null) {
                linearProblem.addRangeActionGroupSetPointVariable(-linearProblem.infinity(), linearProblem.infinity(), groupId);
            }
            addRangeActionGroupConstraint(rangeAction, groupId);
        }
    }

    private void addRangeActionGroupConstraint(RangeAction rangeAction, String groupId) {
        MPConstraint groupSetPointConstraint = linearProblem.addRangeActionGroupSetPointConstraint(0, 0, rangeAction);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionSetPointVariable(rangeAction), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionGroupSetPointVariable(groupId), -1);
    }
}
