/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileKeyword;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElement;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.AssessedElementWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.Contingency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyEquipment;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ContingencyWithRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.CurrentLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.GridStateAlterationCollection;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.GridStateAlterationRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.NCObject;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionDependency;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionGroup;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RemedialActionScheme;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.RotatingMachineAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.SchemeRemedialAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.ShuntCompensatorModification;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.Stage;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.StaticPropertyRange;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TapChanger;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TapPositionAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.TopologyAction;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.cracio.csaprofiles.nc.VoltageLimit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum Query {
    ASSESSED_ELEMENT(AssessedElement.class, CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.IS_COMBINABLE_WITH_CONTINGENCY, false, CsaProfileConstants.IS_COMBINABLE_WITH_REMEDIAL_ACTION, false, CsaProfileConstants.NORMAL_ENABLED, true, CsaProfileConstants.FLOW_RELIABILITY_MARGIN, 0d)),
    ASSESSED_ELEMENT_WITH_CONTINGENCY(AssessedElementWithContingency.class, CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION(AssessedElementWithRemedialAction.class, CsaProfileKeyword.ASSESSED_ELEMENT, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    CONTINGENCY(Contingency.class, CsaProfileKeyword.CONTINGENCY, Map.of(CsaProfileConstants.NORMAL_MUST_STUDY, CsaProfileConstants.MUST_STUDY), Map.of()),
    CONTINGENCY_EQUIPMENT(ContingencyEquipment.class, CsaProfileKeyword.CONTINGENCY, Map.of(), Map.of()),
    CONTINGENCY_WITH_REMEDIAL_ACTION(ContingencyWithRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    CURRENT_LIMIT(CurrentLimit.class, CsaProfileKeyword.CGMES, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE), Map.of()),
    GRID_STATE_ALTERATION_COLLECTION(GridStateAlterationCollection.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(), Map.of()),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION(GridStateAlterationRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE), Map.of()),
    REMEDIAL_ACTION_DEPENDENCY(RemedialActionDependency.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    REMEDIAL_ACTION_GROUP(RemedialActionGroup.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(), Map.of()),
    REMEDIAL_ACTION_SCHEME(RemedialActionScheme.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ARMED, CsaProfileConstants.ARMED), Map.of()),
    ROTATING_MACHINE_ACTION(RotatingMachineAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    SCHEME_REMEDIAL_ACTION(SchemeRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_AVAILABLE, CsaProfileConstants.AVAILABLE), Map.of()),
    SHUNT_COMPENSATOR_MODIFICATION(ShuntCompensatorModification.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    STAGE(Stage.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(), Map.of()),
    STATIC_PROPERTY_RANGE(StaticPropertyRange.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE), Map.of()),
    TAP_CHANGER(TapChanger.class, CsaProfileKeyword.CGMES, Map.of(), Map.of()),
    TAP_POSITION_ACTION(TapPositionAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    TOPOLOGY_ACTION(TopologyAction.class, CsaProfileKeyword.REMEDIAL_ACTION, Map.of(CsaProfileConstants.NORMAL_ENABLED, CsaProfileConstants.ENABLED), Map.of(CsaProfileConstants.NORMAL_ENABLED, true)),
    VOLTAGE_ANGLE_LIMIT(VoltageAngleLimit.class, CsaProfileKeyword.EQUIPMENT_RELIABILITY, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE), Map.of()),
    VOLTAGE_LIMIT(VoltageLimit.class, CsaProfileKeyword.CGMES, Map.of(CsaProfileConstants.NORMAL_VALUE, CsaProfileConstants.VALUE), Map.of(CsaProfileConstants.IS_INFINITE_DURATION, true));

    private final String title;
    private final CsaProfileKeyword targetProfilesKeyword;
    private final Map<String, String> overridableFields;
    private final Class<? extends NCObject> nativeClass;
    private final Map<String, Object> defaultValues;

    Query(Class<? extends NCObject> nativeClass, CsaProfileKeyword targetProfilesKeyword, Map<String, String> overridableFields, Map<String, Object> defaultValues) {
        this.title = Character.toLowerCase(nativeClass.getSimpleName().charAt(0)) + nativeClass.getSimpleName().substring(1);
        this.targetProfilesKeyword = targetProfilesKeyword;
        this.overridableFields = overridableFields;
        this.nativeClass = nativeClass;
        this.defaultValues = defaultValues;
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

    public Class<? extends NCObject> getNativeClass() {
        return nativeClass;
    }

    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }
}
