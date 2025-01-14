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
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
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

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecFiller implements ProblemFiller {
    private final FlowResult initialFlowResult;
    private final Set<FlowCnec> monitoredCnecs;
    private final Unit unit;
    private final double mnecViolationCost;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecConstraintAdjustmentCoefficient;
    private final OffsetDateTime timestamp;

    public MnecFiller(FlowResult initialFlowResult, Set<FlowCnec> monitoredCnecs, Unit unit, MnecParametersExtension mnecParameters, OffsetDateTime timestamp) {
        this.initialFlowResult = initialFlowResult;
        this.monitoredCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.monitoredCnecs.addAll(FillersUtil.getFlowCnecsNotNaNFlow(monitoredCnecs, initialFlowResult));
        this.unit = unit;
        this.mnecViolationCost = mnecParameters.getViolationCost();
        this.mnecAcceptableMarginDecrease = mnecParameters.getAcceptableMarginDecrease();
        this.mnecConstraintAdjustmentCoefficient = mnecParameters.getConstraintAdjustmentCoefficient();
        this.timestamp = timestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        Set<FlowCnec> validMonitoredCnecs = FillersUtil.getFlowCnecsComputationStatusOk(monitoredCnecs, sensitivityResult);
        buildMarginViolationVariable(linearProblem, validMonitoredCnecs);
        buildMnecMarginConstraints(linearProblem, validMonitoredCnecs);
        fillObjectiveWithMnecPenaltyCost(linearProblem, validMonitoredCnecs);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    private void buildMarginViolationVariable(LinearProblem linearProblem, Set<FlowCnec> validMonitoredCnecs) {
        validMonitoredCnecs.forEach(mnec -> mnec.getMonitoredSides().forEach(side ->
            linearProblem.addMnecViolationVariable(0, linearProblem.infinity(), mnec, side, Optional.ofNullable(timestamp))
        ));
    }

    private void buildMnecMarginConstraints(LinearProblem linearProblem, Set<FlowCnec> validMonitoredCnecs) {
        validMonitoredCnecs.forEach(mnec -> mnec.getMonitoredSides().forEach(side -> {
                double mnecInitialFlowInMW = initialFlowResult.getFlow(mnec, side, unit) * RaoUtil.getFlowUnitMultiplier(mnec, side, unit, MEGAWATT);

                OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(mnec, side, Optional.ofNullable(timestamp));
                OpenRaoMPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(mnec, side, Optional.ofNullable(timestamp));

                Optional<Double> maxFlow = mnec.getUpperBound(side, MEGAWATT);
                if (maxFlow.isPresent()) {
                    double ub = Math.max(maxFlow.get(), mnecInitialFlowInMW + mnecAcceptableMarginDecrease) - mnecConstraintAdjustmentCoefficient;
                    OpenRaoMPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(-linearProblem.infinity(), ub, mnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.ofNullable(timestamp));
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, -1);
                }

                Optional<Double> minFlow = mnec.getLowerBound(side, MEGAWATT);
                if (minFlow.isPresent()) {
                    double lb = Math.min(minFlow.get(), mnecInitialFlowInMW - mnecAcceptableMarginDecrease) + mnecConstraintAdjustmentCoefficient;
                    OpenRaoMPConstraint maxConstraint = linearProblem.addMnecFlowConstraint(lb, linearProblem.infinity(), mnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.ofNullable(timestamp));
                    maxConstraint.setCoefficient(flowVariable, 1);
                    maxConstraint.setCoefficient(mnecViolationVariable, 1);
                }
            }
        ));
    }

    public void fillObjectiveWithMnecPenaltyCost(LinearProblem linearProblem, Set<FlowCnec> validMonitoredCnecs) {
        validMonitoredCnecs.stream().filter(FlowCnec::isMonitored).forEach(mnec ->
            mnec.getMonitoredSides().forEach(side ->
            linearProblem.getObjective().setCoefficient(linearProblem.getMnecViolationVariable(mnec, side, Optional.ofNullable(timestamp)),
                    RaoUtil.getFlowUnitMultiplier(mnec, side, MEGAWATT, unit) * mnecViolationCost / mnec.getMonitoredSides().size())
            ));
    }
}
