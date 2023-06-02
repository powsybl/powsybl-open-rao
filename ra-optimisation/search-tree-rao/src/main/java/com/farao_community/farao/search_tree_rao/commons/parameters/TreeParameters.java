/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;

/**
 * This class contains internal FARAO parameters used in the SearchTree algorithm.
 * These parameters are dynamically generated by the SearchTreeRaoProvider depending on the context and on
 * the user's RAO parameters, and then used in SearchTree algorithm.
 * They should not be visible to the user.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class TreeParameters {

    public enum StopCriterion {
        MIN_OBJECTIVE,
        AT_TARGET_OBJECTIVE_VALUE
    }

    private final StopCriterion stopCriterion;
    private final double targetObjectiveValue;
    private final int maximumSearchDepth;
    private final int leavesInParallel;
    private final boolean pstRangeShrinking;

    public TreeParameters(StopCriterion stopCriterion,
                           double targetObjectiveValue,
                           int maximumSearchDepth,
                           int leavesInParallel,
                          boolean pstRangeShrinking) {
        this.stopCriterion = stopCriterion;
        this.targetObjectiveValue = targetObjectiveValue;
        this.maximumSearchDepth = maximumSearchDepth;
        this.leavesInParallel = leavesInParallel;
        this.pstRangeShrinking = pstRangeShrinking;
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

    public double getTargetObjectiveValue() {
        return targetObjectiveValue;
    }

    public int getMaximumSearchDepth() {
        return maximumSearchDepth;
    }

    public int getLeavesInParallel() {
        return leavesInParallel;
    }

    public boolean getPstRangeShrinking() {
        return pstRangeShrinking;
    }

    public static TreeParameters buildForPreventivePerimeter(RaoParameters parameters) {
        RangeActionsOptimizationParameters.PstRangeShrinking pstRangeShrinking = parameters.getRangeActionsOptimizationParameters().getPstRangeShrinking();
        boolean shouldShrinkPstRange = pstRangeShrinking.equals(RangeActionsOptimizationParameters.PstRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO) ||
            pstRangeShrinking.equals(RangeActionsOptimizationParameters.PstRangeShrinking.ENABLED);
        switch (parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion()) {
            case MIN_OBJECTIVE:
                return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                    0.0, // value does not matter
                    parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                    parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                    shouldShrinkPstRange);
            case SECURE:
                return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                    0.0, // secure
                    parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                    parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                    shouldShrinkPstRange);
            default:
                throw new FaraoException("Unknown preventive stop criterion: " + parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        }
    }

    public static TreeParameters buildForCurativePerimeter(RaoParameters parameters, Double preventiveOptimizedCost) {
        StopCriterion stopCriterion;
        double targetObjectiveValue;
        switch (parameters.getObjectiveFunctionParameters().getCurativeStopCriterion()) {
            case MIN_OBJECTIVE:
                stopCriterion = StopCriterion.MIN_OBJECTIVE;
                targetObjectiveValue = 0.0;
                break;
            case SECURE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = 0.0;
                break;
            case PREVENTIVE_OBJECTIVE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = preventiveOptimizedCost - parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement();
                break;
            case PREVENTIVE_OBJECTIVE_AND_SECURE:
                stopCriterion = StopCriterion.AT_TARGET_OBJECTIVE_VALUE;
                targetObjectiveValue = Math.min(preventiveOptimizedCost - parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 0);
                break;
            default:
                throw new FaraoException("Unknown curative stop criterion: " + parameters.getObjectiveFunctionParameters().getCurativeStopCriterion());
        }
        RangeActionsOptimizationParameters.PstRangeShrinking pstRangeShrinking = parameters.getRangeActionsOptimizationParameters().getPstRangeShrinking();
        boolean shouldShrinkPstRange = pstRangeShrinking.equals(RangeActionsOptimizationParameters.PstRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO) ||
            pstRangeShrinking.equals(RangeActionsOptimizationParameters.PstRangeShrinking.ENABLED);
        return new TreeParameters(stopCriterion,
            targetObjectiveValue,
                parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                parameters.getMultithreadingParameters().getCurativeLeavesInParallel(),
            shouldShrinkPstRange);
    }

    public static TreeParameters buildForSecondPreventivePerimeter(RaoParameters parameters) {
        boolean pstRangeShrinking = parameters.getRangeActionsOptimizationParameters().getPstRangeShrinking().equals(RangeActionsOptimizationParameters.PstRangeShrinking.ENABLED);
        if (parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().equals(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE)
            && !parameters.getObjectiveFunctionParameters().getCurativeStopCriterion().equals(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE)) {
            return new TreeParameters(StopCriterion.AT_TARGET_OBJECTIVE_VALUE,
                0.0, // secure
                parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                pstRangeShrinking);
        } else {
            return new TreeParameters(StopCriterion.MIN_OBJECTIVE,
                0.0, // value does not matter
                parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth(),
                parameters.getMultithreadingParameters().getPreventiveLeavesInParallel(),
                pstRangeShrinking);
        }
    }
}
