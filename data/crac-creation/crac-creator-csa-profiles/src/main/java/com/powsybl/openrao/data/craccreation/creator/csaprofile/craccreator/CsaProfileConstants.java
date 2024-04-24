/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import java.util.List;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public final class CsaProfileConstants {

    private CsaProfileConstants() {
    }

    /**
     * CSA Profiles keywords
     */
    public enum CsaProfileKeywords {
        ASSESSED_ELEMENT("AE"),
        CONTINGENCY("CO"),
        EQUIPMENT_RELIABILITY("ER"),
        REMEDIAL_ACTION("RA"),
        STEADY_STATE_INSTRUCTION("SSI"),
        STEADY_STATE_HYPOTHESIS("SSH");

        private final String keyword;

        CsaProfileKeywords(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public String toString() {
            return keyword;
        }
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

    public static final String IMPORTED_CONTINGENT_STATUS = "http://iec.ch/TC57/CIM100#ContingencyEquipmentStatusKind.outOfService";

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
    public static final String TOPOLOGY_ACTION = "topologyAction";
    public static final String ROTATING_MACHINE_ACTION = "rotatingMachineAction";
    public static final String TAP_POSITION_ACTION = "tapPositionAction";
    public static final String STATIC_PROPERTY_RANGE = "staticPropertyRange";
    public static final String REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION = "contingencyWithRemedialAction";
    public static final String MRID = "mRID";
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

    public enum PropertyReference {
        SWITCH("Switch.open"),
        ROTATING_MACHINE("RotatingMachine.p"),
        TAP_CHANGER("TapChanger.step"),
        SHUNT_COMPENSATOR("ShuntCompensator.sections");

        PropertyReference(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = "http://energy.referencedata.eu/PropertyReference/";

        @Override
        public String toString() {
            return PropertyReference.URL + this.name;
        }
    }

    public static final String ROTATING_MACHINE = "rotatingMachineId";
    public static final String TAP_CHANGER = "tapChangerId";
    public static final String NORMAL_VALUE = "normalValue";
    public static final String OVERRIDE_VALUE = "value";
    public static final String STATIC_PROPERTY_RANGE_VALUE_KIND = "valueKind";
    public static final String STATIC_PROPERTY_RANGE_DIRECTION = "direction";

    public enum RelativeDirectionKind {
        NONE("none"),
        DOWN("down"),
        UP("up"),
        UP_AND_DOWN("upAndDown");

        RelativeDirectionKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#RelativeDirectionKind.";

        @Override
        public String toString() {
            return RelativeDirectionKind.URL + this.name;
        }
    }

    public enum ValueOffsetKind {
        ABSOLUTE("absolute"),
        INCREMENTAL("incremental"),
        INCREMENTAL_PERCENTAGE("incrementalPercentage");

        ValueOffsetKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#ValueOffsetKind.";

        @Override
        public String toString() {
            return ValueOffsetKind.URL + this.name;
        }
    }

    public enum RemedialActionKind {
        CURATIVE("curative"),
        PREVENTIVE("preventive");

        RemedialActionKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#RemedialActionKind.";

        @Override
        public String toString() {
            return RemedialActionKind.URL + this.name;
        }
    }

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

    public static final String REQUEST_CURRENT_LIMIT = "currentLimit";
    public static final String REQUEST_VOLTAGE_LIMIT = "voltageLimit";
    public static final String REQUEST_TOPOLOGY_ACTION = "topologyAction";
    public static final String REQUEST_ROTATING_MACHINE_ACTION = "rotatingMachineAction";
    public static final String REQUEST_SHUNT_COMPENSATOR_MODIFICATION = "shuntCompensatorModification";
    public static final String REQUEST_TAP_POSITION_ACTION = "tapPositionAction";
    public static final String CGMES = "CGMES";
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

    public enum ElementCombinationConstraintKind {
        CONSIDERED("considered"),
        INCLUDED("included"),
        EXCLUDED("excluded");

        ElementCombinationConstraintKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_NS_NC_URL + "#ElementCombinationConstraintKind.";

        @Override
        public String toString() {
            return ElementCombinationConstraintKind.URL + this.name;
        }
    }

    public enum LimitTypeKind {
        PATL("patl"),
        TATL("tatl"),
        HIGH_VOLTAGE("highVoltage"),
        LOW_VOLTAGE("lowVoltage");

        LimitTypeKind(String name) {
            this.name = name;
        }

        private final String name;
        private static final String URL = ENTSOE_URL + "LimitTypeKind.";

        @Override
        public String toString() {
            return LimitTypeKind.URL + this.name;
        }
    }

    public enum OperationalLimitDirectionKind {
        ABSOLUTE("absoluteValue"),
        HIGH("high"),
        LOW("low");

        OperationalLimitDirectionKind(String direction) {
            this.direction = direction;
        }

        private final String direction;
        private static final String URL = IEC_URL + "CIM100#OperationalLimitDirectionKind.";

        @Override
        public String toString() {
            return OperationalLimitDirectionKind.URL + this.direction;
        }
    }

    /**
     * requests for angle cnec
     */

    public static final String REQUEST_IS_FLOW_TO_REF_TERMINAL = "isFlowToRefTerminal";
    public static final String REQUEST_VOLTAGE_ANGLE_LIMIT = "voltageAngleLimit";

    public enum LimitType {
        ANGLE(REQUEST_VOLTAGE_ANGLE_LIMIT),
        CURRENT(REQUEST_CURRENT_LIMIT),
        VOLTAGE(REQUEST_VOLTAGE_LIMIT);

        LimitType(String type) {
            this.type = type;
        }

        private final String type;

        @Override
        public String toString() {
            return this.type;
        }
    }

    public enum HeaderType {
        START_END_DATE,
        SCENARIO_TIME;
    }

    public enum OverridingObjectsFields {
        CONTINGENCY("contingencyOverriding", REQUEST_CONTINGENCY, REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY, REQUEST_CONTINGENCIES_OVERRIDE_MUST_STUDY, HeaderType.START_END_DATE),
        ASSESSED_ELEMENT("assessedElementOverriding", REQUEST_ASSESSED_ELEMENT, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        ASSESSED_ELEMENT_WITH_CONTINGENCY("assessedElementWithContingencyOverriding", REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION("assessedElementWithRemedialActionOverriding", REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        CONTINGENCY_WITH_REMEDIAL_ACTION("contingencyWithRemedialActionOverriding", REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        GRID_STATE_ALTERATION_REMEDIAL_ACTION("gridStateAlterationRemedialActionOverriding", REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION, NORMAL_AVAILABLE, OVERRIDE_AVAILABLE, HeaderType.START_END_DATE),
        GRID_STATE_ALTERATION("gridStateAlterationOverriding", "gridStateAlteration", NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        STATIC_PROPERTY_RANGE("staticPropertyRangeOverriding", CsaProfileConstants.STATIC_PROPERTY_RANGE, NORMAL_VALUE, OVERRIDE_VALUE, HeaderType.START_END_DATE),
        REMEDIAL_ACTION_SCHEME("remedialActionSchemeOverriding", CsaProfileConstants.REMEDIAL_ACTION_SCHEME, NORMAL_ARMED, OVERRIDE_ARMED, HeaderType.START_END_DATE),
        VOLTAGE_ANGLE_LIMIT("voltageAngleLimitOverriding", "voltageAngleLimit", NORMAL_VALUE, OVERRIDE_VALUE, HeaderType.START_END_DATE),
        CURRENT_LIMIT("currentLimitOverriding", REQUEST_CURRENT_LIMIT, NORMAL_VALUE, OVERRIDE_VALUE, HeaderType.SCENARIO_TIME),
        VOLTAGE_LIMIT("voltageLimitOverriding", REQUEST_VOLTAGE_LIMIT, NORMAL_VALUE, OVERRIDE_VALUE, HeaderType.SCENARIO_TIME),
        TOPOLOGY_ACTION("topologyActionOverriding", REQUEST_TOPOLOGY_ACTION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        ROTATING_MACHINE_ACTION("rotatingMachineActionOverriding", REQUEST_ROTATING_MACHINE_ACTION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        SHUNT_COMPENSATOR_MODIFICATION("shuntCompensatorModificationOverriding", REQUEST_SHUNT_COMPENSATOR_MODIFICATION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        TAP_POSITION_ACTION("tapPositionActionOverriding", REQUEST_TAP_POSITION_ACTION, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE),
        SCHEME_REMEDIAL_ACTION("schemeRemedialActionOverriding", REQUEST_SCHEME_REMEDIAL_ACTION, NORMAL_AVAILABLE, OVERRIDE_AVAILABLE, HeaderType.START_END_DATE),
        SCHEME_REMEDIAL_ACTION_DEPENDENCY("remedialActionDependencyOverriding", REQUEST_REMEDIAL_ACTION_DEPENDENCY, NORMAL_ENABLED, OVERRIDE_ENABLED, HeaderType.START_END_DATE);

        final String requestName;
        final String objectName;
        final String initialFieldName;
        final String overriddenFieldName;
        final HeaderType headerType;

        OverridingObjectsFields(String requestName, String objectName, String initialFieldName, String overridedFieldName, HeaderType headerType) {
            this.requestName = requestName;
            this.objectName = objectName;
            this.initialFieldName = initialFieldName;
            this.overriddenFieldName = overridedFieldName;
            this.headerType = headerType;
        }

        public String getRequestName() {
            return this.requestName;
        }

        public String getObjectName() {
            return this.objectName;
        }

        public String getInitialFieldName() {
            return this.initialFieldName;
        }

        public String getOverridedFieldName() {
            return this.overriddenFieldName;
        }

        public HeaderType getHeaderType() {
            return this.headerType;
        }
    }

    public static final String SCENARIO_TIME = "scenarioTime";
}
