/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

import java.util.List;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_1_INSTANT = "curative 1";
    public static final String CURATIVE_2_INSTANT = "curative 2";
    public static final String CURATIVE_3_INSTANT = "curative 3";

    private CsaProfileConstants() {
    }

    /**
     * constants to read rdf files
     */

    public static final String EXTENSION_FILE_CSA_PROFILE = "zip";

    public static final String RDF_BASE_URL = "http://entsoe.eu";

    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";

    /**
     * constants to access triplestore data
     */

    public static final String SPARQL_FILE_CSA_PROFILE = "csa_profile.sparql";

    /**
     * constants to request file headers
     */

    public static final String REQUEST_HEADER_START_DATE = "startDate";

    public static final String REQUEST_HEADER_END_DATE = "endDate";

    public static final String REQUEST_HEADER_KEYWORD = "keyword";

    /**
     * requests for contingencies
     */

    public static final String REQUEST_HEADER = "header";

    public static final String REQUEST_CONTINGENCY = "contingency";

    public static final String REQUEST_ORDINARY_CONTINGENCY = "ordinaryContingency";

    public static final String REQUEST_EXCEPTIONAL_CONTINGENCY = "exceptionalContingency";

    public static final String REQUEST_OUT_OF_RANGE_CONTINGENCY = "outOfRangeContingency";

    public static final String REQUEST_CONTINGENCY_EQUIPMENT = "contingencyEquipment";

    public static final String REQUEST_CONTINGENCIES_NAME = "name";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR = "idEquipmentOperator";

    public static final String REQUEST_CONTINGENCIES_EQUIPMENT_ID = "contingencyEquipmentId";

    public static final String REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY = "normalMustStudy";

    public static final String REQUEST_CONTINGENCIES_OVERRIDE_MUST_STUDY = "mustStudy";

    public static final String REQUEST_CONTINGENCIES_CONTINGENT_STATUS = "contingentStatus";

    public static final String OUT_OF_SERVICE_CONTINGENT_STATUS = "http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService";

    /**
     * remedial actions
     */

    public static final String REMEDIAL_ACTION_NAME = "name";
    public static final String SCHEME_REMEDIAL_ACTION = "schemeRemedialAction";
    public static final String REMEDIAL_ACTION_SCHEME = "remedialActionScheme";
    public static final String STAGE = "stage";
    public static final String SIPS = "http://entsoe.eu/ns/nc#RemedialActionSchemeKind.sips";
    public static final String NORMAL_ARMED = "normalArmed";
    public static final String OVERRIDE_ARMED = "armed";
    public static final String DEPENDING_REMEDIAL_ACTION_GROUP = "dependingRemedialActionGroup";
    public static final String GRID_STATE_ALTERATION_COLLECTION = "gridStateAlterationCollection";
    public static final String GRID_STATE_ALTERATION_REMEDIAL_ACTION = "gridStateAlterationRemedialAction";
    public static final String GRID_STATE_ALTERATION = "gridStateAlteration";
    public static final String TOPOLOGY_ACTION = "topologyAction";
    public static final String ROTATING_MACHINE_ACTION = "rotatingMachineAction";
    public static final String TAP_POSITION_ACTION = "tapPositionAction";
    public static final String STATIC_PROPERTY_RANGE = "staticPropertyRange";
    public static final String REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION = "contingencyWithRemedialAction";
    public static final String TIME_TO_IMPLEMENT = "timeToImplement";
    public static final String TSO = "tso";
    public static final String NORMAL_AVAILABLE = "normalAvailable";
    public static final String OVERRIDE_AVAILABLE = "available";
    public static final String KIND = "kind";
    public static final String COMBINATION_CONSTRAINT_KIND = "combinationConstraintKind";
    public static final String SWITCH = "switchId";
    public static final String NORMAL_ENABLED = "normalEnabled";
    public static final String OVERRIDE_ENABLED = "enabled";
    public static final String GRID_ALTERATION_PROPERTY_REFERENCE = "propertyReference";
    public static final String SHUNT_COMPENSATOR_MODIFICATION = "shuntCompensatorModification";
    public static final String SHUNT_COMPENSATOR_ID = "shuntCompensatorId";
    public static final String REQUEST_SCHEME_REMEDIAL_ACTION = "schemeRemedialAction";
    public static final String REQUEST_REMEDIAL_ACTION_GROUP = "remedialActionGroup";
    public static final String REQUEST_REMEDIAL_ACTION_DEPENDENCY = "remedialActionDependency";
    public static final String ROTATING_MACHINE = "rotatingMachineId";
    public static final String TAP_CHANGER = "tapChangerId";
    public static final String NORMAL_VALUE = "normalValue";
    public static final String OVERRIDE_VALUE = "value";
    public static final String STATIC_PROPERTY_RANGE_VALUE_KIND = "valueKind";
    public static final String STATIC_PROPERTY_RANGE_DIRECTION = "direction";

    /**
     * requests for flow cnec
     */

    public static final String REQUEST_ASSESSED_ELEMENT = "assessedElement";
    public static final String REQUEST_REMEDIAL_ACTION = "remedialAction";
    public static final String REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION = "gridStateAlterationRemedialAction";
    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY = "assessedElementWithContingency";
    public static final String REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION = "assessedElementWithRemedialAction";
    public static final String REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE = "inBaseCase";
    public static final String REQUEST_ASSESSED_ELEMENT_NAME = "name";
    public static final String REQUEST_ASSESSED_ELEMENT_OPERATOR = "assessedSystemOperator";
    public static final String REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT = "operationalLimit";
    public static final String REQUEST_ASSESSED_ELEMENT_CONDUCTING_EQUIPMENT = "conductingEquipment";
    public static final String REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED = "normalEnabled";
    public static final String REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY = "isCombinableWithContingency";
    public static final String REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION = "isCombinableWithRemedialAction";
    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_COMBINATION_CONSTRAINT_KIND = "combinationConstraintKind";
    public static final String REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY_NORMAL_ENABLED = "normalEnabled";
    public static final String REQUEST_ASSESSED_ELEMENT_SCANNED_FOR_REGION = "scannedForRegion";
    public static final String REQUEST_ASSESSED_ELEMENT_SECURED_FOR_REGION = "securedForRegion";
    public static final String REQUEST_FLOW_RELIABILITY_MARGIN = "flowReliabilityMargin";
    public static final String OVERLAPPING_ZONE = "overlappingZone";

    public static final String REQUEST_CURRENT_LIMIT = "currentLimit";
    public static final String REQUEST_VOLTAGE_LIMIT = "voltageLimit";
    public static final String REQUEST_TOPOLOGY_ACTION = "topologyAction";
    public static final String REQUEST_ROTATING_MACHINE_ACTION = "rotatingMachineAction";
    public static final String REQUEST_SHUNT_COMPENSATOR_MODIFICATION = "shuntCompensatorModification";
    public static final String REQUEST_TAP_POSITION_ACTION = "tapPositionAction";
    public static final String REQUEST_OPERATIONAL_LIMIT_VALUE = "value";
    public static final String REQUEST_VOLTAGE_ANGLE_LIMIT_NORMAL_VALUE = "normalValue";
    public static final String REQUEST_OPERATIONAL_LIMIT_TERMINAL = "terminal";
    public static final String REQUEST_OPERATIONAL_LIMIT_EQUIPMENT = "equipment";
    public static final String REQUEST_OPERATIONAL_LIMIT_TYPE = "limitType";
    public static final String REQUEST_OPERATIONAL_LIMIT_DIRECTION = "direction";
    public static final String REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION = "acceptableDuration";
    public static final String REQUEST_VOLTAGE_LIMIT_IS_INFINITE_DURATION = "isInfiniteDuration";
    public static final String ENTSOE_NS_NC_URL = "http://entsoe.eu/ns/nc";
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_LEFT = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary_1");
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_RIGHT = List.of("CGMES.Terminal2", "CGMES.Terminal_Boundary_2");
    public static final List<String> CURRENT_LIMIT_POSSIBLE_ALIASES_BY_TYPE_TIE_LINE = List.of("CGMES.Terminal1", "CGMES.Terminal_Boundary");
    public static final String IEC_URL = "http://iec.ch/TC57/";
    public static final String ENTSOE_URL = "http://entsoe.eu/CIM/SchemaExtension/3/1#";

    /**
     * requests for angle cnec
     */

    public static final String REQUEST_IS_FLOW_TO_REF_TERMINAL = "isFlowToRefTerminal";
    public static final String REQUEST_VOLTAGE_ANGLE_LIMIT = "voltageAngleLimit";
    public static final String SCENARIO_TIME = "scenarioTime";
}
