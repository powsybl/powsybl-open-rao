/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileKeyword;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.OverridableAttribute;
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

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum Query {
    ASSESSED_ELEMENT(AssessedElement.class, CsaProfileKeyword.ASSESSED_ELEMENT, OverridableAttribute.ENABLED, Map.of("isCombinableWithContingency", false, "isCombinableWithRemedialAction", false, OverridableAttribute.ENABLED.getDefaultName(), true, "flowReliabilityMargin", 0d)),
    ASSESSED_ELEMENT_WITH_CONTINGENCY(AssessedElementWithContingency.class, CsaProfileKeyword.ASSESSED_ELEMENT, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION(AssessedElementWithRemedialAction.class, CsaProfileKeyword.ASSESSED_ELEMENT, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    CONTINGENCY(Contingency.class, CsaProfileKeyword.CONTINGENCY, OverridableAttribute.MUST_STUDY, Map.of()),
    CONTINGENCY_EQUIPMENT(ContingencyEquipment.class, CsaProfileKeyword.CONTINGENCY, null, Map.of()),
    CONTINGENCY_WITH_REMEDIAL_ACTION(ContingencyWithRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    CURRENT_LIMIT(CurrentLimit.class, CsaProfileKeyword.CGMES, OverridableAttribute.VALUE, Map.of()),
    GRID_STATE_ALTERATION_COLLECTION(GridStateAlterationCollection.class, CsaProfileKeyword.REMEDIAL_ACTION, null, Map.of()),
    GRID_STATE_ALTERATION_REMEDIAL_ACTION(GridStateAlterationRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.AVAILABLE, Map.of()),
    REMEDIAL_ACTION_DEPENDENCY(RemedialActionDependency.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    REMEDIAL_ACTION_GROUP(RemedialActionGroup.class, CsaProfileKeyword.REMEDIAL_ACTION, null, Map.of()),
    REMEDIAL_ACTION_SCHEME(RemedialActionScheme.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ARMED, Map.of()),
    ROTATING_MACHINE_ACTION(RotatingMachineAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    SCHEME_REMEDIAL_ACTION(SchemeRemedialAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.AVAILABLE, Map.of()),
    SHUNT_COMPENSATOR_MODIFICATION(ShuntCompensatorModification.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    STAGE(Stage.class, CsaProfileKeyword.REMEDIAL_ACTION, null, Map.of()),
    STATIC_PROPERTY_RANGE(StaticPropertyRange.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.VALUE, Map.of()),
    TAP_CHANGER(TapChanger.class, CsaProfileKeyword.CGMES, null, Map.of()),
    TAP_POSITION_ACTION(TapPositionAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    TOPOLOGY_ACTION(TopologyAction.class, CsaProfileKeyword.REMEDIAL_ACTION, OverridableAttribute.ENABLED, Map.of(OverridableAttribute.ENABLED.getDefaultName(), true)),
    VOLTAGE_ANGLE_LIMIT(VoltageAngleLimit.class, CsaProfileKeyword.EQUIPMENT_RELIABILITY, OverridableAttribute.VALUE, Map.of()),
    VOLTAGE_LIMIT(VoltageLimit.class, CsaProfileKeyword.CGMES, OverridableAttribute.VALUE, Map.of("isInfiniteDuration", true));

    private final String title;
    private final CsaProfileKeyword targetProfilesKeyword;
    private final OverridableAttribute overridableAttribute;
    private final Class<? extends NCObject> nativeClass;
    private final Map<String, Object> defaultValues;

    Query(Class<? extends NCObject> nativeClass, CsaProfileKeyword targetProfilesKeyword, OverridableAttribute overridableAttribute, Map<String, Object> defaultValues) {
        this.title = Character.toLowerCase(nativeClass.getSimpleName().charAt(0)) + nativeClass.getSimpleName().substring(1);
        this.targetProfilesKeyword = targetProfilesKeyword;
        this.overridableAttribute = overridableAttribute;
        this.nativeClass = nativeClass;
        this.defaultValues = defaultValues;
    }

    public String getTitle() {
        return title;
    }

    public CsaProfileKeyword getTargetProfilesKeyword() {
        return targetProfilesKeyword;
    }

    public OverridableAttribute getOverridableAttribute() {
        return overridableAttribute;
    }

    public Class<? extends NCObject> getNativeClass() {
        return nativeClass;
    }

    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }
}
