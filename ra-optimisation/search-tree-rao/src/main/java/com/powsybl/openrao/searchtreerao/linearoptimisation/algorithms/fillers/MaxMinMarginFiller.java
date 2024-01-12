/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {
    protected final Set<FlowCnec> optimizedCnecs;
    private final Unit unit;

    public MaxMinMarginFiller(Set<FlowCnec> optimizedCnecs,
                              Unit unit) {
        this.optimizedCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.optimizedCnecs.addAll(optimizedCnecs);
        this.unit = unit;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // build variables
        buildMinimumMarginVariable(linearProblem);

        // build constraints
        buildMinimumMarginConstraints(linearProblem);

        // complete objective
        fillObjectiveWithMinMargin(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // Objective does not change, nothing to do
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * MM represents the smallest margin of all Cnecs.
     * It is given in MEGAWATT.
     */
    private void buildMinimumMarginVariable(LinearProblem linearProblem) {
        if (!optimizedCnecs.isEmpty()) {
            linearProblem.addMinimumMarginVariable(-LinearProblem.infinity(), LinearProblem.infinity());
        } else {
            // if there is no Cnecs, the minMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumMarginVariable(0.0, 0.0);
        }
    }

    /**
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definitilon
     * of the min margin : MM = min{c in CNEC} margin[c].
     * <p>
     * For each Cnec c, the constraints are (if the max margin is defined in MEGAWATT) :
     * <p>
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     * <p>
     * For each Cnec c, the constraints are (if the max margin is defined in AMPERE) :
     * <p>
     * MM <= (fmax[c] - F[c]) * 1000 / (Unom * sqrt(3))     (ABOVE_THRESHOLD)
     * MM <= (F[c] - fmin[c]) * 1000 / (Unom * sqrt(3))     (BELOW_THRESHOLD)
     */
    private void buildMinimumMarginConstraints(LinearProblem linearProblem) {
        OpenRaoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();

        optimizedCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, MEGAWATT);
            maxFlow = cnec.getUpperBound(side, MEGAWATT);
            double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnec, side, unit, MEGAWATT);

            if (minFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), -minFlow.get(), cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), maxFlow.get(), cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        }));
    }

    /**
     * Add in the objective function of the linear problem the min Margin.
     * <p>
     * min(-MM)
     */
    private void fillObjectiveWithMinMargin(LinearProblem linearProblem) {
        OpenRaoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        linearProblem.getObjective().setCoefficient(minimumMarginVariable, -1);
    }

}

