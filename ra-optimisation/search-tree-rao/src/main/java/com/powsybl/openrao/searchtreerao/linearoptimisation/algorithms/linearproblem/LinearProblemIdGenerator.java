/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

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

    private static final String TAP = "tap";
    private static final String TAP_VARIATION = "tapvariation";
    private static final String TAP_VARIATION_BINARY = "isvariation";
    private static final String TAP_TO_ANGLE_CONVERSION = "taptoangleconversion";
    private static final String UP_OR_DOWN_VARIATION = "upordownvariation";
    private static final String VIRTUAL_SET_POINT = "virtualsetpoint";
    private static final String VIRTUAL_TAP = "virtualtap";
    private static final String ABSOLUTE_VARIATION = "absolutevariation";
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
    private static final String PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP = "pstabsolutevariationfrominitialtap";
    private static final String MAX_ELEMENTARY_ACTIONS_PER_TSO = "maxelementaryactionspertso";
    private static final String RANGE_ACTION_VARIATION = "rangeactionvariation";
    private static final String RANGE_ACTION_ACTIVATION = "rangeactionactivation";
    private static final String RANGE_ACTION_SET_POINT_VARIATION = "rangeactionsetpointvariation";
    private static final String RANGE_ACTION_ABSOLUTE_VARIATION = "rangeactionabsolutevariation";

    private LinearProblemIdGenerator() {
        // Should not be instantiated
    }

    public static String flowVariableId(FlowCnec flowCnec, TwoSides side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), FLOW, VARIABLE_SUFFIX);
    }

    public static String flowConstraintId(FlowCnec flowCnec, TwoSides side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), FLOW, CONSTRAINT_SUFFIX);
    }

    public static String rangeActionSetpointVariableId(RangeAction<?> rangeAction, State state) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String rangeActionRelativeSetpointConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.RaRangeShrinking raRangeShrinking) {
        return rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + RELATIVE + SEPARATOR + SET_POINT + SEPARATOR + raRangeShrinking.toString() + CONSTRAINT_SUFFIX;
    }

    public static String pstRangeActionRelativeTapConstraintId(PstRangeAction pstRangeAction, State state) {
        return pstRangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + RELATIVE + SEPARATOR + TAP + SEPARATOR + CONSTRAINT_SUFFIX;
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

    public static String minimumMarginConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), MIN_MARGIN, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String minimumMarginVariableId() {
        return MIN_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginVariableId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String minimumRelativeMarginConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold) {
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

    public static String maxLoopFlowConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.BoundExtension lbOrUb) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), MAX_LOOPFLOW, lbOrUb.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String loopflowViolationVariableId(FlowCnec flowCnec, TwoSides side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), LOOPFLOWVIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecViolationVariableId(FlowCnec mnec, TwoSides side) {
        return String.join(SEPARATOR, mnec.getId(), side.toString().toLowerCase(), MNEC_VIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecFlowConstraintId(FlowCnec mnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold) {
        return String.join(SEPARATOR, mnec.getId(), side.toString().toLowerCase(), MNEC_FLOW, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String optimizeCnecBinaryVariableId(FlowCnec flowCnec, TwoSides side) {
        return String.join(SEPARATOR, flowCnec.getId(), side.toString().toLowerCase(), OPTIMIZE_CNEC, VARIABLE_SUFFIX);
    }

    public static String dontOptimizeCnecConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold) {
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

    public static String pstAbsoluteVariationFromInitialTapVariableId(PstRangeAction pstRangeAction, State state) {
        return PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP + SEPARATOR + pstRangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + VARIABLE_SUFFIX;
    }

    public static String pstAbsoluteVariationFromInitialTapConstraintId(PstRangeAction pstRangeAction, State state, LinearProblem.AbsExtension positiveOrNegative) {
        return PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP + SEPARATOR + pstRangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX + SEPARATOR + positiveOrNegative;
    }

    public static String maxElementaryActionsPerTsoConstraintId(String operator, State state) {
        return MAX_ELEMENTARY_ACTIONS_PER_TSO + SEPARATOR + operator + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionVariationVariableId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection) {
        return RANGE_ACTION_VARIATION + SEPARATOR + rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + VARIABLE_SUFFIX + SEPARATOR + variationDirection;
    }

    public static String rangeActionSetPointVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return RANGE_ACTION_SET_POINT_VARIATION + SEPARATOR + rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public static String rangeActionAbsoluteVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return RANGE_ACTION_ABSOLUTE_VARIATION + SEPARATOR + rangeAction.getId() + SEPARATOR + state.getId() + SEPARATOR + CONSTRAINT_SUFFIX;
    }
}
