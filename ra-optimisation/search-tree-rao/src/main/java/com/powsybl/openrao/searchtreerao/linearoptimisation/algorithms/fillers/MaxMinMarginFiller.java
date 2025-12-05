/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {
    protected final Set<FlowCnec> optimizedCnecs;
    private final Unit unit;
    private final boolean costOptimization;
    protected final OffsetDateTime timestamp;
    private final SearchTreeRaoCostlyMinMarginParameters costlyMinMarginParameters;

    public MaxMinMarginFiller(Set<FlowCnec> optimizedCnecs,
                              Unit unit, boolean costOptimization,
                              SearchTreeRaoCostlyMinMarginParameters costlyMinMarginParameters,
                              OffsetDateTime timestamp) {
        this.optimizedCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.optimizedCnecs.addAll(optimizedCnecs);
        this.unit = unit;
        this.costOptimization = costOptimization;
        this.costlyMinMarginParameters = costlyMinMarginParameters;
        this.timestamp = timestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(optimizedCnecs, sensitivityResult);

        // build variables
        buildMinimumMarginVariable(linearProblem, validFlowCnecs);
        if (costOptimization) {
            linearProblem.addMinMarginShiftedViolationVariable(Optional.ofNullable(timestamp));
        }

        // build constraints
        buildMinimumMarginConstraints(linearProblem, validFlowCnecs);
        if (costOptimization) {
            addMinMarginShiftedViolationConstraint(linearProblem);
        }

        // complete objective
        fillObjectiveWithMinMargin(linearProblem);
    }

    /**
     * Shifts the security domain of the RAO by shiftedViolationThreshold (only in costly optimization).
     * All CNECs with a margin below shiftedViolationThreshold would be considered as not-secure during linear RAO:
     * <ul>
     *     <li>if minMargin >= shiftedViolationThreshold : minMarginShiftedViolationConstraint can be at 0 to minimize objective function</li>
     *     <li>if minMargin < shiftedViolationThreshold : minMarginShiftedViolationConstraint = shiftedViolationThreshold - minimumMargin</li>
     * </ul>
     * Each unit of minMarginShiftedViolationConstraint over 0 is penalized by shiftedViolationPenalty.
     */
    private void addMinMarginShiftedViolationConstraint(LinearProblem linearProblem) {
        OpenRaoMPConstraint minMarginShiftedViolationConstraint = linearProblem.addMinMarginShiftedViolationConstraint(Optional.ofNullable(timestamp), costlyMinMarginParameters.getShiftedViolationThreshold());
        minMarginShiftedViolationConstraint.setCoefficient(linearProblem.getMinMarginShiftedViolationVariable(Optional.ofNullable(timestamp)), 1.0);
        minMarginShiftedViolationConstraint.setCoefficient(linearProblem.getMinimumMarginVariable(Optional.ofNullable(timestamp)), 1.0);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * MM represents the smallest margin of all Cnecs.
     * It is given in the objective function unit.
     */
    private void buildMinimumMarginVariable(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        if (!validFlowCnecs.isEmpty()) {
            linearProblem.addMinimumMarginVariable(-linearProblem.infinity(), linearProblem.infinity(), Optional.ofNullable(timestamp));
        } else {
            // if there is no Cnecs, the minMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumMarginVariable(0.0, 0.0, Optional.ofNullable(timestamp));
        }
    }

    /**
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definition
     * of the min margin : MM = min{c in CNEC} margin[c].
     * <p>
     * For each Cnec c, the constraints are (the max margin is defined in the objective function unit) :
     * <p>
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     * <p>
     */
    private void buildMinimumMarginConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        OpenRaoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable(Optional.ofNullable(timestamp));

        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side, Optional.ofNullable(timestamp));

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, unit);
            maxFlow = cnec.getUpperBound(side, unit);

            if (minFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), -minFlow.get(), cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.ofNullable(timestamp));
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), maxFlow.get(), cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.ofNullable(timestamp));
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        }));
    }

    /**
     * Add in the objective function of the linear problem the min Margin.
     * <ul>
     *     <li> min(-MM) for max min margin optimization</li>
     *     <li> min(shiftedViolationPenalty * MMV) for costly optimization</li>
     * </ul>
     */
    private void fillObjectiveWithMinMargin(LinearProblem linearProblem) {
        if (costOptimization) {
            linearProblem.getObjective().setCoefficient(linearProblem.getMinMarginShiftedViolationVariable(Optional.ofNullable(timestamp)), costlyMinMarginParameters.getShiftedViolationPenalty());
        } else {
            linearProblem.getObjective().setCoefficient(linearProblem.getMinimumMarginVariable(Optional.ofNullable(timestamp)), -1);
        }
    }

}
