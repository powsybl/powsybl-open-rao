/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LinearProblemIdGenerator {
    private static final String VARIABLE_SUFFIX = "variable";
    private static final String CONSTRAINT_SUFFIX = "constraint";
    private static final String SEPARATOR = "_";

    private static final String FLOW = "flow";
    private static final String SET_POINT = "setpoint";
    private static final String TAP_VARIATION = "tapvariation";
    private static final String TAP_VARIATION_BINARY = "isvariation";
    private static final String TAP_TO_ANGLE_CONVERSION = "taptoangleconversion";
    private static final String UP_OR_DOWN_VARIATION = "upordownvariation";
    private static final String VIRTUAL_SET_POINT = "virtualsetpoint";
    private static final String VIRTUAL_TAP = "virtualtap";
    private static final String ABSOLUTE_VARIATION = "absolutevariation";
    private static final String MIN_MARGIN = "minmargin";
    private static final String MIN_RELATIVE_MARGIN = "minrelmargin";
    private static final String MAX_LOOPFLOW = "maxloopflow";
    private static final String LOOPFLOWVIOLATION = "loopflowviolation";
    private static final String MNEC_VIOLATION = "mnecviolation";
    private static final String MNEC_FLOW = "mnecflow";
    private static final String MARGIN_DECREASE = "margindecrease";
    private static final String MAX_RA = "maxra";
    private static final String MAX_TSO = "maxtso";
    private static final String MAX_RA_PER_TSO = "maxrapertso";
    private static final String MAX_PST_PER_TSO = "maxpstpertso";
    private static final String TSO_RA_USED = "tsoraused";

    private LinearProblemIdGenerator() {
        // Should not be instantiated
    }

    public static String flowVariableId(FlowCnec cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String flowConstraintId(FlowCnec flowCnec) {
        return flowCnec.getId() + SEPARATOR + FLOW + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionSetpointVariableId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstTapVariableVariationId(RangeAction<?> rangeAction, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + TAP_VARIATION + upwardOrDownward.toString().toLowerCase() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionBinaryVariableId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + TAP_VARIATION_BINARY + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstTapBinaryVariationInDirectionId(RangeAction<?> rangeAction, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + TAP_VARIATION_BINARY + upwardOrDownward.toString().toLowerCase() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String tapToAngleConversionConstraintId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + TAP_TO_ANGLE_CONVERSION + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String upOrDownPstVariationConstraintId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + UP_OR_DOWN_VARIATION + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String isVariationConstraintId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + TAP_VARIATION_BINARY + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String isVariationInDirectionConstraintId(RangeAction<?> rangeAction, LinearProblem.VariationReferenceExtension preperimeterOrPreviousIteration, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + TAP_VARIATION_BINARY
            + SEPARATOR + preperimeterOrPreviousIteration.toString().toLowerCase()
            + SEPARATOR + upwardOrDownward.toString().toLowerCase()
            + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionGroupSetpointVariableId(String rangeActionGroupId) {
        return rangeActionGroupId + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstGroupTapVariableId(String rangeActionGroupId) {
        return rangeActionGroupId + SEPARATOR + VIRTUAL_TAP + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionGroupSetpointConstraintId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String pstGroupTapConstraintId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_TAP + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String absoluteRangeActionVariationVariableId(RangeAction<?> rangeAction) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String absoluteRangeActionVariationConstraintId(RangeAction<?> rangeAction, LinearProblem.AbsExtension positiveOrNegative) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + positiveOrNegative.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String minimumMarginConstraintId(FlowCnec flowCnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return flowCnec.getId() + SEPARATOR + MIN_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String minimumMarginVariableId() {
        return MIN_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginVariableId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginConstraintId(FlowCnec flowCnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return flowCnec.getId() + SEPARATOR + MIN_RELATIVE_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxLoopFlowConstraintId(FlowCnec flowCnec, LinearProblem.BoundExtension lbOrUb) {
        return flowCnec.getId() + SEPARATOR + MAX_LOOPFLOW + lbOrUb.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String loopflowViolationVariableId(FlowCnec flowCnec) {
        return flowCnec.getId() + SEPARATOR + LOOPFLOWVIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String mnecViolationVariableId(FlowCnec mnec) {
        return mnec.getId() + SEPARATOR + MNEC_VIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String mnecFlowConstraintId(FlowCnec mnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return mnec.getId() + SEPARATOR + MNEC_FLOW + belowOrAboveThreshold.toString().toLowerCase()  + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String marginDecreaseVariableId(FlowCnec flowCnec) {
        return flowCnec.getId() + SEPARATOR + MARGIN_DECREASE + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String marginDecreaseConstraintId(FlowCnec flowCnec, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return flowCnec.getId() + SEPARATOR + MARGIN_DECREASE + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxRaConstraintId() {
        return MAX_RA + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxTsoConstraintId() {
        return MAX_TSO + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxRaPerTsoConstraintId(String operator) {
        return MAX_RA_PER_TSO + SEPARATOR + operator + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxPstPerTsoConstraintId(String operator) {
        return MAX_PST_PER_TSO + SEPARATOR + operator + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String tsoRaUsedVariableId(String operator) {
        return TSO_RA_USED + SEPARATOR + operator + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String tsoRaUsedConstraintId(String operator, RangeAction<?> rangeAction) {
        return TSO_RA_USED + SEPARATOR + operator + SEPARATOR + rangeAction.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }
}
