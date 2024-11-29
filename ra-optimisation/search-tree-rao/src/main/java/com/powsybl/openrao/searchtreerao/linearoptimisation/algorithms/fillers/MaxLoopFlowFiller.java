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
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller implements ProblemFiller {
    private final Set<FlowCnec> loopFlowCnecs;
    private final FlowResult initialFlowResult;
    private final PtdfApproximation loopFlowPtdfApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;
    private FlowResult preOptimFlowResult; // = flow result used in the first "fill" iteration

    public MaxLoopFlowFiller(Set<FlowCnec> loopFlowCnecs, FlowResult initialFlowResult, LoopFlowParameters loopFlowParameters, com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters loopFlowParametersExtension) {
        this.loopFlowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.loopFlowCnecs.addAll(FillersUtil.getFlowCnecsNotNaNFlow(loopFlowCnecs, initialFlowResult));
        this.initialFlowResult = initialFlowResult;
        this.loopFlowPtdfApproximationLevel = loopFlowParametersExtension.getPtdfApproximation();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getAcceptableIncrease();
        this.loopFlowViolationCost = loopFlowParametersExtension.getViolationCost();
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowParametersExtension.getConstraintAdjustmentCoefficient();
    }

    private Set<FlowCnec> getValidLoopFlowCnecs(SensitivityResult sensitivityResult) {
        return FillersUtil.getFlowCnecsComputationStatusOk(loopFlowCnecs, sensitivityResult);
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        if (preOptimFlowResult == null) {
            preOptimFlowResult = flowResult;
        }
        FlowResult flowResultToUse = loopFlowPtdfApproximationLevel.shouldUpdatePtdfWithPstChange() ? flowResult : preOptimFlowResult;
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(linearProblem, getValidLoopFlowCnecs(sensitivityResult), flowResultToUse);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    /**
     * currentLoopflow = flowVariable - PTDF * NetPosition, where flowVariable a OpenRaoMPVariable
     * Constraint for loopflow in optimization is: - MaxLoopFlow <= currentLoopFlow <= MaxLoopFlow,
     * where MaxLoopFlow is calculated as
     * max(Input TSO loopflow limit, Loopflow value computed from initial network)
     * <p>
     * let CommercialFlow = PTDF * NetPosition, then
     * - MaxLoopFlow + CommercialFlow <= flowVariable <= MaxLoopFlow + CommercialFlow
     * <p>
     * Loopflow limit may be tuned by a "Loopflow adjustment coefficient":
     * MaxLoopFlow = Loopflow constraint - Loopflow adjustment coefficient
     * <p>
     * An additional OpenRaoMPVariable "loopflowViolationVariable" may be added when "Loopflow violation cost" is not zero:
     * - (MaxLoopFlow + loopflowViolationVariable) <= currentLoopFlow <= MaxLoopFlow + loopflowViolationVariable
     * equivalent to 2 constraints:
     * - MaxLoopFlow <= currentLoopFlow + loopflowViolationVariable
     * currentLoopFlow - loopflowViolationVariable <= MaxLoopFlow
     * or:
     * - MaxLoopFlow + CommercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
     * NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + CommercialFlow
     * and a "virtual cost" is added to objective function as "loopflowViolationVariable * Loopflow violation cost"
     */
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(LinearProblem linearProblem, Set<FlowCnec> validLoopFlowCnecs, FlowResult flowResult) {
        for (FlowCnec cnec : validLoopFlowCnecs) {
            for (TwoSides side : cnec.getMonitoredSides()) {

                // build loopFlow upper bound, with inputThreshold, initial loop-flows, and configuration parameters
                double loopFlowUpperBound = getLoopFlowUpperBound(cnec, side);
                if (loopFlowUpperBound == Double.POSITIVE_INFINITY) {
                    continue;
                }

                // get loop-flow variable
                OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

                OpenRaoMPVariable loopflowViolationVariable = linearProblem.addLoopflowViolationVariable(
                    0,
                    linearProblem.infinity(),
                    cnec,
                    side
                );

                // build constraint which defines the loopFlow :
                // - MaxLoopFlow + commercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
                // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + commercialFlow
                // loopflowViolationVariable is divided by number of monitored sides to not increase its effect on the objective function

                OpenRaoMPConstraint positiveLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -loopFlowUpperBound + flowResult.getCommercialFlow(cnec, side, Unit.MEGAWATT),
                    linearProblem.infinity(),
                    cnec,
                    side,
                    LinearProblem.BoundExtension.LOWER_BOUND
                );
                positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
                positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1.0);

                OpenRaoMPConstraint negativeLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -linearProblem.infinity(),
                    loopFlowUpperBound + flowResult.getCommercialFlow(cnec, side, Unit.MEGAWATT),
                    cnec,
                    side,
                    LinearProblem.BoundExtension.UPPER_BOUND
                );
                negativeLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
                negativeLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, -1);

                //update objective function with loopflowViolationCost
                linearProblem.getObjective().setCoefficient(loopflowViolationVariable, loopFlowViolationCost / cnec.getMonitoredSides().size());
            }
        }
    }

    private double getLoopFlowUpperBound(FlowCnec loopFlowCnec, TwoSides side) {
        double loopFlowThreshold = loopFlowCnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialFlowResult.getLoopFlow(loopFlowCnec, side, Unit.MEGAWATT);
        // The first term ensures that the initial situation is always feasible, whatever the configuration parameters.
        // A tiny bit of slack (0.01) has been added to the threshold to avoid the rounding causing infeasibility.
        return Math.max(Math.abs(initialLoopFlow),
            Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation) - loopFlowConstraintAdjustmentCoefficient) + 0.01;
    }
}
