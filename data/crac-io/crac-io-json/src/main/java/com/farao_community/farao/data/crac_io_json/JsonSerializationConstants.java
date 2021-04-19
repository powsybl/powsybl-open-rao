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
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class JsonSerializationConstants {

    private JsonSerializationConstants() { }

    public static final String UNEXPECTED_FIELD = "Unexpected field: ";

    // field
    public static final String NETWORK_DATE = "networkDate";

    public static final String NETWORK_ELEMENT = "networkElement";
    public static final String NETWORK_ELEMENTS = "networkElements";
    public static final String NETWORK_ELEMENTS_IDS = "networkElementsIds";
    public static final String NETWORK_ELEMENT_ID = "networkElementId";

    public static final String GROUP_ID = "groupId";

    public static final String CONTINGENCY = "contingency";
    public static final String CONTINGENCIES = "contingencies";
    public static final String CONTINGENCY_ID = "contingencyId";

    public static final String INSTANT = "instant";

    public static final String CNEC = "cnec";
    public static final String CNECS = "cnecs";

    public static final String FLOW_CNECS = "flowCnecs";

    public static final String THRESHOLDS = "thresholds";
    public static final String FRM = "frm";
    public static final String OPTIMIZED = "optimized";
    public static final String MONITORED = "monitored";

    public static final String RANGE_ACTIONS = "rangeActions";

    public static final String NETWORK_ACTIONS = "networkActions";
    public static final String ELEMENTARY_ACTIONS = "elementaryActions";

    public static final String USAGE_METHOD = "usageMethod";
    public static final String USAGE_RULES = "usageRules";

    public static final String TYPE = "type";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String EXTENSIONS = "extensions";
    public static final String RANGE_DEFINITION = "rangeDefinition";

    public static final String RANGES = "ranges";
    public static final String SETPOINT = "setpoint";
    public static final String OPERATOR = "operator";
    public static final String ACTION_TYPE = "actionType";

    public static final String XNODE_IDS = "xnodeIds";

    public static final String UNIT = "unit";
    public static final String RULE = "rule";
    public static final String MIN = "min";
    public static final String MAX = "max";

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

    // rules
    public static final String ON_LOW_VOLTAGE_LEVEL_RULE = "onLowVoltageLevel";
    public static final String ON_HIGH_VOLTAGE_LEVEL_RULE = "onHighVoltageLevel";
    public static final String ON_NON_REGULATED_SIDE_RULE = "onNonRegulatedSide";
    public static final String ON_REGULATED_SIDE_RULE = "onRegulatedSide";
    public static final String ON_LEFT_SIDE_RULE = "onLeftSide";
    public static final String ON_RIGHT_SIDE_RULE = "onRightSide";

    // implementation class types
    public static final String SIMPLE_STATE_TYPE = "simple-state";

    public static final String COMPLEX_CONTINGENCY_TYPE = "complex-contingency";
    public static final String XNODE_CONTINGENCY_TYPE = "xnode-contingency";

    public static final String FLOW_CNEC_TYPE = "flow-cnec";

    public static final String PST_RANGE_ACTION_IMPL_TYPE = "pst-range-action-impl";

    public static final String NETWORK_ACTION_IMPL_TYPE = "network-action-impl";
    public static final String TOPOLOGY_TYPE = "topological-action";
    public static final String PST_SETPOINT_TYPE = "pst-setpoint";
    public static final String INJECTION_SETPOINT_TYPE = "injection-setpoint";
    public static final String COMPLEX_NETWORK_ACTION_TYPE = "complex-network-action";

    public static final String FREE_TO_USE_TYPE = "free-to-use";
    public static final String ON_STATE_TYPE = "on-state";

    public static String serializeInstant(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
                return PREVENTIVE_INSTANT;
            case OUTAGE:
                return OUTAGE_INSTANT;
            case AUTO:
                return  AUTO_INSTANT;
            case CURATIVE:
                return CURATIVE_INSTANT;
            default:
                throw new FaraoException(String.format("Unsupported instant %s", instant));
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

    public static String serializeBranchThresholdRule(BranchThresholdRule rule) {
        switch (rule) {
            case ON_LOW_VOLTAGE_LEVEL:
                return ON_LOW_VOLTAGE_LEVEL_RULE;
            case ON_HIGH_VOLTAGE_LEVEL:
                return ON_HIGH_VOLTAGE_LEVEL_RULE;
            case ON_NON_REGULATED_SIDE:
                return ON_NON_REGULATED_SIDE_RULE;
            case ON_REGULATED_SIDE:
                return ON_REGULATED_SIDE_RULE;
            case ON_LEFT_SIDE:
                return ON_LEFT_SIDE_RULE;
            case ON_RIGHT_SIDE:
                return ON_RIGHT_SIDE_RULE;
            default:
                throw new FaraoException(String.format("Unsupported branch threshold rule %s", rule));
        }
    }
}
