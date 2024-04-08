/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreator;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.CsaProfileCnecCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency.CsaProfileContingencyCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.CsaProfileRemedialActionsCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.ElementaryActionsHelper;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.craccreation.util.RaUsageLimitsAdder;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.AUTO_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_1_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_2_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.CURATIVE_3_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.OUTAGE_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants.PREVENTIVE_INSTANT;
import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils.isValidInterval;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CsaProfileCracCreator implements CracCreator<CsaProfileCrac, CsaProfileCracCreationContext> {

    private Crac crac;
    private Network network;
    CsaProfileCracCreationContext creationContext;

    @Override
    public String getNativeCracFormat() {
        return "CsaProfileCrac";
    }

    public CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        CsaCracCreationParameters csaParameters = cracCreationParameters.getExtension(CsaCracCreationParameters.class);
        String regionEicCode = csaParameters.getCapacityCalculationRegionEicCode();
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        addCsaInstants();
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);
        clearNativeCracContextsAndMap(nativeCrac, offsetDateTime);

        Map<String, String> overridingData = nativeCrac.getOverridingCracData(offsetDateTime);
        PropertyBags contingencies = CsaProfileCracUtils.overrideData(nativeCrac.getContingencies(), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY);
        PropertyBags assessedElements = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElements(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT);
        PropertyBags assessedElementsWithContingencies = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElementsWithContingencies(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_CONTINGENCY);
        PropertyBags currentLimits = CsaProfileCracUtils.overrideData(nativeCrac.getCurrentLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.CURRENT_LIMIT);
        PropertyBags voltageLimits = CsaProfileCracUtils.overrideData(nativeCrac.getVoltageLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_LIMIT);
        PropertyBags angleLimits = CsaProfileCracUtils.overrideData(nativeCrac.getAngleLimits(), overridingData, CsaProfileConstants.OverridingObjectsFields.VOLTAGE_ANGLE_LIMIT);
        PropertyBags assessedElementsWithRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getAssessedElementsWithRemedialAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION);
        PropertyBags contingenciesWithRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getContingencyWithRemedialAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.CONTINGENCY_WITH_REMEDIAL_ACTION);
        PropertyBags gridStateAlterationRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationRemedialAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        PropertyBags remedialActionSchemes = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActionScheme(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME);
        PropertyBags gridStateAlterationsCollection = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationCollection(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION);
        PropertyBags staticPropertyRanges = CsaProfileCracUtils.overrideData(nativeCrac.getStaticPropertyRanges(), overridingData, CsaProfileConstants.OverridingObjectsFields.STATIC_PROPERTY_RANGE);
        PropertyBags topologyActions = CsaProfileCracUtils.overrideData(nativeCrac.getTopologyAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.TOPOLOGY_ACTION);
        PropertyBags rotatingMachineActions = CsaProfileCracUtils.overrideData(nativeCrac.getRotatingMachineAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.ROTATING_MACHINE_ACTION);
        PropertyBags shuntCompensatorModifications = CsaProfileCracUtils.overrideData(nativeCrac.getShuntCompensatorModifications(), overridingData, CsaProfileConstants.OverridingObjectsFields.SHUNT_COMPENSATOR_MODIFICATION);
        PropertyBags tapPositionActions = CsaProfileCracUtils.overrideData(nativeCrac.getTapPositionAction(), overridingData, CsaProfileConstants.OverridingObjectsFields.TAP_POSITION_ACTION);
        PropertyBags schemeRemedialActions = CsaProfileCracUtils.overrideData(nativeCrac.getSchemeRemedialActions(), overridingData, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION);
        PropertyBags remedialActionDependencies = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActionDependencies(), overridingData, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION_DEPENDENCY);

        createContingencies(contingencies, nativeCrac.getContingencyEquipments());

        createCnecs(assessedElements, assessedElementsWithContingencies, currentLimits, voltageLimits, angleLimits, cracCreationParameters.getDefaultMonitoredSides(), regionEicCode);

        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), assessedElements, assessedElementsWithRemedialAction);
        ElementaryActionsHelper elementaryActionsHelper = new ElementaryActionsHelper(gridStateAlterationRemedialAction, schemeRemedialActions, remedialActionSchemes, nativeCrac.getStage(), gridStateAlterationsCollection, assessedElementsWithRemedialAction, contingenciesWithRemedialAction, staticPropertyRanges, topologyActions, rotatingMachineActions, shuntCompensatorModifications, tapPositionActions, nativeCrac.getRemedialActionGroups(), remedialActionDependencies);
        createRemedialActions(onConstraintUsageRuleAdder, elementaryActionsHelper);
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants() {
        crac.newInstant(PREVENTIVE_INSTANT, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT, InstantKind.AUTO)
            .newInstant(CURATIVE_1_INSTANT, InstantKind.CURATIVE)
            .newInstant(CURATIVE_2_INSTANT, InstantKind.CURATIVE)
            .newInstant(CURATIVE_3_INSTANT, InstantKind.CURATIVE);
    }

    private void clearNativeCracContextsAndMap(CsaProfileCrac nativeCrac, OffsetDateTime offsetDateTime) {
        nativeCrac.getHeaders().forEach((contextName, properties) -> {
            if (!properties.isEmpty()) {
                PropertyBag property = properties.get(0);
                if (!checkTimeCoherence(property, offsetDateTime)) {
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn(String.format("[REMOVED] The file : %s will be ignored. Its dates are not consistent with the import date : %s", contextName, offsetDateTime));
                    nativeCrac.clearContext(contextName);
                    nativeCrac.clearKeywordMap(contextName);
                }
            }
        });
    }

    private boolean checkTimeCoherence(PropertyBag header, OffsetDateTime offsetDateTime) {
        String startTime = header.getId(CsaProfileConstants.REQUEST_HEADER_START_DATE);
        String endTime = header.getId(CsaProfileConstants.REQUEST_HEADER_END_DATE);
        return isValidInterval(offsetDateTime, startTime, endTime);
    }

    private void createRemedialActions(OnConstraintUsageRuleHelper onConstraintUsageRuleAdder, ElementaryActionsHelper elementaryActionsHelper) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, onConstraintUsageRuleAdder, elementaryActionsHelper);
    }

    private void createContingencies(PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags) {
        new CsaProfileContingencyCreator(crac, network, contingenciesPropertyBags, contingencyEquipmentsPropertyBags, creationContext);
    }

    private void createCnecs(PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, Set<Side> defaultMonitoredSides, String regionEic) {
        new CsaProfileCnecCreator(crac, network, assessedElementsPropertyBags, assessedElementsWithContingenciesPropertyBags, currentLimitsPropertyBags, voltageLimitsPropertyBags, angleLimitsPropertyBags, creationContext, defaultMonitoredSides, regionEic);
    }
}
