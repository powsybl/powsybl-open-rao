/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LinearProbelmIdGenerator {
    private static final String VARIABLE_SUFFIX = "variable";
    private static final String CONSTRAINT_SUFFIX = "constraint";
    private static final String SEPARATOR = "_";

    private static final String FLOW = "flow";
    private static final String SET_POINT = "setpoint";
    private static final String VIRTUAL_SET_POINT = "virtualsetpoint";
    private static final String ABSOLUTE_VARIATION = "absolutevariation";
    private static final String MIN_MARGIN = "minmargin";
    private static final String MIN_RELATIVE_MARGIN = "minrelmargin";
    private static final String MAX_LOOPFLOW = "maxloopflow";
    private static final String LOOPFLOWVIOLATION = "loopflowviolation";
    private static final String MNEC_VIOLATION = "mnecviolation";
    private static final String MNEC_FLOW = "mnecflow";
    private static final String MARGIN_DECREASE = "margindecrease";

    private LinearProbelmIdGenerator() {
        // Should not be instantiated
    }

    public static String flowVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String flowConstraintId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionSetPointVariableId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionGroupSetPointVariableId(String rangeActionGroupId) {
        return rangeActionGroupId + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionGroupSetPointConstraintId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String absoluteRangeActionVariationVariableId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String absoluteRangeActionVariationConstraintId(RangeAction rangeAction, LinearProblem.AbsExtension positiveOrNegative) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + positiveOrNegative.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String minimumMarginConstraintId(BranchCnec cnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String minimumMarginVariableId() {
        return MIN_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginVariableId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginConstraintId(BranchCnec cnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_RELATIVE_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxLoopFlowConstraintId(BranchCnec cnec, LinearProblem.BoundExtension lbOrUb) {
        return cnec.getId() + SEPARATOR + MAX_LOOPFLOW + lbOrUb.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String loopflowViolationVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + LOOPFLOWVIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String mnecViolationVariableId(BranchCnec mnec) {
        return mnec.getId() + SEPARATOR + MNEC_VIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String mnecFlowConstraintId(BranchCnec mnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return mnec.getId() + SEPARATOR + MNEC_FLOW + belowOrAboveThreshold.toString().toLowerCase()  + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String marginDecreaseVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String marginDecreaseConstraintId(BranchCnec cnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }
}
