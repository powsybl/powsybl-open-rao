/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum OverridingObjectsFields {
    CONTINGENCY("contingencyOverriding", CsaProfileConstants.CONTINGENCY, CsaProfileConstants.NORMAL_MUST_STUDY, CsaProfileConstants.MUST_STUDY, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT("assessedElementOverriding", CsaProfileConstants.ASSESSED_ELEMENT, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT_WITH_CONTINGENCY("assessedElementWithContingencyOverriding", CsaProfileConstants.ASSESSED_ELEMENT_WITH_CONTINGENCY, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION("assessedElementWithRemedialActionOverriding", CsaProfileConstants.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    CONTINGENCY_WITH_REMEDIAL_ACTION("contingencyWithRemedialActionOverriding", CsaProfileConstants.CONTINGENCY_WITH_REMEDIAL_ACTION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION("gridStateAlterationRemedialActionOverriding", CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION, CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE, HeaderType.START_END_DATE),
    GRID_STATE_ALTERATION("gridStateAlterationOverriding", CsaProfileConstants.GRID_STATE_ALTERATION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    STATIC_PROPERTY_RANGE("staticPropertyRangeOverriding", CsaProfileConstants.STATIC_PROPERTY_RANGE, CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE, HeaderType.START_END_DATE),
    REMEDIAL_ACTION_SCHEME("remedialActionSchemeOverriding", CsaProfileConstants.REMEDIAL_ACTION_SCHEME, CsaProfileConstants.NORMAL_ARMED, CsaProfileConstants.OVERRIDE_ARMED, HeaderType.START_END_DATE),
    VOLTAGE_ANGLE_LIMIT("voltageAngleLimitOverriding", "voltageAngleLimit", CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE, HeaderType.START_END_DATE),
    CURRENT_LIMIT("currentLimitOverriding", CsaProfileConstants.CURRENT_LIMIT, CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE, HeaderType.SCENARIO_TIME),
    VOLTAGE_LIMIT("voltageLimitOverriding", CsaProfileConstants.VOLTAGE_LIMIT, CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE, HeaderType.SCENARIO_TIME),
    TOPOLOGY_ACTION("topologyActionOverriding", CsaProfileConstants.TOPOLOGY_ACTION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    ROTATING_MACHINE_ACTION("rotatingMachineActionOverriding", CsaProfileConstants.ROTATING_MACHINE_ACTION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    SHUNT_COMPENSATOR_MODIFICATION("shuntCompensatorModificationOverriding", CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    TAP_POSITION_ACTION("tapPositionActionOverriding", CsaProfileConstants.TAP_POSITION_ACTION, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE),
    SCHEME_REMEDIAL_ACTION("schemeRemedialActionOverriding", CsaProfileConstants.SCHEME_REMEDIAL_ACTION, CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE, HeaderType.START_END_DATE),
    SCHEME_REMEDIAL_ACTION_DEPENDENCY("remedialActionDependencyOverriding", CsaProfileConstants.REMEDIAL_ACTION_DEPENDENCY, CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED, HeaderType.START_END_DATE);

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
