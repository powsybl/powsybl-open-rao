/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreator;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.cim.CurrentLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.cim.VoltageLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec.CsaProfileCnecCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency.CsaProfileContingencyCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.CsaProfileRemedialActionsCreator;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.ElementaryActionsHelper;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction.OnConstraintUsageRuleHelper;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElement;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.AssessedElementWithContingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Contingency;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.ContingencyEquipment;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.VoltageAngleLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.craccreation.util.RaUsageLimitsAdder;
import com.powsybl.triplestore.api.PropertyBags;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

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
        this.crac = cracCreationParameters.getCracFactory().create(nativeCrac.toString());
        this.network = network;
        this.creationContext = new CsaProfileCracCreationContext(crac, offsetDateTime, network.getNameOrId());
        addCsaInstants();
        RaUsageLimitsAdder.addRaUsageLimits(crac, cracCreationParameters);
        nativeCrac.setForTimestamp(offsetDateTime);

        Map<String, String> overridingData = nativeCrac.getOverridingData();
        PropertyBags gridStateAlterationRemedialAction = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationRemedialActions(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION_REMEDIAL_ACTION);
        PropertyBags remedialActionSchemes = CsaProfileCracUtils.overrideData(nativeCrac.getRemedialActionSchemes(), overridingData, CsaProfileConstants.OverridingObjectsFields.REMEDIAL_ACTION_SCHEME);
        PropertyBags gridStateAlterationsCollection = CsaProfileCracUtils.overrideData(nativeCrac.getGridStateAlterationCollections(), overridingData, CsaProfileConstants.OverridingObjectsFields.GRID_STATE_ALTERATION);
        PropertyBags schemeRemedialActions = CsaProfileCracUtils.overrideData(nativeCrac.getSchemeRemedialActions(), overridingData, CsaProfileConstants.OverridingObjectsFields.SCHEME_REMEDIAL_ACTION);

        createContingencies(nativeCrac.getContingencies(), nativeCrac.getContingencyEquipments());

        createCnecs(nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementWithContingencies(), nativeCrac.getCurrentLimits(), nativeCrac.getVoltageLimits(), nativeCrac.getVoltageAngleLimits(), cracCreationParameters.getDefaultMonitoredSides(), csaParameters.getCapacityCalculationRegionEicCode());

        OnConstraintUsageRuleHelper onConstraintUsageRuleAdder = new OnConstraintUsageRuleHelper(creationContext.getCnecCreationContexts(), nativeCrac.getAssessedElements(), nativeCrac.getAssessedElementWithRemedialActions());
        ElementaryActionsHelper elementaryActionsHelper = new ElementaryActionsHelper(gridStateAlterationRemedialAction, schemeRemedialActions, remedialActionSchemes, nativeCrac.getStages(), gridStateAlterationsCollection, nativeCrac.getAssessedElementWithRemedialActions(), nativeCrac.getContingencyWithRemedialActions(), nativeCrac.getStaticPropertyRanges(), nativeCrac.getTopologyActions(), nativeCrac.getRotatingMachineActions(), nativeCrac.getShuntCompensatorModifications(), nativeCrac.getTapPositionActions(), nativeCrac.getRemedialActionGroups(), nativeCrac.getRemedialActionDependencies());
        createRemedialActions(onConstraintUsageRuleAdder, elementaryActionsHelper, csaParameters.getSpsMaxTimeToImplementThresholdInSeconds());
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

    private void createRemedialActions(OnConstraintUsageRuleHelper onConstraintUsageRuleAdder, ElementaryActionsHelper elementaryActionsHelper, int spsMaxTimeToImplementThreshold) {
        new CsaProfileRemedialActionsCreator(crac, network, creationContext, onConstraintUsageRuleAdder, elementaryActionsHelper, spsMaxTimeToImplementThreshold);
    }

    private void createContingencies(Set<Contingency> nativeContingencies, Set<ContingencyEquipment> nativeContingencyEquipments) {
        new CsaProfileContingencyCreator(crac, network, nativeContingencies, nativeContingencyEquipments, creationContext);
    }

    private void createCnecs(Set<AssessedElement> nativeAssessedElements, Set<AssessedElementWithContingency> nativeAssessedElementsWithContingencies, Set<CurrentLimit> nativeCurrentLimits, Set<VoltageLimit> nativeVoltageLimits, Set<VoltageAngleLimit> nativeVoltageAngleLimits, Set<Side> defaultMonitoredSides, String regionEic) {
        new CsaProfileCnecCreator(crac, network, nativeAssessedElements, nativeAssessedElementsWithContingencies, nativeCurrentLimits, nativeVoltageLimits, nativeVoltageAngleLimits, creationContext, defaultMonitoredSides, regionEic);
    }
}
