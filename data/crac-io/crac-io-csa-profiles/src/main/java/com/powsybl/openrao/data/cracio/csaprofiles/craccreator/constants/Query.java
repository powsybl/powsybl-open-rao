/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum Query {
    ASSESSED_ELEMENT("assessedElement", CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    ASSESSED_ELEMENT_WITH_CONTINGENCY("assessedElementWithContingency", CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION("assessedElementWithRemedialAction", CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    CONTINGENCY("contingency", CsaProfileKeyword.CONTINGENCY, Map.of(CsaProfileConstants.NORMAL_MUST_STUDY, CsaProfileConstants.MUST_STUDY)),
    CONTINGENCY_EQUIPMENT("contingencyEquipment", CsaProfileKeyword.CONTINGENCY, Map.of()),
    CONTINGENCY_WITH_REMEDIAL_ACTION("contingencyWithRemedialAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    CURRENT_LIMIT("currentLimit", CsaProfileKeyword.CGMES, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE)),
    GRID_STATE_ALTERATION_COLLECTION("gridStateAlterationCollection", CsaProfileKeyword.REMEDIAL_ACTION, Map.of()),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION("gridStateAlterationRemedialAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE)),
    HEADER("header", null, Map.of()),
    REMEDIAL_ACTION_DEPENDENCY("remedialActionDependency", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    REMEDIAL_ACTION_GROUP("remedialActionGroup", CsaProfileKeyword.REMEDIAL_ACTION, Map.of()),
    REMEDIAL_ACTION_SCHEME("remedialActionScheme", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ARMED, CsaProfileConstants.ARMED)),
    ROTATING_MACHINE_ACTION("rotatingMachineAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    SCHEME_REMEDIAL_ACTION("schemeRemedialAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE)),
    SHUNT_COMPENSATOR_MODIFICATION("shuntCompensatorModification", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    STAGE("stage", CsaProfileKeyword.REMEDIAL_ACTION, Map.of()),
    STATIC_PROPERTY_RANGE("staticPropertyRange", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE)),
    TAP_CHANGER("tapChanger", CsaProfileKeyword.CGMES, Map.of()),
    TAP_POSITION_ACTION("tapPositionAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    TOPOLOGY_ACTION("topologyAction", CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED)),
    VOLTAGE_ANGLE_LIMIT("voltageAngleLimit", CsaProfileKeyword.EQUIPMENT_RELIABILITY, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE)),
    VOLTAGE_LIMIT("voltageLimit", CsaProfileKeyword.CGMES, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE));

    private final String title;
    private final CsaProfileKeyword targetProfilesKeyword;
    private final Map<String, String> overridableFields;

    Query(String title, CsaProfileKeyword targetProfilesKeyword, Map<String, String> overridableFields) {
        this.title = title;
        this.targetProfilesKeyword = targetProfilesKeyword;
        this.overridableFields = overridableFields;
    }

    public String getTitle() {
        return title;
    }

    public CsaProfileKeyword getTargetProfilesKeyword() {
        return targetProfilesKeyword;
    }

    public Map<String, String> getOverridableFields() {
        return new HashMap<>(overridableFields);
    }
}
