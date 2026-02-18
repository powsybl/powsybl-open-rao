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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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
    private static final String TSO_RA_USED_CUMULATIVE = "tsorausedcumulative";
    private static final String PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP = "pstabsolutevariationfrominitialtap";
    private static final String MAX_ELEMENTARY_ACTIONS_PER_TSO = "maxelementaryactionspertso";
    private static final String RANGE_ACTION_VARIATION = "rangeactionvariation";
    private static final String RANGE_ACTION_SET_POINT_VARIATION = "rangeactionsetpointvariation";
    private static final String RANGE_ACTION_ABSOLUTE_VARIATION = "rangeactionabsolutevariation";
    private static final String INJECTION_BALANCE = "injectionbalance";
    private static final String TOTAL_PST_RANGE_ACTION_TAP_VARIATION = "totalpstrangeactiontapvariation";
    private static final String GENERATOR_POWER = "generatorpower";
    private static final String GENERATOR_POWER_GRADIENT_CONSTRAINT = "generatorpowergradientconstraint";
    private static final String MIN_MARGIN_SHIFTED_VIOLATION = "minmarginshiftedviolation";
    private static final DateTimeFormatter DATE_TIME_FORMATER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private LinearProblemIdGenerator() {
        // Should not be instantiated
    }

    private static String formatName(Optional<OffsetDateTime> timestamp, String... substrings) {
        String name = String.join(SEPARATOR, substrings).replace("__", "_"); // remove empty strings
        return timestamp.map(time -> name + SEPARATOR + time.format(DATE_TIME_FORMATER)).orElse(name);
    }

    private static String formatName(String... substrings) {
        return formatName(Optional.empty(), substrings);
    }

    public static String flowVariableId(FlowCnec flowCnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), FLOW, VARIABLE_SUFFIX);
    }

    public static String flowConstraintId(FlowCnec flowCnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), FLOW, CONSTRAINT_SUFFIX);
    }

    public static String rangeActionSetpointVariableId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), SET_POINT, VARIABLE_SUFFIX);
    }

    public static String rangeActionRelativeSetpointConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.RaRangeShrinking raRangeShrinking) {
        return formatName(rangeAction.getId(), state.getId(), RELATIVE, SET_POINT, raRangeShrinking.toString(), CONSTRAINT_SUFFIX);
    }

    public static String pstRangeActionRelativeTapConstraintId(PstRangeAction pstRangeAction, State state) {
        return formatName(pstRangeAction.getId(), state.getId(), RELATIVE, TAP, CONSTRAINT_SUFFIX);
    }

    public static String pstTapVariableVariationId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return formatName(rangeAction.getId(), state.getId(), TAP_VARIATION + upwardOrDownward.toString().toLowerCase(), VARIABLE_SUFFIX);
    }

    public static String rangeActionBinaryVariableId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), TAP_VARIATION_BINARY, VARIABLE_SUFFIX);
    }

    public static String pstTapBinaryVariationInDirectionId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return formatName(rangeAction.getId(), state.getId(), TAP_VARIATION_BINARY + upwardOrDownward.toString().toLowerCase(), VARIABLE_SUFFIX);
    }

    public static String tapToAngleConversionConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), TAP_TO_ANGLE_CONVERSION, CONSTRAINT_SUFFIX);
    }

    public static String upOrDownPstVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), UP_OR_DOWN_VARIATION, CONSTRAINT_SUFFIX);
    }

    public static String isVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), TAP_VARIATION_BINARY, CONSTRAINT_SUFFIX);
    }

    public static String isVariationInDirectionConstraintId(RangeAction<?> rangeAction, State state, LinearProblem.VariationReferenceExtension preperimeterOrPreviousIteration, LinearProblem.VariationDirectionExtension upwardOrDownward) {
        return formatName(rangeAction.getId(), state.getId(), TAP_VARIATION_BINARY, preperimeterOrPreviousIteration.toString().toLowerCase(), upwardOrDownward.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String rangeActionGroupSetpointVariableId(String rangeActionGroupId, State state
    ) {
        return formatName(rangeActionGroupId, state.getId(), VIRTUAL_SET_POINT, VARIABLE_SUFFIX);
    }

    public static String pstGroupTapVariableId(String rangeActionGroupId, State state) {
        return formatName(rangeActionGroupId, state.getId(), VIRTUAL_TAP, VARIABLE_SUFFIX);
    }

    public static String rangeActionGroupSetpointConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), rangeAction.getGroupId().orElseThrow(), VIRTUAL_SET_POINT, CONSTRAINT_SUFFIX);
    }

    public static String pstGroupTapConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), rangeAction.getGroupId().orElseThrow(), VIRTUAL_TAP, CONSTRAINT_SUFFIX);
    }

    public static String absoluteRangeActionVariationVariableId(RangeAction<?> rangeAction, State state) {
        return formatName(rangeAction.getId(), state.getId(), ABSOLUTE_VARIATION, VARIABLE_SUFFIX);
    }

    public static String minimumMarginConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), MIN_MARGIN, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String minimumMarginVariableId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_MARGIN, VARIABLE_SUFFIX);
    }

    public static String minimumRelativeMarginVariableId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_RELATIVE_MARGIN, VARIABLE_SUFFIX);
    }

    public static String minimumRelativeMarginConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), MIN_RELATIVE_MARGIN, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String minimumRelativeMarginSignBinaryVariableId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_RELATIVE_MARGIN_SIGN_BINARY, VARIABLE_SUFFIX);
    }

    public static String minimumRelMarginSignDefinitionConstraintId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_RELATIVE_MARGIN_SIGN_BINARY, CONSTRAINT_SUFFIX);
    }

    public static String minimumRelativeMarginSetToZeroConstraintId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_RELATIVE_MARGIN, CONSTRAINT_SUFFIX);
    }

    public static String maxLoopFlowConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.BoundExtension lbOrUb, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), MAX_LOOPFLOW, lbOrUb.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String loopflowViolationVariableId(FlowCnec flowCnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), LOOPFLOWVIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecViolationVariableId(FlowCnec mnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, mnec.getId(), side.toString().toLowerCase(), MNEC_VIOLATION, VARIABLE_SUFFIX);
    }

    public static String mnecFlowConstraintId(FlowCnec mnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, mnec.getId(), side.toString().toLowerCase(), MNEC_FLOW, belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String optimizeCnecBinaryVariableId(FlowCnec flowCnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), OPTIMIZE_CNEC, VARIABLE_SUFFIX);
    }

    public static String dontOptimizeCnecConstraintId(FlowCnec flowCnec, TwoSides side, LinearProblem.MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, flowCnec.getId(), side.toString().toLowerCase(), OPTIMIZE_CNEC + belowOrAboveThreshold.toString().toLowerCase(), CONSTRAINT_SUFFIX);
    }

    public static String maxRaConstraintId(State state) {
        return formatName(MAX_RA, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String maxTsoConstraintId(State state) {
        return formatName(MAX_TSO, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String maxRaPerTsoConstraintId(String operator, State state) {
        return formatName(MAX_RA_PER_TSO, operator, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String maxPstPerTsoConstraintId(String operator, State state) {
        return formatName(MAX_PST_PER_TSO, operator, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String tsoRaUsedVariableId(String operator, State state) {
        return formatName(TSO_RA_USED, operator, state.getId(), VARIABLE_SUFFIX);
    }

    public static String tsoRaUsedCumulativeVariableId(String operator, State state) {
        return formatName(TSO_RA_USED_CUMULATIVE, operator, state.getId(), VARIABLE_SUFFIX);
    }

    public static String tsoRaUsedConstraintId(String operator, RangeAction<?> rangeAction, State state) {
        return formatName(TSO_RA_USED, operator, rangeAction.getId(), state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String pstAbsoluteVariationFromInitialTapVariableId(PstRangeAction pstRangeAction, State state) {
        return formatName(PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP, pstRangeAction.getId(), state.getId(), VARIABLE_SUFFIX);
    }

    public static String pstAbsoluteVariationFromInitialTapConstraintId(PstRangeAction pstRangeAction, State state, LinearProblem.AbsExtension positiveOrNegative) {
        return formatName(PST_ABSOLUTE_VARIATION_FROM_INITIAL_TAP, pstRangeAction.getId(), state.getId(), CONSTRAINT_SUFFIX, positiveOrNegative.toString());
    }

    public static String maxElementaryActionsPerTsoConstraintId(String operator, State state) {
        return formatName(MAX_ELEMENTARY_ACTIONS_PER_TSO, operator, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String rangeActionVariationVariableId(RangeAction<?> rangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection) {
        return formatName(RANGE_ACTION_VARIATION, rangeAction.getId(), state.getId(), VARIABLE_SUFFIX, variationDirection.toString());
    }

    public static String rangeActionSetPointVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(RANGE_ACTION_SET_POINT_VARIATION, rangeAction.getId(), state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String rangeActionAbsoluteVariationConstraintId(RangeAction<?> rangeAction, State state) {
        return formatName(RANGE_ACTION_ABSOLUTE_VARIATION, rangeAction.getId(), state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String injectionBalanceConstraintId(State state) {
        return formatName(INJECTION_BALANCE, state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String totalPstRangeActionTapVariationVariableId(PstRangeAction pstRangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection) {
        return formatName(TOTAL_PST_RANGE_ACTION_TAP_VARIATION, pstRangeAction.getId(), state.getId(), VARIABLE_SUFFIX, variationDirection.toString());
    }

    public static String totalPstRangeActionTapVariationConstraintId(PstRangeAction pstRangeAction, State state) {
        return formatName(TOTAL_PST_RANGE_ACTION_TAP_VARIATION, pstRangeAction.getId(), state.getId() + SEPARATOR + CONSTRAINT_SUFFIX);
    }

    public static String tapVariableId(PstRangeAction pstRangeAction, State state) {
        return formatName(TAP + SEPARATOR + pstRangeAction.getId(), state.getId(), VARIABLE_SUFFIX);
    }

    public static String tapConstraintId(PstRangeAction pstRangeAction, State state) {
        return formatName(TAP + SEPARATOR + pstRangeAction.getId(), state.getId(), CONSTRAINT_SUFFIX);
    }

    public static String generatorPowerVariableId(String generatorId, OffsetDateTime timestamp) {
        return formatName(Optional.ofNullable(timestamp), GENERATOR_POWER, generatorId, VARIABLE_SUFFIX);
    }

    public static String generatorPowerConstraintId(String generatorId, OffsetDateTime timestamp) {
        return formatName(Optional.ofNullable(timestamp), GENERATOR_POWER, generatorId, CONSTRAINT_SUFFIX);
    }

    public static String generatorPowerGradientConstraintId(String generatorId, OffsetDateTime currentTimestamp, OffsetDateTime previousTimestamp) {
        return formatName(Optional.empty(), GENERATOR_POWER_GRADIENT_CONSTRAINT, generatorId, currentTimestamp.format(DATE_TIME_FORMATER), previousTimestamp.format(DATE_TIME_FORMATER), CONSTRAINT_SUFFIX);
    }

    public static String minMarginShiftedViolationVariableId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_MARGIN_SHIFTED_VIOLATION, VARIABLE_SUFFIX);
    }

    public static String minMarginShiftedViolationConstraintId(Optional<OffsetDateTime> timestamp) {
        return formatName(timestamp, MIN_MARGIN_SHIFTED_VIOLATION, CONSTRAINT_SUFFIX);
    }
}
