/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum Query {
    ASSESSED_ELEMENT("assessedElement"),
    ASSESSED_ELEMENT_WITH_CONTINGENCY("assessedElementWithContingency"),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION("assessedElementWithRemedialAction"),
    CONTINGENCY("contingency"),
    CONTINGENCY_EQUIPMENT("contingencyEquipment"),
    CONTINGENCY_WITH_REMEDIAL_ACTION("contingencyWithRemedialAction"),
    CURRENT_LIMIT("currentLimit"),
    GRID_STATE_ALTERATION_COLLECTION("gridStateAlterationCollection"),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION("gridStateAlterationRemedialAction"),
    HEADER("header"),
    REMEDIAL_ACTION_DEPENDENCY("remedialActionDependency"),
    REMEDIAL_ACTION_GROUP("remedialActionGroup"),
    REMEDIAL_ACTION_SCHEME("remedialActionScheme"),
    ROTATING_MACHINE_ACTION("rotatingMachineAction"),
    SCHEME_REMEDIAL_ACTION("schemeRemedialAction"),
    SHUNT_COMPENSATOR_MODIFICATION("shuntCompensatorModification"),
    STAGE("stage"),
    STATIC_PROPERTY_RANGE("staticPropertyRange"),
    TAP_CHANGER("tapChanger"),
    TAP_POSITION_ACTION("tapPositionAction"),
    TOPOLOGY_ACTION("topologyAction"),
    VOLTAGE_ANGLE_LIMIT("voltageAngleLimit"),
    VOLTAGE_LIMIT("voltageLimit");

    private final String title;

    Query(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
