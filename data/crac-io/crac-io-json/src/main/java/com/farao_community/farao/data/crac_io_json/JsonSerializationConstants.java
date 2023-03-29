/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class JsonSerializationConstants {

    private JsonSerializationConstants() {
    }

    public static final String CRAC_IO_VERSION = "1.5";
    /*
    v1.1: addition of switchPairs
    v1.2: addition of injectionRangeAction
    v1.3: addition of hvdcRangeAction's and injectionRangeAction's initial setpoints
    v1.4: addition of AngleCnecs; frm renamed to reliabilityMargin
    v1.5: addition of VoltageCnecs
     */

    // headers
    public static final String TYPE = "type";
    public static final String VERSION = "version";
    public static final String INFO = "info";
    public static final String CRAC_TYPE = "CRAC";
    public static final String CRAC_INFO = "Generated by FARAO http://farao-community.github.io";

    // field
    public static final String NETWORK_ELEMENTS_IDS = "networkElementsIds";
    public static final String NETWORK_ELEMENT_ID = "networkElementId";
    public static final String EXPORTING_NETWORK_ELEMENT_ID = "exportingNetworkElementId";
    public static final String IMPORTING_NETWORK_ELEMENT_ID = "importingNetworkElementId";
    public static final String NETWORK_ELEMENTS_NAME_PER_ID = "networkElementsNamePerId";
    public static final String NETWORK_ELEMENT_IDS_AND_KEYS = "networkElementIdsAndKeys";

    public static final String GROUP_ID = "groupId";
    public static final String SPEED = "speed";

    public static final String CONTINGENCIES = "contingencies";
    public static final String CONTINGENCY_ID = "contingencyId";

    public static final String INSTANT = "instant";

    public static final String FLOW_CNECS = "flowCnecs";
    public static final String FLOW_CNEC_ID = "flowCnecId";

    public static final String ANGLE_CNECS = "angleCnecs";
    public static final String ANGLE_CNEC_ID = "angleCnecId";

    public static final String VOLTAGE_CNECS = "voltageCnecs";

    public static final String THRESHOLDS = "thresholds";
    public static final String RELIABILITY_MARGIN = "reliabilityMargin";
    public static final String FRM = "frm";
    public static final String OPTIMIZED = "optimized";
    public static final String MONITORED = "monitored";
    public static final String I_MAX = "iMax";
    public static final String NOMINAL_VOLTAGE = "nominalV";

    public static final String PST_RANGE_ACTIONS = "pstRangeActions";
    public static final String HVDC_RANGE_ACTIONS = "hvdcRangeActions";
    public static final String INJECTION_RANGE_ACTIONS = "injectionRangeActions";

    public static final String NETWORK_ACTIONS = "networkActions";
    public static final String TOPOLOGICAL_ACTIONS = "topologicalActions";
    public static final String PST_SETPOINTS = "pstSetpoints";
    public static final String INJECTION_SETPOINTS = "injectionSetpoints";
    public static final String SWITCH_PAIRS = "switchPairs";

    public static final String USAGE_METHOD = "usageMethod";
    public static final String ON_INSTANT_USAGE_RULES = "onInstantUsageRules";
    public static final String ON_STATE_USAGE_RULES = "onStateUsageRules";
    public static final String ON_FLOW_CONSTRAINT_USAGE_RULES = "onFlowConstraintUsageRules";
    public static final String ON_ANGLE_CONSTRAINT_USAGE_RULES = "onAngleConstraintUsageRules";
    public static final String ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES = "onFlowConstraintInCountryUsageRules";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String EXTENSIONS = "extensions";

    public static final String RANGES = "ranges";
    public static final String SETPOINT = "setpoint";
    public static final String OPERATOR = "operator";
    public static final String ACTION_TYPE = "actionType";
    public static final String RANGE_TYPE = "rangeType";
    public static final String INITIAL_SETPOINT = "initialSetpoint";
    public static final String INITIAL_TAP = "initialTap";
    public static final String TAP_TO_ANGLE_CONVERSION_MAP = "tapToAngleConversionMap";

    public static final String UNIT = "unit";
    public static final String RULE = "rule"; // retro-compatibility only
    public static final String SIDE = "side";
    public static final String MIN = "min";
    public static final String MAX = "max";

    public static final String COUNTRY = "country";

    // instants
    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_INSTANT = "curative";

    // units
    public static final String AMPERE_UNIT = "ampere";
    public static final String MEGAWATT_UNIT = "megawatt";
    public static final String DEGREE_UNIT = "degree";
    public static final String KILOVOLT_UNIT = "kilovolt";
    public static final String PERCENT_IMAX_UNIT = "percent_imax";
    public static final String TAP_UNIT = "tap";

    // rules, retro-compatibility only
    public static final String ON_LOW_VOLTAGE_LEVEL_RULE = "onLowVoltageLevel";
    public static final String ON_HIGH_VOLTAGE_LEVEL_RULE = "onHighVoltageLevel";
    public static final String ON_NON_REGULATED_SIDE_RULE = "onNonRegulatedSide";
    public static final String ON_REGULATED_SIDE_RULE = "onRegulatedSide";
    public static final String ON_LEFT_SIDE_RULE = "onLeftSide";
    public static final String ON_RIGHT_SIDE_RULE = "onRightSide";

    // threshold side
    public static final String LEFT_SIDE = "left";
    public static final String RIGHT_SIDE = "right";

    // usage methods
    public static final String UNAVAILABLE_USAGE_METHOD = "unavailable";
    public static final String FORCED_USAGE_METHOD = "forced";
    public static final String AVAILABLE_USAGE_METHOD = "available";
    public static final String UNDEFINED_USAGE_METHOD = "undefined";

    // range types
    public static final String ABSOLUTE_RANGE = "absolute";
    public static final String RELATIVE_TO_PREVIOUS_INSTANT_RANGE = "relativeToPreviousInstant";
    public static final String RELATIVE_TO_INITIAL_NETWORK_RANGE = "relativeToInitialNetwork";

    // action types
    public static final String OPEN_ACTION = "open";
    public static final String CLOSE_ACTION = "close";

    // manipulate version
    public static int getPrimaryVersionNumber(String fullVersion) {
        return Integer.parseInt(divideVersionNumber(fullVersion)[0]);
    }

    public static int getSubVersionNumber(String fullVersion) {
        return Integer.parseInt(divideVersionNumber(fullVersion)[1]);
    }

    private static String[] divideVersionNumber(String fullVersion) {
        String[] dividedV = fullVersion.split("\\.");
        if (dividedV.length != 2 || !Arrays.stream(dividedV).allMatch(StringUtils::isNumeric)) {
            throw new FaraoException("json CRAC version number must be of the form vX.Y");
        }
        return dividedV;
    }

    // serialization of enums

    public static String serializeInstant(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
                return PREVENTIVE_INSTANT;
            case OUTAGE:
                return OUTAGE_INSTANT;
            case AUTO:
                return AUTO_INSTANT;
            case CURATIVE:
                return CURATIVE_INSTANT;
            default:
                throw new FaraoException(String.format("Unsupported instant %s", instant));
        }
    }

    public static Instant deserializeInstant(String stringValue) {
        switch (stringValue) {
            case PREVENTIVE_INSTANT:
                return Instant.PREVENTIVE;
            case OUTAGE_INSTANT:
                return Instant.OUTAGE;
            case AUTO_INSTANT:
                return Instant.AUTO;
            case CURATIVE_INSTANT:
                return Instant.CURATIVE;
            default:
                throw new FaraoException(String.format("Unrecognized instant %s", stringValue));
        }
    }

    public static String serializeUnit(Unit unit) {
        switch (unit) {
            case AMPERE:
                return AMPERE_UNIT;
            case DEGREE:
                return DEGREE_UNIT;
            case MEGAWATT:
                return MEGAWATT_UNIT;
            case KILOVOLT:
                return KILOVOLT_UNIT;
            case PERCENT_IMAX:
                return PERCENT_IMAX_UNIT;
            case TAP:
                return TAP_UNIT;
            default:
                throw new FaraoException(String.format("Unsupported unit %s", unit));
        }
    }

    public static Unit deserializeUnit(String stringValue) {
        switch (stringValue) {
            case AMPERE_UNIT:
                return Unit.AMPERE;
            case DEGREE_UNIT:
                return Unit.DEGREE;
            case MEGAWATT_UNIT:
                return Unit.MEGAWATT;
            case KILOVOLT_UNIT:
                return Unit.KILOVOLT;
            case PERCENT_IMAX_UNIT:
                return Unit.PERCENT_IMAX;
            case TAP_UNIT:
                return Unit.TAP;
            default:
                throw new FaraoException(String.format("Unrecognized unit %s", stringValue));
        }
    }

    public static String serializeSide(Side side) {
        switch (side) {
            case LEFT:
                return LEFT_SIDE;
            case RIGHT:
                return RIGHT_SIDE;
            default:
                throw new FaraoException(String.format("Unsupported side %s", side));
        }
    }

    public static Side deserializeSide(String stringValue) {
        switch (stringValue) {
            case LEFT_SIDE:
                return Side.LEFT;
            case RIGHT_SIDE:
                return Side.RIGHT;
            default:
                throw new FaraoException(String.format("Unrecognized side %s", stringValue));
        }
    }

    /**
     * Converts old BranchThresholdRule to Side
     * For retro-compatibility purposes only
     */
    public static Side convertBranchThresholdRuleToSide(String branchThresholdRule, Pair<Double, Double> nominalV) {
        switch (branchThresholdRule) {
            case ON_LEFT_SIDE_RULE:
            case ON_REGULATED_SIDE_RULE:
                // This is true only when the network is in UCTE format.
                return Side.LEFT;
            case ON_RIGHT_SIDE_RULE:
            case ON_NON_REGULATED_SIDE_RULE:
                // This is true only when the network is in UCTE format.
                return Side.RIGHT;
            case ON_LOW_VOLTAGE_LEVEL_RULE:
                if (Objects.isNull(nominalV) || Objects.isNull(nominalV.getLeft()) || Objects.isNull(nominalV.getRight()) || Double.isNaN(nominalV.getLeft()) || Double.isNaN(nominalV.getRight())) {
                    throw new FaraoException("ON_LOW_VOLTAGE_LEVEL thresholds can only be defined on FlowCnec whose nominalVoltages have been set on both sides");
                }
                if (nominalV.getLeft() <= nominalV.getRight()) {
                    return Side.LEFT;
                } else {
                    return Side.RIGHT;
                }
            case ON_HIGH_VOLTAGE_LEVEL_RULE:
                if (Objects.isNull(nominalV) || Objects.isNull(nominalV.getLeft()) || Objects.isNull(nominalV.getRight()) || Double.isNaN(nominalV.getLeft()) || Double.isNaN(nominalV.getRight())) {
                    throw new FaraoException("ON_HIGH_VOLTAGE_LEVEL thresholds can only be defined on FlowCnec whose nominalVoltages have been set on both sides");
                }
                if (nominalV.getLeft() < nominalV.getRight()) {
                    return Side.RIGHT;
                } else {
                    return Side.LEFT;
                }
            default:
                throw new FaraoException(String.format("Rule %s is not yet handled for thresholds on FlowCnec", branchThresholdRule));
        }
    }

    public static String serializeUsageMethod(UsageMethod usageMethod) {
        switch (usageMethod) {
            case UNAVAILABLE:
                return UNAVAILABLE_USAGE_METHOD;
            case FORCED_IF_AVAILABLE:
                return FORCED_USAGE_METHOD;
            case AVAILABLE:
                return AVAILABLE_USAGE_METHOD;
            case UNDEFINED:
                return UNDEFINED_USAGE_METHOD;
            default:
                throw new FaraoException(String.format("Unsupported usage method %s", usageMethod));
        }
    }

    public static UsageMethod deserializeUsageMethod(String stringValue) {
        switch (stringValue) {
            case UNAVAILABLE_USAGE_METHOD:
                return UsageMethod.UNAVAILABLE;
            case FORCED_USAGE_METHOD:
                return UsageMethod.FORCED_IF_AVAILABLE;
            case AVAILABLE_USAGE_METHOD:
                return UsageMethod.AVAILABLE;
            case UNDEFINED_USAGE_METHOD:
                return UsageMethod.UNDEFINED;
            default:
                throw new FaraoException(String.format("Unrecognized usage method %s", stringValue));
        }
    }

    public static String serializeRangeType(RangeType rangeType) {
        switch (rangeType) {
            case ABSOLUTE:
                return ABSOLUTE_RANGE;
            case RELATIVE_TO_PREVIOUS_INSTANT:
                return RELATIVE_TO_PREVIOUS_INSTANT_RANGE;
            case RELATIVE_TO_INITIAL_NETWORK:
                return RELATIVE_TO_INITIAL_NETWORK_RANGE;
            default:
                throw new FaraoException(String.format("Unsupported range type %s", rangeType));
        }
    }

    public static RangeType deserializeRangeType(String stringValue) {
        switch (stringValue) {
            case ABSOLUTE_RANGE:
                return RangeType.ABSOLUTE;
            case RELATIVE_TO_PREVIOUS_INSTANT_RANGE:
                return RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
            case RELATIVE_TO_INITIAL_NETWORK_RANGE:
                return RangeType.RELATIVE_TO_INITIAL_NETWORK;
            default:
                throw new FaraoException(String.format("Unrecognized range type %s", stringValue));
        }
    }

    public static String serializeActionType(ActionType actionType) {
        switch (actionType) {
            case OPEN:
                return OPEN_ACTION;
            case CLOSE:
                return CLOSE_ACTION;
            default:
                throw new FaraoException(String.format("Unsupported action type %s", actionType));
        }
    }

    public static ActionType deserializeActionType(String stringValue) {
        switch (stringValue) {
            case OPEN_ACTION:
                return ActionType.OPEN;
            case CLOSE_ACTION:
                return ActionType.CLOSE;
            default:
                throw new FaraoException(String.format("Unrecognized action type %s", stringValue));
        }
    }

    public static class ThresholdComparator implements Comparator<Threshold> {
        @Override
        public int compare(Threshold o1, Threshold o2) {
            String unit1 = serializeUnit(o1.getUnit());
            String unit2 = serializeUnit(o2.getUnit());
            if (unit1.equals(unit2)) {
                if (o1.min().isPresent()) {
                    return -1;
                }
                return 1;
            } else {
                return unit1.compareTo(unit2);
            }
        }
    }
}
