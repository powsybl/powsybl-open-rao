/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LinearProblemIdGenerator {
    private static final String VARIABLE_SUFFIX = "variable";
    private static final String CONSTRAINT_SUFFIX = "constraint";
    private static final String SEPARATOR = "_";

    private static final String FLOW = "flow";
    private static final String RELATIVE = "relative";
    private static final String SET_POINT = "setpoint";
    private static final String TAP_VARIATION = "tapvariation";
    private static final String TAP_VARIATION_BINARY = "isvariation";
    private static final String TAP_TO_ANGLE_CONVERSION = "taptoangleconversion";
    private static final String UP_OR_DOWN_VARIATION = "upordownvariation";
    private static final String VIRTUAL_SET_POINT = "virtualsetpoint";
    private static final String VIRTUAL_TAP = "virtualtap";
    private static final String ABSOLUTE_VARIATION = "absolutevariation";
    private static final String SIGNED_VARIATION = "signedvariation";
    private static final String INJECTION_BALANCE = "injectionbalance";
    private static final String MIN_MARGIN = "minmargin";
    private static final String MIN_RELATIVE_MARGIN = "minrelmargin";
    private static final String MIN_RELATIVE_MARGIN_SIGN_BINARY = "minrelmarginispositive";
    private static final String MAX_LOOPFLOW = "maxloopflow";
    private static final String LOOPFLOWVIOLATION = "loopflowviolation";
    private static final String MNEC_VIOLATION = "mnecviolation";
    private static final String MNEC_FLOW = "mnecflow";
    private static final String OPTIMIZE_CNEC = "optimizecnec";
    private static final String MAX_RA = "maxra";
    private static final String MAX_TSO = "maxtso";
    private static final String MAX_RA_PER_TSO = "maxrapertso";
    private static final String MAX_PST_PER_TSO = "maxpstpertso";
    private static final String TSO_RA_USED = "tsoraused";

    private LinearProblemIdGenerator() {
        // Should not be instantiated
    }

    public static String flowVariableId(FlowCnec flowCnec, Side side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), FLOW, VARIABLE_SUFFIX);
    }

    public static String flowConstraintId(FlowCnec flowCnec, Side side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), FLOW, CONSTRAINT_SUFFIX);
    }

    public static String rangeActionSetpointVariableId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionRelativeSetpointConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.RaRangeShrinking raRangeShrinking) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + RELATIVE + SEPARATOR + SET_POINT + SEPARATOR + raRangeShrinking.toString() + CONSTRAINT_SUFFIX;
    }

    public static String pstTapVariableVariationId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + TAP_VARIATION + upwardOrDownward.toString().toLowerCase() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionBinaryVariableId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + TAP_VARIATION_BINARY + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstTapBinaryVariationInDirectionId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + TAP_VARIATION_BINARY + upwardOrDownward.toString().toLowerCase() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String tapToAngleConversionConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + TAP_TO_ANGLE_CONVERSION + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String upOrDownPstVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + UP_OR_DOWN_VARIATION + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String isVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + TAP_VARIATION_BINARY + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String isVariationInDirectionConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.VariationReferenceExtension preperimeterOrPreviousIteration, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return rangeAction.getId() + SEPARATOR + state.getId()
            + SEPARATOR + TAP_VARIATION_BINARY
            + SEPARATOR + preperimeterOrPreviousIteration.toString().toLowerCase()
            + SEPARATOR + upwardOrDownward.toString().toLowerCase()
            + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionGroupSetpointVariableId(String rangeActionGroupId, State state) {
        return rangeActionGroupId + SEPARATOR + state.getId() + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstGroupTapVariableId(String rangeActionGroupId, State state) {
        return rangeActionGroupId + SEPARATOR + state.getId() + SEPARATOR + VIRTUAL_TAP + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionGroupSetpointConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String pstGroupTapConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_TAP + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String absoluteRangeActionVariationVariableId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + ABSOLUTE_VARIATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String signedRangeActionVariationVariableId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + SIGNED_VARIATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String absoluteRangeActionVariationConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.AbsExtension positiveOrNegative) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + ABSOLUTE_VARIATION + positiveOrNegative.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String signedRangeActionVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + SIGNED_VARIATION + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String injectionBalanceVariationConstraintId(State state, int timeStepIndex) {
        return INJECTION_BALANCE + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX + SEPARATOR + timeStepIndex;
    }

    public static String minimumMarginConstraintId(FlowCnec flowCnec, Side side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), MIN_MARGIN, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String minimumMarginVariableId() {
        return MIN_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginVariableId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginConstraintId(FlowCnec flowCnec, Side side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), MIN_RELATIVE_MARGIN, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String minimumRelativeMarginSignBinaryVariableId() {
        return MIN_RELATIVE_MARGIN_SIGN_BINARY + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelMarginSignDefinitionConstraintId() {
        return MIN_RELATIVE_MARGIN_SIGN_BINARY + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String minimumRelativeMarginSetToZeroConstraintId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxLoopFlowConstraintId(FlowCnec flowCnec, Side side, LinearProblem.BoundExtension lbOrUb) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), MAX_LOOPFLOW, lbOrUb.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String loopflowViolationVariableId(FlowCnec flowCnec, Side side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), LOOPFLOWVIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecViolationVariableId(FlowCnec mnec, Side side) {
        return String.join(SEPARATOR, mnec.getId(), side.toString().toLowerCase(), MNEC_VIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecFlowConstraintId(FlowCnec mnec, Side side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, mnec.getId(), side.toString().toLowerCase(), MNEC_FLOW, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String optimizeCnecBinaryVariableId(FlowCnec flowCnec, Side side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), OPTIMIZE_CNEC, VARIABLE_SUFFIX);
    }

    public static String dontOptimizeCnecConstraintId(FlowCnec flowCnec, Side side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), OPTIMIZE_CNEC + belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String maxRaConstraintId(State state) {
        return MAX_RA + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxTsoConstraintId(State state) {
        return MAX_TSO + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxRaPerTsoConstraintId(String operator, State state) {
        return MAX_RA_PER_TSO + SEPARATOR + operator + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String maxPstPerTsoConstraintId(String operator, State state) {
        return MAX_PST_PER_TSO + SEPARATOR + operator + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String tsoRaUsedVariableId(String operator, State state) {
        return TSO_RA_USED + SEPARATOR + operator + SEPARATOR + state.getId() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String tsoRaUsedConstraintId(String operator, RangeAction<?> rangeAction, State state) {
        return TSO_RA_USED + SEPARATOR + operator + SEPARATOR + rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }
}
