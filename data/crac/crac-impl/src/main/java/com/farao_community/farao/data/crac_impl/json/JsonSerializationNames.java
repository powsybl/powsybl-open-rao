/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class JsonSerializationNames {

    private JsonSerializationNames() { }

    public static final String UNEXPECTED_FIELD = "Unexpected field: ";

    // field
    public static final String NETWORK_DATE = "networkDate";

    public static final String NETWORK_ELEMENT = "networkElement";
    public static final String NETWORK_ELEMENTS = "networkElements";

    public static final String GROUP_ID = "groupId";

    public static final String CONTINGENCY = "contingency";
    public static final String CONTINGENCIES = "contingencies";

    public static final String INSTANT = "instant";
    public static final String INSTANTS = "instants";

    public static final String STATE = "state";
    public static final String STATES = "states";

    public static final String CNEC = "cnec";
    public static final String CNECS = "cnecs";

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

    // implementation class types
    public static final String SIMPLE_STATE_TYPE = "simple-state";

    public static final String COMPLEX_CONTINGENCY_TYPE = "complex-contingency";
    public static final String XNODE_CONTINGENCY_TYPE = "xnode-contingency";

    public static final String FLOW_CNEC_TYPE = "flow-cnec";

    public static final String PST_RANGE_ACTION_IMPL_TYPE = "pst-range-action-impl";

    public static final String NETWORK_ACTION_IMPL_TYPE = "network-action-impl";
    public static final String TOPOLOGY_TYPE = "topology";
    public static final String PST_SETPOINT_TYPE = "pst-setpoint";
    public static final String INJECTION_SETPOINT_TYPE = "injection-setpoint";
    public static final String COMPLEX_NETWORK_ACTION_TYPE = "complex-network-action";

    public static final String FREE_TO_USE_TYPE = "free-to-use";
    public static final String ON_CONSTRAINT_TYPE = "on-constraint";
    public static final String ON_STATE_TYPE = "on-state";
}
