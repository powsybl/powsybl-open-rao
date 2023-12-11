/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionsCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.Set;

import static com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils.isValidInterval;

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
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());

        clearNativeCracContextsAndMap(nativeCrac, offsetDateTime);
        createContingencies(nativeCrac.getContingencies(), nativeCrac.getContingencyEquipments());
        createCnecs(nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementsWithContingencies(), nativeCrac.getCurrentLimits(), nativeCrac.getVoltageLimits(), nativeCrac.getAngleLimits(), cracCreationParameters.getDefaultMonitoredSides());
        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementsWithRemedialAction());
        createRemedialActions(nativeCrac.getRemedialActions(), nativeCrac.getTopologyAction(), nativeCrac.getRotatingMachineAction(), nativeCrac.getShuntCompensatorModifications(), nativeCrac.getTapPositionAction(), nativeCrac.getStaticPropertyRanges(), nativeCrac.getContingencyWithRemedialAction(), onConstraintUsageRuleAdder,
            nativeCrac.getSchemeRemedialActionsRaProfile(), nativeCrac.getSchemeRemedialActionsRasProfile(), nativeCrac.getRemedialActionScheme(), nativeCrac.getStage(), nativeCrac.getGridStateAlterationCollection(), nativeCrac.getTopologyActionAuto(), nativeCrac.getRotatingMachineActionAuto(), nativeCrac.getShuntCompensatorModificationAuto(), nativeCrac.getTapPositionActionAuto());
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void clearNativeCracContextsAndMap(CsaProfileCrac nativeCrac, OffsetDateTime offsetDateTime) {
        nativeCrac.getHeaders().forEach((contextName, properties) -> {
            if (!properties.isEmpty()) {
                PropertyBag property = properties.get(0);
                if (!checkTimeCoherence(property, offsetDateTime)) {
                    FaraoLoggerProvider.BUSINESS_WARNS.warn(String.format("[REMOVED] The file : %s will be ignored. Its dates are not consistent with the import date : %s", contextName, offsetDateTime));
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
                                       PropertyBags schemeRemedialActionsRaPropertyBags, PropertyBags schemeRemedialActionsRasPropertyBags, PropertyBags remedialActionSchemePropertyBags, PropertyBags stagePropertyBags, PropertyBags gridStateAlterationCollectionPropertyBags, PropertyBags topologyActionAuto, PropertyBags rotatingMachineActionAuto, PropertyBags shuntCompensatorModificationAuto, PropertyBags tapPositionActionAuto) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, remedialActionsPropertyBags, contingencyWithRemedialActionsPropertyBags, topologyActionsPropertyBags, rotatingMachineActionPropertyBags, shuntCompensatorModificationPropertyBags, tapPositionPropertyBags, staticPropertyRanges, onConstraintUsageRuleAdder,
                schemeRemedialActionsRaPropertyBags, schemeRemedialActionsRasPropertyBags, remedialActionSchemePropertyBags, stagePropertyBags, gridStateAlterationCollectionPropertyBags, topologyActionAuto, rotatingMachineActionAuto, shuntCompensatorModificationAuto, tapPositionActionAuto);
    }

    private void createContingencies(PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags) {
        new CsaProfileContingencyCreator(crac, network, contingenciesPropertyBags, contingencyEquipmentsPropertyBags, creationContext);
    }

    private void createCnecs(PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags, Set<Side> defaultMonitoredSides) {
        new CsaProfileCnecCreator(crac, network, assessedElementsPropertyBags, assessedElementsWithContingenciesPropertyBags, currentLimitsPropertyBags, voltageLimitsPropertyBags, angleLimitsPropertyBags, creationContext, defaultMonitoredSides);
    }
}
