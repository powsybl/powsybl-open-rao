/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionsCostFiller implements ProblemFiller {
    private final OptimizationPerimeter optimizationPerimeter;
    private final RangeActionsOptimizationParameters rangeActionsOptimizationParameters;

    public RemedialActionsCostFiller(OptimizationPerimeter optimizationPerimeter, RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.optimizationPerimeter = optimizationPerimeter;
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        State optimizationState = optimizationPerimeter.getMainOptimizationState();
        OpenRaoMPVariable costVariable = linearProblem.addRemedialActionsCostVariable(optimizationState);
        OpenRaoMPConstraint costConstraint = linearProblem.addRemedialActionsCostConstraint(optimizationState);
        costConstraint.setCoefficient(costVariable, 1d);
        optimizationPerimeter.getRangeActionsPerState().getOrDefault(optimizationState, Set.of()).forEach(
            rangeAction -> {
                costConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(rangeAction, LinearProblem.VariationDirectionExtension.UPWARD, optimizationState), -rangeAction.getActivationCost().orElse(0d));
                costConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(rangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD, optimizationState), -rangeAction.getActivationCost().orElse(0d));
                costConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariationVariable(rangeAction, LinearProblem.VariationDirectionExtension.UPWARD, optimizationState), -getVariationCost(rangeAction, RangeAction.VariationDirection.UP));
                costConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariationVariable(rangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD, optimizationState), -getVariationCost(rangeAction, RangeAction.VariationDirection.DOWN));
            }
        );
        linearProblem.getObjective().setCoefficient(costVariable, 1);
    }

    private double getVariationCost(RangeAction<?> rangeAction, RangeAction.VariationDirection variationDirection) {
        if (rangeAction.getVariationCost(variationDirection).isPresent()) {
            return rangeAction.getVariationCost(variationDirection).get();
        } else if (rangeAction instanceof InjectionRangeAction) {
            return rangeActionsOptimizationParameters.getInjectionRaPenaltyCost();
        } else if (rangeAction instanceof HvdcRangeAction) {
            return rangeActionsOptimizationParameters.getHvdcPenaltyCost();
        } else if (rangeAction instanceof PstRangeAction) {
            return rangeActionsOptimizationParameters.getPstPenaltyCost();
        } else {
            return 0d; // TODO: define parameter for CT
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to update
    }
}
