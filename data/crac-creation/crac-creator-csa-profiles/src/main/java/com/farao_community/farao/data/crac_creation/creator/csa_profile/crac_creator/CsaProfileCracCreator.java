/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.cnec.CsaProfileCnecCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.contingency.CsaProfileContingencyCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.CsaProfileRemedialActionsCreator;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CsaProfileCracCreator implements CracCreator<CsaProfileCrac, CsaProfileCracCreationContext> {

    CsaProfileCracCreationContext creationContext;
    private Crac crac;
    private Network network;

    @Override
    public String getNativeCracFormat() {
        return "CsaProfileCrac";
    }

    public CsaProfileCracCreationContext createCrac(CsaProfileCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters cracCreationParameters) {
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        addCsaInstants();
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());

        createContingencies(nativeCrac.getContingencies(), nativeCrac.getContingencyEquipments());
        createCnecs(nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementsWithContingencies(), nativeCrac.getCurrentLimits(), nativeCrac.getVoltageLimits(), nativeCrac.getAngleLimits());
        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementsWithRemedialAction());
        createRemedialActions(nativeCrac.getRemedialActions(), nativeCrac.getTopologyAction(), nativeCrac.getRotatingMachineAction(), nativeCrac.getTapPositionAction(), nativeCrac.getStaticPropertyRanges(), nativeCrac.getContingencyWithRemedialAction(), onConstraintUsageRuleAdder);
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void addCsaInstants() {
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
    }

    private void createRemedialActions(PropertyBags remedialActionsPropertyBags, PropertyBags topologyActionsPropertyBags, PropertyBags rotatingMachineActionPropertyBags, PropertyBags tapPositionPropertyBags, PropertyBags staticPropertyRanges, PropertyBags contingencyWithRemedialActionsPropertyBags, OnConstraintUsageRuleHelper onConstraintUsageRuleAdder) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, remedialActionsPropertyBags, contingencyWithRemedialActionsPropertyBags, topologyActionsPropertyBags, rotatingMachineActionPropertyBags, tapPositionPropertyBags, staticPropertyRanges, onConstraintUsageRuleAdder);
    }

    private void createContingencies(PropertyBags contingenciesPropertyBags, PropertyBags contingencyEquipmentsPropertyBags) {
        new CsaProfileContingencyCreator(crac, network, contingenciesPropertyBags, contingencyEquipmentsPropertyBags, creationContext);
    }

    private void createCnecs(PropertyBags assessedElementsPropertyBags, PropertyBags assessedElementsWithContingenciesPropertyBags, PropertyBags currentLimitsPropertyBags, PropertyBags voltageLimitsPropertyBags, PropertyBags angleLimitsPropertyBags) {
        new CsaProfileCnecCreator(crac, network, assessedElementsPropertyBags, assessedElementsWithContingenciesPropertyBags, currentLimitsPropertyBags, voltageLimitsPropertyBags, angleLimitsPropertyBags, creationContext);
    }
}
