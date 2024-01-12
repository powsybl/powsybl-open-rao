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
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

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
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        addCsaInstants();
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
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
        PropertyBags remedialActions = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActions(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION);
        PropertyBags remedialActionSchemes = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActionScheme(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME);
        PropertyBags gridStateAlterations = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationCollection(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION);
        PropertyBags staticPropertyRanges = CsaProfileCracUtils.overrideData(nativeCrac.getStaticPropertyRanges(), overridingData, CsaProfileConstants.OverridingObjectsFields.RANGE_CONSTRAINT);

        createContingencies(contingencies, nativeCrac.getContingencyEquipments());
        createCnecs(assessedElements, assessedElementsWithContingencies, currentLimits, voltageLimits, angleLimits, cracCreationParameters.getDefaultMonitoredSides());
        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), assessedElements, assessedElementsWithRemedialAction);
        createRemedialActions(remedialActions, nativeCrac.getTopologyAction(), nativeCrac.getRotatingMachineAction(), nativeCrac.getShuntCompensatorModifications(), nativeCrac.getTapPositionAction(), staticPropertyRanges, contingenciesWithRemedialAction, onConstraintUsageRuleAdder,
            nativeCrac.getSchemeRemedialActions(), remedialActionSchemes, nativeCrac.getStage(), gridStateAlterations, nativeCrac.getTopologyActionAuto(), nativeCrac.getRotatingMachineActionAuto(), nativeCrac.getShuntCompensatorModificationAuto(),
            nativeCrac.getTapPositionActionAuto());
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants() {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        // TODO : add other curative instants here
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

    private void createRemedialActions(PropertyBags remedialActionsPropertyBags, PropertyBags topologyActionsPropertyBags, PropertyBags rotatingMachineActionPropertyBags, PropertyBags shuntCompensatorModificationPropertyBags, PropertyBags tapPositionPropertyBags, PropertyBags staticPropertyRanges, PropertyBags contingencyWithRemedialActionsPropertyBags, OnConstraintUsageRuleHelper onConstraintUsageRuleAdder,
                                       PropertyBags schemeRemedialActionsPropertyBags, PropertyBags remedialActionSchemePropertyBags, PropertyBags stagePropertyBags, PropertyBags gridStateAlterationCollectionPropertyBags, PropertyBags topologyActionAuto, PropertyBags rotatingMachineActionAuto, PropertyBags shuntCompensatorModificationAuto, PropertyBags tapPositionActionAuto) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, remedialActionsPropertyBags, contingencyWithRemedialActionsPropertyBags, topologyActionsPropertyBags, rotatingMachineActionPropertyBags, shuntCompensatorModificationPropertyBags, tapPositionPropertyBags, staticPropertyRanges, onConstraintUsageRuleAdder,
                schemeRemedialActionsPropertyBags, remedialActionSchemePropertyBags, stagePropertyBags, gridStateAlterationCollectionPropertyBags, topologyActionAuto, rotatingMachineActionAuto, shuntCompensatorModificationAuto, tapPositionActionAuto);
    }

    private void createContingencies(PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags) {
        new CsaProfileContingencyCreator(crac, network, contingenciesPropertyBags, contingencyEquipmentsPropertyBags, creationContext);
    }

    private void createCnecs(PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, Set<Side> defaultMonitoredSides) {
        new CsaProfileCnecCreator(crac, network, assessedElementsPropertyBags, assessedElementsWithContingenciesPropertyBags, currentLimitsPropertyBags, voltageLimitsPropertyBags, angleLimitsPropertyBags, creationContext, defaultMonitoredSides);
    }
}
