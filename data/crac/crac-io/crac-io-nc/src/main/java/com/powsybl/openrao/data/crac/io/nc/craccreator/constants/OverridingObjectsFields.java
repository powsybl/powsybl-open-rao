/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.constants;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum OverridingObjectsFields {
    CONTINGENCY("contingencyOverriding", NcConstants.REQUEST_CONTINGENCY,
                NcConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY, NcConstants.REQUEST_CONTINGENCIES_OVERRIDE_MUST_STUDY, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT("assessedElementOverriding", NcConstants.REQUEST_ASSESSED_ELEMENT,
                     NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT_WITH_CONTINGENCY("assessedElementWithContingencyOverriding", NcConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY,
                                      NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION("assessedElementWithRemedialActionOverriding", NcConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION,
                                          NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    CONTINGENCY_WITH_REMEDIAL_ACTION("contingencyWithRemedialActionOverriding", NcConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION,
                                     NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION("gridStateAlterationRemedialActionOverriding", NcConstants.REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION,
                                          NcConstants.NORMAL_AVAILABLE, NcConstants.OVERRIDE_AVAILABLE, HeaderType.START_END_DATE),
    GRID_STATE_ALTERATION("gridStateAlterationOverriding", NcConstants.GRID_STATE_ALTERATION,
                          NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    STATIC_PROPERTY_RANGE("staticPropertyRangeOverriding", NcConstants.STATIC_PROPERTY_RANGE,
                          NcConstants.NORMAL_VALUE, NcConstants.OVERRIDE_VALUE, HeaderType.START_END_DATE),
    REMEDIAL_ACTION_SCHEME("remedialActionSchemeOverriding", NcConstants.REMEDIAL_ACTION_SCHEME,
                           NcConstants.NORMAL_ARMED, NcConstants.OVERRIDE_ARMED, HeaderType.START_END_DATE),
    VOLTAGE_ANGLE_LIMIT("voltageAngleLimitOverriding", "voltageAngleLimit",
                        NcConstants.NORMAL_VALUE, NcConstants.OVERRIDE_VALUE, HeaderType.START_END_DATE),
    CURRENT_LIMIT("currentLimitOverriding", NcConstants.REQUEST_CURRENT_LIMIT,
                  NcConstants.NORMAL_VALUE, NcConstants.OVERRIDE_VALUE, HeaderType.SCENARIO_TIME),
    VOLTAGE_LIMIT("voltageLimitOverriding", NcConstants.REQUEST_VOLTAGE_LIMIT,
                  NcConstants.NORMAL_VALUE, NcConstants.OVERRIDE_VALUE, HeaderType.SCENARIO_TIME),
    TOPOLOGY_ACTION("topologyActionOverriding", NcConstants.REQUEST_TOPOLOGY_ACTION,
                    NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    ROTATING_MACHINE_ACTION("rotatingMachineActionOverriding", NcConstants.REQUEST_ROTATING_MACHINE_ACTION,
                            NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    SHUNT_COMPENSATOR_MODIFICATION("shuntCompensatorModificationOverriding", NcConstants.REQUEST_SHUNT_COMPENSATOR_MODIFICATION,
                                   NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    TAP_POSITION_ACTION("tapPositionActionOverriding", NcConstants.REQUEST_TAP_POSITION_ACTION,
                        NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE),
    SCHEME_REMEDIAL_ACTION("schemeRemedialActionOverriding", NcConstants.REQUEST_SCHEME_REMEDIAL_ACTION,
                           NcConstants.NORMAL_AVAILABLE, NcConstants.OVERRIDE_AVAILABLE, HeaderType.START_END_DATE),
    SCHEME_REMEDIAL_ACTION_DEPENDENCY("remedialActionDependencyOverriding", NcConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY,
                                      NcConstants.NORMAL_ENABLED, NcConstants.OVERRIDE_ENABLED, HeaderType.START_END_DATE);

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
